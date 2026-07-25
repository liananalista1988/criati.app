package br.app.criati.admin;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;
import br.app.criati.admin.repository.RedefinicaoSenhaGlobalAuditoriaRepository;
import br.app.criati.shared.enums.AcaoAuditoriaSegurancaGlobal;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
import br.app.criati.usuario.model.Usuario;

/**
 * Escrita da auditoria de FALHA/NEGACAO da redefinicao global de senha, numa
 * transacao propria ({@code REQUIRES_NEW}): precisa sobreviver mesmo quando a
 * operacao principal e recusada ou (em tese) sofre rollback, ja que nao ha
 * "transacao principal" bem-sucedida para ser atomica com ela nesses casos.
 * Metodo precisa ser chamado atraves do proxy Spring (a partir de um bean
 * diferente do chamador) - self-invocation quebraria o REQUIRES_NEW
 * silenciosamente (limitacao conhecida de proxies AOP do Spring).
 *
 * <p>O evento de SUCESSO nao passa por aqui: ele e gravado pelo proprio
 * {@link RedefinirSenhaGlobalService}, na MESMA transacao da troca de senha,
 * de proposito - se a gravacao da auditoria de sucesso falhar, a troca de
 * senha tambem deve ser revertida (mesma garantia ja usada e testada no
 * fluxo empresarial de redefinicao de senha).
 */
@Service
public class RedefinicaoSenhaGlobalAuditoriaService {

	private final RedefinicaoSenhaGlobalAuditoriaRepository repository;

	public RedefinicaoSenhaGlobalAuditoriaService(RedefinicaoSenhaGlobalAuditoriaRepository repository) {
		this.repository = repository;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void registrarFalha(Usuario administrador, UUID usuarioAlvoId, ResultadoAuditoriaSeguranca resultado,
			MotivoAuditoriaSeguranca motivo, String ipOrigem) {
		repository.save(new RedefinicaoSenhaGlobalAuditoria(administrador, usuarioAlvoId,
				AcaoAuditoriaSegurancaGlobal.REDEFINICAO_ADMINISTRATIVA_SENHA_GLOBAL, resultado, motivo, ipOrigem));
	}
}
