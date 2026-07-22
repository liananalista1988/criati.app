package br.app.criati.financeiro.shared.service;

import java.util.List;
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
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioJaVinculadoPessoaFinanceiraException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class PessoaFinanceiraService {

	private final PessoaFinanceiraRepository pessoaRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;

	public PessoaFinanceiraService(
			PessoaFinanceiraRepository pessoaRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			UsuarioRepository usuarioRepository,
			EmpresaRepository empresaRepository) {
		this.pessoaRepository = pessoaRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<PessoaFinanceira> listar(ContextoEmpresaAtual contexto, StatusCadastro status) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		return status == null
				? pessoaRepository.findAllByEmpresaIdOrderByNomeAsc(contexto.empresaId())
				: pessoaRepository.findAllByEmpresaIdAndStatusOrderByNomeAsc(contexto.empresaId(), status);
	}

	@Transactional(readOnly = true)
	public PessoaFinanceira buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<Usuario> listarUsuariosVinculaveis(ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		return usuarioEmpresaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(vinculo -> vinculo.getStatus() == StatusCadastro.ATIVO)
				.map(UsuarioEmpresa::getUsuario)
				.filter(usuario -> usuario.getStatus() == StatusCadastro.ATIVO)
				.sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
				.toList();
	}

	@Transactional
	public PessoaFinanceira criar(
			String nome, String apelido, UUID usuarioId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = normalizarObrigatorio(nome, "Nome", 150);
		String apelidoNormalizado = normalizarOpcional(apelido, "Apelido", 100);
		Usuario usuarioVinculado = validarUsuarioVinculado(usuarioId, contexto.empresaId(), null);
		Usuario autor = buscarAutor(contexto.usuarioId());
		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);

		return pessoaRepository.save(
				new PessoaFinanceira(empresa, nomeNormalizado, usuarioVinculado, apelidoNormalizado, autor));
	}

	@Transactional
	public PessoaFinanceira atualizar(
			UUID id, String nome, String apelido, UUID usuarioId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		PessoaFinanceira pessoa = buscarDaEmpresa(id, contexto.empresaId());
		Usuario usuarioVinculado = validarUsuarioVinculado(usuarioId, contexto.empresaId(), pessoa.getId());
		pessoa.atualizar(
				normalizarObrigatorio(nome, "Nome", 150),
				usuarioVinculado,
				normalizarOpcional(apelido, "Apelido", 100),
				buscarAutor(contexto.usuarioId()));
		return pessoaRepository.save(pessoa);
	}

	@Transactional
	public PessoaFinanceira desativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		PessoaFinanceira pessoa = buscarDaEmpresa(id, contexto.empresaId());
		if (!pessoa.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Pessoa financeira ja esta inativa");
		}
		pessoa.desativar(buscarAutor(contexto.usuarioId()));
		return pessoaRepository.save(pessoa);
	}

	@Transactional
	public PessoaFinanceira reativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		PessoaFinanceira pessoa = buscarDaEmpresa(id, contexto.empresaId());
		if (pessoa.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Pessoa financeira ja esta ativa");
		}
		if (pessoa.getUsuario() != null) {
			validarUsuarioVinculado(pessoa.getUsuario().getId(), contexto.empresaId(), pessoa.getId());
		}
		pessoa.reativar(buscarAutor(contexto.usuarioId()));
		return pessoaRepository.save(pessoa);
	}

	private Usuario validarUsuarioVinculado(UUID usuarioId, UUID empresaId, UUID pessoaIdAtual) {
		if (usuarioId == null) {
			return null;
		}
		UsuarioEmpresa vinculo = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(usuarioId, empresaId)
				.filter(item -> item.getStatus() == StatusCadastro.ATIVO)
				.filter(item -> item.getUsuario().getStatus() == StatusCadastro.ATIVO)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		boolean jaVinculado = pessoaIdAtual == null
				? pessoaRepository.existsByEmpresaIdAndUsuarioIdAndStatus(empresaId, usuarioId, StatusCadastro.ATIVO)
				: pessoaRepository.existsByEmpresaIdAndUsuarioIdAndStatusAndIdNot(
						empresaId, usuarioId, StatusCadastro.ATIVO, pessoaIdAtual);
		if (jaVinculado) {
			throw new UsuarioJaVinculadoPessoaFinanceiraException();
		}
		return vinculo.getUsuario();
	}

	private PessoaFinanceira buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new PessoaFinanceiraNaoEncontradaException();
		}
		return pessoaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(campo + " e obrigatorio");
		}
		String normalizado = valor.trim();
		if (normalizado.length() > tamanhoMaximo) {
			throw new DadosInvalidosException(campo + " deve possuir no maximo " + tamanhoMaximo + " caracteres");
		}
		return normalizado;
	}

	private String normalizarOpcional(String valor, String campo, int tamanhoMaximo) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return normalizarObrigatorio(valor, campo, tamanhoMaximo);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
