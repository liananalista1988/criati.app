package br.app.criati.tenant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.shared.enums.StatusCadastro;
import jakarta.servlet.http.HttpSession;

@Service
public class ContextoEmpresaService {

	private final UsuarioEmpresaRepository usuarioEmpresaRepository;

	public ContextoEmpresaService(UsuarioEmpresaRepository usuarioEmpresaRepository) {
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
	}

	@Transactional(readOnly = true)
	public List<UsuarioEmpresa> listarVinculosAtivos(UUID usuarioId) {
		return usuarioEmpresaRepository.findAllByUsuarioIdAndStatus(usuarioId, StatusCadastro.ATIVO);
	}

	@Transactional(readOnly = true)
	public ContextoEmpresaAtual selecionarEmpresaAtiva(HttpSession session, UUID usuarioId, UUID empresaId) {
		UsuarioEmpresa vinculo = buscarVinculoValido(usuarioId, empresaId)
				.orElseThrow(AcessoNegadoException::new);

		session.setAttribute(ContextoEmpresaSessionKeys.EMPRESA_ID, vinculo.getEmpresa().getId());
		session.setAttribute(ContextoEmpresaSessionKeys.USUARIO_EMPRESA_ID, vinculo.getId());

		return paraContexto(usuarioId, vinculo);
	}

	@Transactional(readOnly = true)
	public Optional<ContextoEmpresaAtual> obterContextoAtual(HttpSession session, UUID usuarioId) {
		Object empresaIdAtributo = session.getAttribute(ContextoEmpresaSessionKeys.EMPRESA_ID);
		Object usuarioEmpresaIdAtributo = session.getAttribute(ContextoEmpresaSessionKeys.USUARIO_EMPRESA_ID);
		if (empresaIdAtributo == null || usuarioEmpresaIdAtributo == null) {
			return Optional.empty();
		}

		Optional<UsuarioEmpresa> vinculo = buscarVinculoValido(usuarioId, (UUID) empresaIdAtributo)
				.filter(v -> v.getId().equals(usuarioEmpresaIdAtributo));

		if (vinculo.isEmpty()) {
			limparContexto(session);
			return Optional.empty();
		}

		return Optional.of(paraContexto(usuarioId, vinculo.get()));
	}

	public ContextoEmpresaAtual exigirContextoAtivo(HttpSession session, UUID usuarioId) {
		return obterContextoAtual(session, usuarioId)
				.orElseThrow(AcessoNegadoException::new);
	}

	public void limparContexto(HttpSession session) {
		session.removeAttribute(ContextoEmpresaSessionKeys.EMPRESA_ID);
		session.removeAttribute(ContextoEmpresaSessionKeys.USUARIO_EMPRESA_ID);
	}

	private Optional<UsuarioEmpresa> buscarVinculoValido(UUID usuarioId, UUID empresaId) {
		return usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId)
				.filter(v -> v.getStatus() == StatusCadastro.ATIVO)
				.filter(v -> v.getEmpresa().getStatus() == StatusCadastro.ATIVO);
	}

	private ContextoEmpresaAtual paraContexto(UUID usuarioId, UsuarioEmpresa vinculo) {
		return new ContextoEmpresaAtual(usuarioId, vinculo.getEmpresa().getId(), vinculo.getId(), vinculo.getPerfil());
	}
}
