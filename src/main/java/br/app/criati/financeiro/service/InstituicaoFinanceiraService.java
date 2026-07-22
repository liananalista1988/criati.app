package br.app.criati.financeiro.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.InstituicaoFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class InstituicaoFinanceiraService {

	private final InstituicaoFinanceiraRepository repository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public InstituicaoFinanceiraService(
			InstituicaoFinanceiraRepository repository,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository) {
		this.repository = repository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<InstituicaoFinanceira> listar(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		return repository.listarDisponiveis(contexto.empresaId(), StatusCadastro.ATIVO);
	}

	@Transactional(readOnly = true)
	public InstituicaoFinanceira buscarDisponivel(UUID id, ContextoEmpresaAtual contexto) {
		if (id == null) {
			return null;
		}
		return repository.buscarDisponivel(id, contexto.empresaId(), StatusCadastro.ATIVO)
				.orElseThrow(InstituicaoFinanceiraNaoEncontradaException::new);
	}

	@Transactional
	public InstituicaoFinanceira criarLocal(String nome, String codigo, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = normalizarObrigatorio(nome, "Nome", 150);
		if (repository.existeNomeDisponivel(contexto.empresaId(), nomeNormalizado)) {
			throw new DadosInvalidosException("Instituicao financeira ja cadastrada para esta empresa");
		}
		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = usuarioRepository.findById(contexto.usuarioId())
				.orElseThrow(UsuarioNaoEncontradoException::new);
		return repository.save(new InstituicaoFinanceira(
				empresa, nomeNormalizado, normalizarOpcional(codigo, "Codigo", 20), autor));
	}

	private String normalizarObrigatorio(String valor, String campo, int tamanho) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(campo + " e obrigatorio");
		}
		String normalizado = valor.trim();
		if (normalizado.length() > tamanho) {
			throw new DadosInvalidosException(campo + " deve possuir no maximo " + tamanho + " caracteres");
		}
		return normalizado;
	}

	private String normalizarOpcional(String valor, String campo, int tamanho) {
		return valor == null || valor.isBlank() ? null : normalizarObrigatorio(valor, campo, tamanho);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
