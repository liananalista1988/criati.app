package br.app.criati.acesso.service;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class VincularUsuarioEmpresaService {

	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;

	public VincularUsuarioEmpresaService(
			UsuarioRepository usuarioRepository,
			EmpresaRepository empresaRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository) {
		this.usuarioRepository = usuarioRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
	}

	@Transactional
	public UsuarioEmpresa executar(
			UUID usuarioId,
			UUID empresaId,
			PerfilUsuario perfil,
			ContextoEmpresaAtual contextoChamador) {
		Objects.requireNonNull(contextoChamador, "contextoChamador e obrigatorio");
		if (usuarioId == null) {
			throw new DadosInvalidosException("Usuario e obrigatorio");
		}
		if (empresaId == null) {
			throw new DadosInvalidosException("Empresa e obrigatoria");
		}
		if (perfil == null) {
			throw new DadosInvalidosException("Perfil e obrigatorio");
		}

		// A empresa da operacao precisa ser exatamente a empresa ativa do
		// chamador: nunca confiar no empresaId enviado pelo cliente para decidir
		// em qual empresa o vinculo sera criado.
		if (!empresaId.equals(contextoChamador.empresaId())) {
			throw new AcessoNegadoException();
		}
		if (contextoChamador.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}

		UUID empresaAtivaId = Objects.requireNonNull(
				contextoChamador.empresaId(), "empresaId do contexto e obrigatorio");

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		Empresa empresa = empresaRepository.findById(empresaAtivaId)
				.orElseThrow(EmpresaNaoEncontradaException::new);

		if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId)) {
			throw new UsuarioEmpresaJaVinculadoException();
		}

		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				usuario,
				empresa,
				perfil,
				StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.save(vinculo);
	}
}
