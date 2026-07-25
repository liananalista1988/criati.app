package br.app.criati.acesso.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.RedefinicaoSenhaAuditoria;
import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.RedefinicaoSenhaAuditoriaRepository;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.AutoAlteracaoNaoPermitidaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UltimoAdministradorAtivoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.VinculoStatusInvalidoException;
import br.app.criati.shared.enums.AcaoAuditoriaSeguranca;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.validacao.SenhaValidador;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class GerenciarUsuarioEmpresaService {

	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final RedefinicaoSenhaAuditoriaRepository redefinicaoSenhaAuditoriaRepository;
	private final PasswordEncoder passwordEncoder;
	private final SenhaValidador senhaValidador;

	public GerenciarUsuarioEmpresaService(UsuarioEmpresaRepository usuarioEmpresaRepository,
			UsuarioRepository usuarioRepository, RedefinicaoSenhaAuditoriaRepository redefinicaoSenhaAuditoriaRepository,
			PasswordEncoder passwordEncoder, SenhaValidador senhaValidador) {
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.redefinicaoSenhaAuditoriaRepository = redefinicaoSenhaAuditoriaRepository;
		this.passwordEncoder = passwordEncoder;
		this.senhaValidador = senhaValidador;
	}

	@Transactional(readOnly = true)
	public List<UsuarioEmpresa> listar(
			ContextoEmpresaAtual contextoChamador,
			StatusCadastro statusFiltro,
			PerfilUsuario perfilFiltro,
			String busca) {
		exigirAdministrador(contextoChamador);

		String buscaNormalizada = (busca == null || busca.isBlank())
				? null
				: busca.trim().toLowerCase(Locale.ROOT);

		return usuarioEmpresaRepository.findAllByEmpresaId(contextoChamador.empresaId()).stream()
				.filter(vinculo -> statusFiltro == null || vinculo.getStatus() == statusFiltro)
				.filter(vinculo -> perfilFiltro == null || vinculo.getPerfil() == perfilFiltro)
				.filter(vinculo -> buscaNormalizada == null || corresponde(vinculo, buscaNormalizada))
				.toList();
	}

	@Transactional(readOnly = true)
	public UsuarioEmpresa buscar(UUID usuarioEmpresaId, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);
		return buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
	}

	@Transactional
	public UsuarioEmpresa alterarPerfil(
			UUID usuarioEmpresaId, PerfilUsuario novoPerfil, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);
		if (novoPerfil == null) {
			throw new DadosInvalidosException("Perfil e obrigatorio");
		}

		UsuarioEmpresa vinculo = buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo inativo nao pode ter o perfil alterado");
		}

		boolean ehProprioVinculo = ehProprioVinculo(vinculo, contextoChamador);
		if (ehProprioVinculo && novoPerfil != PerfilUsuario.ADMINISTRADOR) {
			throw new AutoAlteracaoNaoPermitidaException();
		}

		if (vinculo.getPerfil() == PerfilUsuario.ADMINISTRADOR && novoPerfil != PerfilUsuario.ADMINISTRADOR) {
			protegerUltimoAdministrador(vinculo);
		}

		vinculo.alterarPerfil(novoPerfil);
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public UsuarioEmpresa suspender(UUID usuarioEmpresaId, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);

		UsuarioEmpresa vinculo = buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
		if (ehProprioVinculo(vinculo, contextoChamador)) {
			throw new AutoAlteracaoNaoPermitidaException();
		}
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta inativo");
		}

		protegerUltimoAdministrador(vinculo);

		vinculo.suspender();
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public UsuarioEmpresa reativar(UUID usuarioEmpresaId, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);

		UsuarioEmpresa vinculo = buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
		if (vinculo.getStatus() != StatusCadastro.INATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta ativo");
		}

		vinculo.reativar();
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public void remover(UUID usuarioEmpresaId, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);

		UsuarioEmpresa vinculo = buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
		if (ehProprioVinculo(vinculo, contextoChamador)) {
			throw new AutoAlteracaoNaoPermitidaException();
		}
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta inativo");
		}

		protegerUltimoAdministrador(vinculo);

		vinculo.removerLogicamente();
		usuarioEmpresaRepository.save(vinculo);
	}

	// CRIATI-SEG-001: redefinicao administrativa de senha. Reaproveita
	// SenhaValidador (mesma politica minima ja usada em AceitarConviteService)
	// e PasswordEncoder (mesmo bean BCrypt de todo o sistema) — nunca codifica
	// nem valida senha por conta propria. Tudo numa unica transacao: se o
	// registro de auditoria falhar ao salvar, a excecao propaga e o proxy
	// @Transactional reverte tambem a troca de senha (nao ha commit parcial).
	@Transactional
	public void redefinirSenha(
			UUID usuarioEmpresaId, String novaSenha, String confirmacaoSenha, ContextoEmpresaAtual contextoChamador) {
		exigirAdministrador(contextoChamador);

		UsuarioEmpresa vinculo = buscarVinculoDaEmpresaAtiva(usuarioEmpresaId, contextoChamador);
		if (ehProprioVinculo(vinculo, contextoChamador)) {
			throw new AutoAlteracaoNaoPermitidaException();
		}
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo inativo nao pode ter a senha redefinida");
		}

		senhaValidador.validar(novaSenha, confirmacaoSenha);

		Usuario administrador = usuarioRepository.findById(contextoChamador.usuarioId())
				.orElseThrow(UsuarioNaoEncontradoException::new);
		Usuario usuarioAfetado = vinculo.getUsuario();

		usuarioAfetado.redefinirSenha(passwordEncoder.encode(novaSenha));
		usuarioRepository.save(usuarioAfetado);

		RedefinicaoSenhaAuditoria evento = new RedefinicaoSenhaAuditoria(
				vinculo.getEmpresa(), administrador, usuarioAfetado, AcaoAuditoriaSeguranca.REDEFINICAO_ADMINISTRATIVA_SENHA);
		redefinicaoSenhaAuditoriaRepository.save(evento);
	}

	// O vinculo do proprio chamador e identificado pelo usuarioEmpresaId
	// gravado no contexto da sessao, nunca comparando usuarioId: e o mesmo
	// dado ja usado por ContextoEmpresaService para validar a sessao.
	private boolean ehProprioVinculo(UsuarioEmpresa vinculo, ContextoEmpresaAtual contextoChamador) {
		return vinculo.getId().equals(contextoChamador.usuarioEmpresaId());
	}

	// So ha risco de ficar sem Administrador se o vinculo alterado for,
	// neste exato momento, um Administrador ativo da empresa. A contagem
	// ocorre na mesma transacao da escrita (mesma fronteira, sem
	// REQUIRES_NEW); nao ha lock pessimista, risco residual de concorrencia
	// documentado em docs/DECISOES.md.
	private void protegerUltimoAdministrador(UsuarioEmpresa vinculo) {
		if (vinculo.getPerfil() != PerfilUsuario.ADMINISTRADOR || vinculo.getStatus() != StatusCadastro.ATIVO) {
			return;
		}
		long administradoresAtivos = usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				vinculo.getEmpresa().getId(), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		if (administradoresAtivos <= 1) {
			throw new UltimoAdministradorAtivoException();
		}
	}

	private UsuarioEmpresa buscarVinculoDaEmpresaAtiva(UUID usuarioEmpresaId, ContextoEmpresaAtual contextoChamador) {
		if (usuarioEmpresaId == null) {
			throw new AcessoNegadoException();
		}
		// Vinculo inexistente e vinculo de outra empresa recebem exatamente a
		// mesma excecao: nunca revelar se o recurso existe fora da empresa
		// ativa (mesmo padrao de ContextoEmpresaService/ConviteService).
		return usuarioEmpresaRepository.findByIdAndEmpresaId(usuarioEmpresaId, contextoChamador.empresaId())
				.orElseThrow(AcessoNegadoException::new);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contextoChamador) {
		Objects.requireNonNull(contextoChamador, "contextoChamador e obrigatorio");
		if (contextoChamador.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private boolean corresponde(UsuarioEmpresa vinculo, String buscaNormalizada) {
		String nome = vinculo.getUsuario().getNome();
		String email = vinculo.getUsuario().getEmail();
		return (nome != null && nome.toLowerCase(Locale.ROOT).contains(buscaNormalizada))
				|| (email != null && email.toLowerCase(Locale.ROOT).contains(buscaNormalizada));
	}
}
