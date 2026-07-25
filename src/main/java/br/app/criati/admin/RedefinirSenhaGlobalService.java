package br.app.criati.admin;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;
import br.app.criati.admin.repository.RedefinicaoSenhaGlobalAuditoriaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.AutoRedefinicaoSenhaNaoPermitidaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.UsuarioStatusInvalidoException;
import br.app.criati.shared.enums.AcaoAuditoriaSegurancaGlobal;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.validacao.SenhaValidador;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Redefinicao administrativa GLOBAL de senha (CRIATI-SEG-001): somente
 * Superadministrador ({@link Usuario#isSuperAdministrador()}), sobre qualquer
 * usuario ativo da plataforma, sem depender de empresa ativa nem de vinculo
 * (diferente de {@link br.app.criati.acesso.service.GerenciarUsuarioEmpresaService#redefinirSenha},
 * que e por empresa). Reaproveita {@link SenhaValidador} e {@link PasswordEncoder}
 * (mesma politica e mesmo hash de todo o sistema) mas mantem entidade,
 * repositorio e tabela de auditoria proprios - nao mistura com o fluxo
 * empresarial.
 *
 * <p>Ao contrario do fluxo empresarial (que so audita sucesso), toda
 * tentativa negada ou invalida tambem gera um evento de auditoria, numa
 * transacao independente ({@link RedefinicaoSenhaGlobalAuditoriaService}) que
 * sobrevive mesmo que esta transacao principal seja abortada. O evento de
 * sucesso, por sua vez, e gravado na MESMA transacao da troca de senha (ver
 * javadoc de {@link RedefinicaoSenhaGlobalAuditoriaService}).
 */
@Service
public class RedefinirSenhaGlobalService {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final SenhaValidador senhaValidador;
	private final RedefinicaoSenhaGlobalAuditoriaRepository redefinicaoSenhaGlobalAuditoriaRepository;
	private final RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService;

	public RedefinirSenhaGlobalService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
			SenhaValidador senhaValidador, RedefinicaoSenhaGlobalAuditoriaRepository redefinicaoSenhaGlobalAuditoriaRepository,
			RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.senhaValidador = senhaValidador;
		this.redefinicaoSenhaGlobalAuditoriaRepository = redefinicaoSenhaGlobalAuditoriaRepository;
		this.auditoriaFalhaService = auditoriaFalhaService;
	}

	@Transactional
	public void redefinirSenha(UUID usuarioAlvoId, String novaSenha, String confirmacaoSenha,
			UUID administradorChamadorId, String ipOrigem) {
		Usuario chamador = usuarioRepository.findById(administradorChamadorId)
				.orElseThrow(UsuarioNaoEncontradoException::new);

		// Defesa em profundidade: o SecurityConfig ja exige ROLE_SUPERADMIN em
		// qualquer verbo de /api/admin/**, mas a autorizacao real desta acao
		// sensivel tambem e checada aqui no service, nunca apenas escondendo o
		// botao na tela (mesmo padrao ja documentado em PaginaController).
		if (!chamador.isSuperAdministrador()) {
			auditoriaFalhaService.registrarFalha(chamador, usuarioAlvoId, ResultadoAuditoriaSeguranca.NEGADO,
					MotivoAuditoriaSeguranca.SEM_PERMISSAO, ipOrigem);
			throw new AcessoNegadoException();
		}

		if (chamador.getId().equals(usuarioAlvoId)) {
			auditoriaFalhaService.registrarFalha(chamador, usuarioAlvoId, ResultadoAuditoriaSeguranca.NEGADO,
					MotivoAuditoriaSeguranca.PROPRIO_USUARIO, ipOrigem);
			throw new AutoRedefinicaoSenhaNaoPermitidaException();
		}

		Usuario alvo = usuarioRepository.findById(usuarioAlvoId).orElse(null);
		if (alvo == null) {
			auditoriaFalhaService.registrarFalha(chamador, usuarioAlvoId, ResultadoAuditoriaSeguranca.NEGADO,
					MotivoAuditoriaSeguranca.USUARIO_NAO_ENCONTRADO, ipOrigem);
			throw new UsuarioNaoEncontradoException();
		}

		if (alvo.getStatus() != StatusCadastro.ATIVO) {
			auditoriaFalhaService.registrarFalha(chamador, usuarioAlvoId, ResultadoAuditoriaSeguranca.NEGADO,
					MotivoAuditoriaSeguranca.USUARIO_INATIVO, ipOrigem);
			throw new UsuarioStatusInvalidoException("Usuario inativo nao pode ter a senha redefinida");
		}

		try {
			senhaValidador.validar(novaSenha, confirmacaoSenha);
		} catch (DadosInvalidosException excecaoValidacao) {
			auditoriaFalhaService.registrarFalha(chamador, usuarioAlvoId, ResultadoAuditoriaSeguranca.FALHA_VALIDACAO,
					MotivoAuditoriaSeguranca.SENHA_INVALIDA, ipOrigem);
			throw excecaoValidacao;
		}

		alvo.redefinirSenha(passwordEncoder.encode(novaSenha));
		usuarioRepository.save(alvo);

		redefinicaoSenhaGlobalAuditoriaRepository.save(new RedefinicaoSenhaGlobalAuditoria(chamador, usuarioAlvoId,
				AcaoAuditoriaSegurancaGlobal.REDEFINICAO_ADMINISTRATIVA_SENHA_GLOBAL, ResultadoAuditoriaSeguranca.SUCESSO,
				MotivoAuditoriaSeguranca.REDEFINICAO_CONCLUIDA, ipOrigem));
	}
}
