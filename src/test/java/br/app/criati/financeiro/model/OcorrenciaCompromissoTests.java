package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para calculo de valores, status, vencimento e atraso. */
class OcorrenciaCompromissoTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "11111111000188", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.ocorrencia@criati.test", "hash", StatusCadastro.ATIVO);
	private final ContaFinanceira conta = new ContaFinanceira(empresa, "Conta", TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO);
	private final CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, "Energia", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO);
	private final PessoaFinanceira pessoa = new PessoaFinanceira(empresa, "Pessoa", null, null, autor);

	private OcorrenciaCompromisso avulsa(BigDecimal principal, BigDecimal juros, BigDecimal multa, BigDecimal desconto,
			LocalDate vencimento) {
		return new OcorrenciaCompromisso(empresa, null, null, LocalDate.of(2026, 8, 1), "Energia 08/2026", categoria,
				pessoa, null, conta, null, principal, vencimento, null, juros, multa, desconto, null, autor);
	}

	@Test
	void criaOcorrenciaPendenteComSaldoIgualAoTotal() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("350.00"), null, null, null, LocalDate.of(2026, 8, 15));
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PENDENTE);
		assertThat(o.getValorTotal()).isEqualByComparingTo("350.00");
		assertThat(o.getValorPago()).isEqualByComparingTo("0.00");
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("350.00");
	}

	@Test
	void valorTotalSomaJurosEMultaESubtraiDesconto() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), new BigDecimal("5.00"), new BigDecimal("10.00"),
				new BigDecimal("3.00"), LocalDate.of(2026, 8, 15));
		assertThat(o.getValorTotal()).isEqualByComparingTo("112.00");
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("112.00");
	}

	@Test
	void descontoAcimaDePrincipalMaisAcrescimosELancaExcecaoNaCriacao() {
		assertThrows(DadosInvalidosException.class,
				() -> avulsa(new BigDecimal("100.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("101.00"),
						LocalDate.of(2026, 8, 15)));
	}

	@Test
	void componentesMonetariosNegativosSaoRejeitados() {
		assertThrows(DadosInvalidosException.class,
				() -> avulsa(new BigDecimal("100.00"), new BigDecimal("-1.00"), BigDecimal.ZERO, BigDecimal.ZERO,
						LocalDate.of(2026, 8, 15)));
	}

	@Test
	void vencimentoNuloNaoEAceitoPelaEntidade() {
		assertThrows(NullPointerException.class, () -> avulsa(new BigDecimal("100.00"), null, null, null, null));
	}

	@Test
	void estaVencidaQuandoVencimentoPassouESaldoPendente() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2020, 1, 1));
		LocalDate hoje = LocalDate.of(2026, 8, 1);
		assertThat(o.estaVencida(hoje)).isTrue();
		assertThat(o.diasEmAtraso(hoje)).isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 1), hoje));
	}

	@Test
	void naoEstaVencidaQuandoQuitadaMesmoComVencimentoPassado() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2020, 1, 1));
		o.registrarPagamento(new BigDecimal("100.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PAGA);
		assertThat(o.estaVencida(LocalDate.of(2026, 8, 1))).isFalse();
		assertThat(o.diasEmAtraso(LocalDate.of(2026, 8, 1))).isZero();
	}

	@Test
	void naoEstaVencidaQuandoCancelada() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2020, 1, 1));
		o.cancelar(autor);
		assertThat(o.estaVencida(LocalDate.of(2026, 8, 1))).isFalse();
	}

	@Test
	void pagamentoParcialDeixaStatusParcialmentePagaEPreservaSaldo() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("40.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PARCIALMENTE_PAGA);
		assertThat(o.getValorPago()).isEqualByComparingTo("40.00");
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("60.00");
	}

	@Test
	void somaDePagamentosParciaisQuitaAOcorrencia() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("40.00"), autor);
		o.registrarPagamento(new BigDecimal("60.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PAGA);
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("0.00");
	}

	@Test
	void pagamentoComValorZeroOuNegativoELancaExcecao() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		assertThrows(DadosInvalidosException.class, () -> o.registrarPagamento(BigDecimal.ZERO, autor));
		assertThrows(DadosInvalidosException.class, () -> o.registrarPagamento(new BigDecimal("-10.00"), autor));
	}

	@Test
	void pagamentoExcedenteAoSaldoPendenteERejeitado() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		assertThrows(DadosInvalidosException.class, () -> o.registrarPagamento(new BigDecimal("100.01"), autor));
	}

	@Test
	void ocorrenciaCanceladaNaoRecebeNovosPagamentos() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.cancelar(autor);
		assertThrows(DadosInvalidosException.class, () -> o.registrarPagamento(new BigDecimal("10.00"), autor));
	}

	@Test
	void estornoDePagamentoParcialRecalculaValorPagoEStatus() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("40.00"), autor);
		o.estornarPagamento(new BigDecimal("40.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PENDENTE);
		assertThat(o.getValorPago()).isEqualByComparingTo("0.00");
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("100.00");
	}

	@Test
	void estornoDeUmDosPagamentosVoltaParaParcialmentePaga() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("40.00"), autor);
		o.registrarPagamento(new BigDecimal("60.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PAGA);
		o.estornarPagamento(new BigDecimal("60.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PARCIALMENTE_PAGA);
		assertThat(o.getValorPago()).isEqualByComparingTo("40.00");
	}

	@Test
	void alterarValorAposPagamentoNaoPodeInvalidarPagamentoJaRealizado() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("80.00"), autor);
		assertThrows(DadosInvalidosException.class,
				() -> o.atualizarValores(null, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, autor));
	}

	@Test
	void atualizarValoresRecalculaSaldoEStatus() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.registrarPagamento(new BigDecimal("100.00"), autor);
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PAGA);
		o.atualizarValores(null, new BigDecimal("100.00"), new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, autor);
		assertThat(o.getValorTotal()).isEqualByComparingTo("120.00");
		assertThat(o.getSaldoPendente()).isEqualByComparingTo("20.00");
		assertThat(o.getStatus()).isEqualTo(StatusOcorrenciaCompromisso.PARCIALMENTE_PAGA);
	}

	@Test
	void ocorrenciaCanceladaNaoPodeSerEditada() {
		OcorrenciaCompromisso o = avulsa(new BigDecimal("100.00"), null, null, null, LocalDate.of(2026, 8, 15));
		o.cancelar(autor);
		assertThrows(DadosInvalidosException.class,
				() -> o.atualizarValores(null, new BigDecimal("50.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, autor));
		assertThrows(DadosInvalidosException.class, () -> o.atualizarVencimento(LocalDate.of(2026, 9, 1), autor));
	}
}
