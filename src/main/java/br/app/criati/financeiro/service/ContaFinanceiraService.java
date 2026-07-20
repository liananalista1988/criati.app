package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ContaFinanceiraComLancamentosException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.ContaFinanceiraNomeDuplicadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.tenant.ContextoEmpresaAtual;

@Service
public class ContaFinanceiraService {

	private final ContaFinanceiraRepository contaFinanceiraRepository;
	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
	private final EmpresaRepository empresaRepository;

	public ContaFinanceiraService(
			ContaFinanceiraRepository contaFinanceiraRepository,
			LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
			EmpresaRepository empresaRepository) {
		this.contaFinanceiraRepository = contaFinanceiraRepository;
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<ContaFinanceira> listar(
			ContextoEmpresaAtual contexto, StatusCadastro statusFiltro, TipoContaFinanceira tipoFiltro) {
		return contaFinanceiraRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(conta -> statusFiltro == null || conta.getStatus() == statusFiltro)
				.filter(conta -> tipoFiltro == null || conta.getTipo() == tipoFiltro)
				.toList();
	}

	@Transactional(readOnly = true)
	public ContaFinanceira buscar(UUID contaId, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(contaId, contexto.empresaId());
	}

	@Transactional
	public ContaFinanceira criar(
			String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = validarDados(nome, tipo, saldoInicial);
		exigirNomeDisponivel(contexto.empresaId(), nomeNormalizado);

		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		ContaFinanceira conta = new ContaFinanceira(
				empresa, nomeNormalizado, tipo, MoedaUtils.normalizar(saldoInicial), StatusCadastro.ATIVO);
		return contaFinanceiraRepository.save(conta);
	}

	// Editar so e permitido enquanto a conta nao tiver nenhum lancamento: uma
	// vez que exista lancamento, alterar saldoInicial ou tipo invalidaria o
	// historico de saldo ja calculado a partir desses dados.
	@Transactional
	public ContaFinanceira editar(
			UUID contaId,
			String nome,
			TipoContaFinanceira tipo,
			BigDecimal saldoInicial,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = validarDados(nome, tipo, saldoInicial);

		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (lancamentoFinanceiroRepository.existsByContaId(conta.getId())) {
			throw new ContaFinanceiraComLancamentosException();
		}
		if (!conta.getNome().equalsIgnoreCase(nomeNormalizado)) {
			exigirNomeDisponivel(contexto.empresaId(), nomeNormalizado);
		}

		conta.atualizarDados(nomeNormalizado, tipo, MoedaUtils.normalizar(saldoInicial));
		return contaFinanceiraRepository.save(conta);
	}

	@Transactional
	public ContaFinanceira inativar(UUID contaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (!conta.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Conta financeira ja esta inativa");
		}
		conta.inativar();
		return contaFinanceiraRepository.save(conta);
	}

	@Transactional
	public ContaFinanceira reativar(UUID contaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (conta.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Conta financeira ja esta ativa");
		}
		conta.reativar();
		return contaFinanceiraRepository.save(conta);
	}

	private void exigirNomeDisponivel(UUID empresaId, String nomeNormalizado) {
		if (contaFinanceiraRepository.existsByEmpresaIdAndNomeIgnoreCaseAndStatus(
				empresaId, nomeNormalizado, StatusCadastro.ATIVO)) {
			throw new ContaFinanceiraNomeDuplicadoException();
		}
	}

	private ContaFinanceira buscarDaEmpresa(UUID contaId, UUID empresaId) {
		if (contaId == null) {
			throw new ContaFinanceiraNaoEncontradaException();
		}
		return contaFinanceiraRepository.findByIdAndEmpresaId(contaId, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
	}

	private String validarDados(String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial) {
		if (nome == null || nome.isBlank()) {
			throw new DadosInvalidosException("Nome e obrigatorio");
		}
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		if (saldoInicial == null) {
			throw new DadosInvalidosException("Saldo inicial e obrigatorio");
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
