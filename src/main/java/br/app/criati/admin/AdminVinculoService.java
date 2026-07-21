package br.app.criati.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UltimoAdministradorAtivoException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.VinculoStatusInvalidoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// Gestao global de vinculos (Usuario <-> Empresa) pelo Superadministrador, fora
// de qualquer contexto de sessao empresarial. Reaplica exatamente as mesmas
// regras de protecao ja usadas por GerenciarUsuarioEmpresaService/
// VincularUsuarioEmpresaService (ultimo Administrador ativo, duplicidade,
// transicoes de status validas); a unica regra que NAO se aplica aqui e a de
// autoalteracao, pois o Superadministrador nao participa como vinculo da
// empresa que esta administrando.
@Service
public class AdminVinculoService {

	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;

	public AdminVinculoService(
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			UsuarioRepository usuarioRepository,
			EmpresaRepository empresaRepository) {
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<UsuarioEmpresa> listarTodos(
			UUID usuarioIdFiltro, UUID empresaIdFiltro, PerfilUsuario perfilFiltro, StatusCadastro statusFiltro) {
		return usuarioEmpresaRepository.findAll().stream()
				.filter(vinculo -> usuarioIdFiltro == null || vinculo.getUsuario().getId().equals(usuarioIdFiltro))
				.filter(vinculo -> empresaIdFiltro == null || vinculo.getEmpresa().getId().equals(empresaIdFiltro))
				.filter(vinculo -> perfilFiltro == null || vinculo.getPerfil() == perfilFiltro)
				.filter(vinculo -> statusFiltro == null || vinculo.getStatus() == statusFiltro)
				.toList();
	}

	@Transactional(readOnly = true)
	public UsuarioEmpresa buscar(UUID vinculoId) {
		return usuarioEmpresaRepository.findById(vinculoId)
				.orElseThrow(() -> new DadosInvalidosException("Vinculo nao encontrado"));
	}

	@Transactional
	public UsuarioEmpresa criar(UUID usuarioId, UUID empresaId, PerfilUsuario perfil) {
		if (usuarioId == null) {
			throw new DadosInvalidosException("Usuario e obrigatorio");
		}
		if (empresaId == null) {
			throw new DadosInvalidosException("Empresa e obrigatoria");
		}
		if (perfil == null) {
			throw new DadosInvalidosException("Perfil e obrigatorio");
		}

		Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
		Empresa empresa = empresaRepository.findById(empresaId).orElseThrow(EmpresaNaoEncontradaException::new);

		if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId)) {
			throw new UsuarioEmpresaJaVinculadoException();
		}

		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public UsuarioEmpresa alterarPerfil(UUID vinculoId, PerfilUsuario novoPerfil) {
		if (novoPerfil == null) {
			throw new DadosInvalidosException("Perfil e obrigatorio");
		}
		UsuarioEmpresa vinculo = buscar(vinculoId);
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo inativo nao pode ter o perfil alterado");
		}
		if (vinculo.getPerfil() == PerfilUsuario.ADMINISTRADOR && novoPerfil != PerfilUsuario.ADMINISTRADOR) {
			protegerUltimoAdministrador(vinculo);
		}
		vinculo.alterarPerfil(novoPerfil);
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public UsuarioEmpresa suspender(UUID vinculoId) {
		UsuarioEmpresa vinculo = buscar(vinculoId);
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta inativo");
		}
		protegerUltimoAdministrador(vinculo);
		vinculo.suspender();
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public UsuarioEmpresa reativar(UUID vinculoId) {
		UsuarioEmpresa vinculo = buscar(vinculoId);
		if (vinculo.getStatus() != StatusCadastro.INATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta ativo");
		}
		vinculo.reativar();
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional
	public void remover(UUID vinculoId) {
		UsuarioEmpresa vinculo = buscar(vinculoId);
		if (vinculo.getStatus() != StatusCadastro.ATIVO) {
			throw new VinculoStatusInvalidoException("Vinculo ja esta inativo");
		}
		protegerUltimoAdministrador(vinculo);
		vinculo.removerLogicamente();
		usuarioEmpresaRepository.save(vinculo);
	}

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
}
