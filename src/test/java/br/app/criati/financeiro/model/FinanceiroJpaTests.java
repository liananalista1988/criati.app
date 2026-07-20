package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;

@ActiveProfiles("test")
@DataJpaTest
class FinanceiroJpaTests {

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private ContaFinanceiraRepository contaFinanceiraRepository;

	@Autowired
	private CategoriaFinanceiraRepository categoriaFinanceiraRepository;

	@Autowired
	private LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

	@Test
	void devePersistirContaFinanceiraComUuidEAuditoriaTemporal() {
		Empresa empresa = criarEmpresa("11111111000151");

		ContaFinanceira conta = contaFinanceiraRepository.saveAndFlush(new ContaFinanceira(
				empresa, "Caixa Principal", TipoContaFinanceira.CAIXA, new BigDecimal("100.00"), StatusCadastro.ATIVO));

		assertThat(conta.getId()).isNotNull();
		assertThat(conta.getCriadoEm()).isNotNull();
		assertThat(conta.getAtualizadoEm()).isNotNull();
	}

	@Test
	void devePersistirContaFinanceiraComSaldoInicialNegativo() {
		Empresa empresa = criarEmpresa("22222222000152");

		ContaFinanceira conta = contaFinanceiraRepository.saveAndFlush(new ContaFinanceira(
				empresa, "Conta Negativa", TipoContaFinanceira.CONTA_CORRENTE, new BigDecimal("-50.00"),
				StatusCadastro.ATIVO));

		assertThat(conta.getSaldoInicial()).isEqualByComparingTo("-50.00");
	}

	@Test
	void devePersistirCategoriaFinanceiraComUuidEAuditoriaTemporal() {
		Empresa empresa = criarEmpresa("33333333000153");

		CategoriaFinanceira categoria = categoriaFinanceiraRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, "Vendas", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO));

		assertThat(categoria.getId()).isNotNull();
		assertThat(categoria.getCriadoEm()).isNotNull();
		assertThat(categoria.getAtualizadoEm()).isNotNull();
	}

	@Test
	void devePersistirLancamentoFinanceiroComUuidEAuditoriaTemporal() {
		Empresa empresa = criarEmpresa("44444444000154");
		ContaFinanceira conta = contaFinanceiraRepository.saveAndFlush(new ContaFinanceira(
				empresa, "Caixa", TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO));
		CategoriaFinanceira categoria = categoriaFinanceiraRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, "Vendas", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO));

		LancamentoFinanceiro lancamento = lancamentoFinanceiroRepository.saveAndFlush(new LancamentoFinanceiro(
				empresa,
				conta,
				categoria,
				TipoFinanceiro.RECEITA,
				"Venda de teste",
				new BigDecimal("150.00"),
				LocalDate.now(),
				null,
				StatusLancamentoFinanceiro.PENDENTE,
				null));

		assertThat(lancamento.getId()).isNotNull();
		assertThat(lancamento.getCriadoEm()).isNotNull();
		assertThat(lancamento.getAtualizadoEm()).isNotNull();
		assertThat(lancamento.compoeSaldoRealizado()).isFalse();
	}

	@Test
	void lancamentoPagoComponeSaldoRealizado() {
		Empresa empresa = criarEmpresa("55555555000155");
		ContaFinanceira conta = contaFinanceiraRepository.saveAndFlush(new ContaFinanceira(
				empresa, "Caixa", TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO));
		CategoriaFinanceira categoria = categoriaFinanceiraRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, "Vendas", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO));

		LancamentoFinanceiro lancamento = new LancamentoFinanceiro(
				empresa,
				conta,
				categoria,
				TipoFinanceiro.RECEITA,
				"Venda paga",
				new BigDecimal("200.00"),
				LocalDate.now(),
				LocalDate.now(),
				StatusLancamentoFinanceiro.PAGO,
				null);

		assertThat(lancamento.compoeSaldoRealizado()).isTrue();
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}
}
