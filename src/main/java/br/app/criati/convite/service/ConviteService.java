package br.app.criati.convite.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ConviteService {

	private final ConviteRepository conviteRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final ConviteTokenService conviteTokenService;
	private final ConviteNotificador conviteNotificador;
	private final long expiracaoHoras;

	public ConviteService(
			ConviteRepository conviteRepository,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository,
			ConviteTokenService conviteTokenService,
			ConviteNotificador conviteNotificador,
			@Value("${criati.convite.expiracao-horas:72}") long expiracaoHoras) {
		this.conviteRepository = conviteRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.conviteTokenService = conviteTokenService;
		this.conviteNotificador = conviteNotificador;
		this.expiracaoHoras = expiracaoHoras;
	}

	// Politica de duplicidade: um convite PENDENTE existente para o mesmo
	// e-mail na mesma empresa e automaticamente revogado antes de criar o
	// novo (equivalente a "reenviar convite"), em vez de rejeitar com 409 ou
	// reaproveitar o token antigo. Mantem sempre no maximo um convite ativo
	// por par empresa+e-mail e invalida imediatamente qualquer link anterior.
	@Transactional
	public ConviteCriado criar(String email, PerfilUsuario perfil, ContextoEmpresaAtual contextoChamador) {
		Objects.requireNonNull(contextoChamador, "contextoChamador e obrigatorio");
		if (contextoChamador.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
		return criarInterno(contextoChamador.empresaId(), email, perfil, contextoChamador.usuarioId());
	}

	// Variante para o painel global do Superadministrador: nao existe
	// contexto de empresa ativa (Superadministrador nao possui vinculo), entao
	// a empresa e informada diretamente pelo id (nunca por dado do cliente
	// sem validacao - buscarEmpresa/UsuarioNaoEncontrado ja garantem que
	// ambos existem). Usada apenas pelo onboarding/gestao de convites do
	// painel administrativo, sempre atras de ROLE_SUPERADMIN.
	@Transactional
	public ConviteCriado criarComoSuperAdministrador(
			UUID empresaId, String email, PerfilUsuario perfil, UUID criadoPorUsuarioId) {
		return criarInterno(empresaId, email, perfil, criadoPorUsuarioId);
	}

	private ConviteCriado criarInterno(UUID empresaId, String email, PerfilUsuario perfil, UUID criadoPorUsuarioId) {
		if (email == null || email.isBlank()) {
			throw new DadosInvalidosException("E-mail e obrigatorio");
		}
		if (perfil == null) {
			throw new DadosInvalidosException("Perfil e obrigatorio");
		}

		Empresa empresa = empresaRepository.findById(empresaId)
				.orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario criadoPor = usuarioRepository.findById(criadoPorUsuarioId)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);

		conviteRepository
				.findByEmpresaIdAndEmailIgnoreCaseAndStatus(empresa.getId(), emailNormalizado, StatusConvite.PENDENTE)
				.ifPresent(pendente -> {
					pendente.revogar();
					conviteRepository.save(pendente);
				});

		String tokenBruto = conviteTokenService.gerarTokenBruto();
		String tokenHash = conviteTokenService.calcularHash(tokenBruto);
		OffsetDateTime expiraEm = OffsetDateTime.now().plusHours(expiracaoHoras);

		Convite convite = new Convite(empresa, emailNormalizado, perfil, tokenHash, expiraEm, criadoPor);
		conviteRepository.save(convite);
		conviteNotificador.notificar(convite, tokenBruto);

		return new ConviteCriado(convite, tokenBruto);
	}

	@Transactional(readOnly = true)
	public List<Convite> listarPorEmpresa(ContextoEmpresaAtual contextoChamador) {
		Objects.requireNonNull(contextoChamador, "contextoChamador e obrigatorio");
		if (contextoChamador.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
		return conviteRepository.findAllByEmpresaId(contextoChamador.empresaId());
	}

	// Variante para o painel global: lista os convites de qualquer empresa,
	// identificada pelo id (sem exigir contexto/sessao de empresa ativa).
	@Transactional(readOnly = true)
	public List<Convite> listarPorEmpresaComoSuperAdministrador(UUID empresaId) {
		if (!empresaRepository.existsById(empresaId)) {
			throw new EmpresaNaoEncontradaException();
		}
		return conviteRepository.findAllByEmpresaId(empresaId);
	}

	@Transactional
	public void revogar(UUID conviteId, ContextoEmpresaAtual contextoChamador) {
		Objects.requireNonNull(contextoChamador, "contextoChamador e obrigatorio");
		if (contextoChamador.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}

		Convite convite = conviteRepository
				.findByIdAndEmpresaId(conviteId, contextoChamador.empresaId())
				.orElseThrow(AcessoNegadoException::new);
		revogarConvite(convite);
	}

	// Variante para o painel global: identifica o convite por empresa+id
	// diretamente (sem contexto de sessao), sempre atras de ROLE_SUPERADMIN.
	@Transactional
	public void revogarComoSuperAdministrador(UUID empresaId, UUID conviteId) {
		Convite convite = conviteRepository
				.findByIdAndEmpresaId(conviteId, empresaId)
				.orElseThrow(() -> new DadosInvalidosException("Convite nao encontrado para esta empresa"));
		revogarConvite(convite);
	}

	private void revogarConvite(Convite convite) {
		if (convite.getStatus() != StatusConvite.PENDENTE) {
			throw new DadosInvalidosException("Convite nao pode ser revogado");
		}
		convite.revogar();
		conviteRepository.save(convite);
	}

	@Transactional(readOnly = true)
	public Optional<Convite> buscarValidoPeloToken(String tokenBruto) {
		if (tokenBruto == null || tokenBruto.isBlank()) {
			return Optional.empty();
		}
		String hash = conviteTokenService.calcularHash(tokenBruto);
		return conviteRepository.findByTokenHash(hash)
				.filter(convite -> convite.estaEfetivamenteValido(OffsetDateTime.now()))
				.filter(convite -> convite.getEmpresa().getStatus() == StatusCadastro.ATIVO);
	}
}
