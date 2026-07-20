package br.app.criati.financeiro.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraComLancamentosException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.CategoriaFinanceiraNomeDuplicadaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;

@Service
public class CategoriaFinanceiraService {

	private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;
	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
	private final EmpresaRepository empresaRepository;

	public CategoriaFinanceiraService(
			CategoriaFinanceiraRepository categoriaFinanceiraRepository,
			LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
			EmpresaRepository empresaRepository) {
		this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoriaFinanceira> listar(
			ContextoEmpresaAtual contexto, StatusCadastro statusFiltro, TipoFinanceiro tipoFiltro) {
		return categoriaFinanceiraRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(categoria -> statusFiltro == null || categoria.getStatus() == statusFiltro)
				.filter(categoria -> tipoFiltro == null || categoria.getTipo() == tipoFiltro)
				.toList();
	}

	@Transactional(readOnly = true)
	public CategoriaFinanceira buscar(UUID categoriaId, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(categoriaId, contexto.empresaId());
	}

	@Transactional
	public CategoriaFinanceira criar(String nome, TipoFinanceiro tipo, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = validarDados(nome, tipo);
		exigirNomeDisponivel(contexto.empresaId(), tipo, nomeNormalizado);

		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, nomeNormalizado, tipo, StatusCadastro.ATIVO);
		return categoriaFinanceiraRepository.save(categoria);
	}

	@Transactional
	public CategoriaFinanceira editar(
			UUID categoriaId, String nome, TipoFinanceiro tipo, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = validarDados(nome, tipo);

		CategoriaFinanceira categoria = buscarDaEmpresa(categoriaId, contexto.empresaId());
		if (lancamentoFinanceiroRepository.existsByCategoriaId(categoria.getId())) {
			throw new CategoriaFinanceiraComLancamentosException();
		}
		if (categoria.getTipo() != tipo || !categoria.getNome().equalsIgnoreCase(nomeNormalizado)) {
			exigirNomeDisponivel(contexto.empresaId(), tipo, nomeNormalizado);
		}

		categoria.atualizarDados(nomeNormalizado, tipo);
		return categoriaFinanceiraRepository.save(categoria);
	}

	@Transactional
	public CategoriaFinanceira inativar(UUID categoriaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CategoriaFinanceira categoria = buscarDaEmpresa(categoriaId, contexto.empresaId());
		if (!categoria.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Categoria financeira ja esta inativa");
		}
		categoria.inativar();
		return categoriaFinanceiraRepository.save(categoria);
	}

	@Transactional
	public CategoriaFinanceira reativar(UUID categoriaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CategoriaFinanceira categoria = buscarDaEmpresa(categoriaId, contexto.empresaId());
		if (categoria.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Categoria financeira ja esta ativa");
		}
		categoria.reativar();
		return categoriaFinanceiraRepository.save(categoria);
	}

	private void exigirNomeDisponivel(UUID empresaId, TipoFinanceiro tipo, String nomeNormalizado) {
		if (categoriaFinanceiraRepository.existsByEmpresaIdAndTipoAndNomeIgnoreCaseAndStatus(
				empresaId, tipo, nomeNormalizado, StatusCadastro.ATIVO)) {
			throw new CategoriaFinanceiraNomeDuplicadaException();
		}
	}

	private CategoriaFinanceira buscarDaEmpresa(UUID categoriaId, UUID empresaId) {
		if (categoriaId == null) {
			throw new CategoriaFinanceiraNaoEncontradaException();
		}
		return categoriaFinanceiraRepository.findByIdAndEmpresaId(categoriaId, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
	}

	private String validarDados(String nome, TipoFinanceiro tipo) {
		if (nome == null || nome.isBlank()) {
			throw new DadosInvalidosException("Nome e obrigatorio");
		}
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		return nome.trim();
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
