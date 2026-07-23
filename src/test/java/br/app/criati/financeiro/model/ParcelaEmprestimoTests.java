package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.SituacaoParcelaEmprestimo;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para ParcelaEmprestimo. */
class ParcelaEmprestimoTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "22222222000199", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.parcela@criati.test", "hash", StatusCadastro.ATIVO);
	private final ParteFinanceira parte = new ParteFinanceira(empresa, "Amigo Devedor", TipoParteFinanceira.PESSOA, null, null, null, autor);
	private final CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, "Emprestimos", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO);
	private final EmprestimoConcedido emprestimo = new EmprestimoConcedido(empresa, parte, categoria, "Ajuda",
			new BigDecimal("300.00"), LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS, null, null,
			FormaPagamentoEmprestimo.PARCELADO, 3, autor);

	private ParcelaEmprestimo parcela(LocalDate vencimento) {
		return new ParcelaEmprestimo(empresa, emprestimo, 1, 3, new BigDecimal("100.00"), vencimento, autor);
	}

	@Test
	void criaParcelaPendenteComSaldoIgualAoPrincipal() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		assertThat(p.getStatus()).isEqualTo(StatusParcelaEmprestimo.PENDENTE);
		assertThat(p.getValorTotal()).isEqualByComparingTo("100.00");
		assertThat(p.getSaldoPendente()).isEqualByComparingTo("100.00");
		assertThat(p.getValorRecebido()).isEqualByComparingTo("0.00");
	}

	@Test
	void recebimentoParcialDeixaStatusParcialmentePago() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		assertThat(p.getStatus()).isEqualTo(StatusParcelaEmprestimo.PARCIALMENTE_PAGO);
		assertThat(p.getSaldoPendente()).isEqualByComparingTo("60.00");
		assertThat(p.getDataEfetivaPagamento()).isEqualTo(LocalDate.of(2026, 8, 20));
	}

	@Test
	void somaDeRecebimentosQuitaAParcela() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		p.registrarRecebimento(new BigDecimal("60.00"), LocalDate.of(2026, 8, 25), autor);
		assertThat(p.getStatus()).isEqualTo(StatusParcelaEmprestimo.PAGO);
		assertThat(p.getSaldoPendente()).isEqualByComparingTo("0.00");
		assertThat(p.getDataEfetivaPagamento()).isEqualTo(LocalDate.of(2026, 8, 25));
	}

	@Test
	void recebimentoExcedenteAoSaldoERejeitado() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		assertThrows(DadosInvalidosException.class,
				() -> p.registrarRecebimento(new BigDecimal("100.01"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void recebimentoComValorZeroOuNegativoERejeitado() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		assertThrows(DadosInvalidosException.class,
				() -> p.registrarRecebimento(BigDecimal.ZERO, LocalDate.of(2026, 8, 20), autor));
		assertThrows(DadosInvalidosException.class,
				() -> p.registrarRecebimento(new BigDecimal("-10.00"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void estornoDeRecebimentoParcialRecalculaSaldoEStatus() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		p.estornarRecebimento(new BigDecimal("40.00"), autor);
		assertThat(p.getStatus()).isEqualTo(StatusParcelaEmprestimo.PENDENTE);
		assertThat(p.getValorRecebido()).isEqualByComparingTo("0.00");
		assertThat(p.getDataEfetivaPagamento()).isNull();
	}

	@Test
	void parcelaCanceladaNaoRecebePagamento() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.cancelar(autor);
		assertThat(p.getStatus()).isEqualTo(StatusParcelaEmprestimo.CANCELADO);
		assertThrows(DadosInvalidosException.class,
				() -> p.registrarRecebimento(new BigDecimal("10.00"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void parcelaJaPagaNaoPodeSerCancelada() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.registrarRecebimento(new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), autor);
		assertThrows(DadosInvalidosException.class, () -> p.cancelar(autor));
	}

	@Test
	void estaAtrasadaQuandoVencimentoPassouESaldoPendente() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2020, 1, 1));
		LocalDate hoje = LocalDate.of(2026, 8, 1);
		assertThat(p.estaAtrasada(hoje)).isTrue();
		assertThat(p.diasEmAtraso(hoje)).isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 1), hoje));
		assertThat(p.getSituacao(hoje)).isEqualTo(SituacaoParcelaEmprestimo.ATRASADO);
	}

	@Test
	void naoEstaAtrasadaQuandoPagaMesmoComVencimentoPassado() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2020, 1, 1));
		p.registrarRecebimento(new BigDecimal("100.00"), LocalDate.of(2020, 1, 5), autor);
		LocalDate hoje = LocalDate.of(2026, 8, 1);
		assertThat(p.estaAtrasada(hoje)).isFalse();
		assertThat(p.getSituacao(hoje)).isEqualTo(SituacaoParcelaEmprestimo.PAGO);
	}

	@Test
	void naoEstaAtrasadaQuandoCancelada() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2020, 1, 1));
		p.cancelar(autor);
		assertThat(p.estaAtrasada(LocalDate.of(2026, 8, 1))).isFalse();
		assertThat(p.getSituacao(LocalDate.of(2026, 8, 1))).isEqualTo(SituacaoParcelaEmprestimo.CANCELADO);
	}

	@Test
	void registrarDataPrometidaNaoAlteraVencimento() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.registrarDataPrometida(LocalDate.of(2026, 9, 15), autor);
		assertThat(p.getDataPrometida()).isEqualTo(LocalDate.of(2026, 9, 15));
		assertThat(p.getVencimento()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	@Test
	void aplicarEncargosSubstituiValoresERecalculaTotal() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.aplicarEncargos(new BigDecimal("5.00"), new BigDecimal("10.00"), autor);
		assertThat(p.getValorTotal()).isEqualByComparingTo("115.00");
		assertThat(p.getSaldoPendente()).isEqualByComparingTo("115.00");
		p.aplicarEncargos(new BigDecimal("2.00"), new BigDecimal("0.00"), autor);
		assertThat(p.getValorTotal()).isEqualByComparingTo("102.00");
	}

	@Test
	void aplicarEncargosNaoPodeReduzirTotalAbaixoDoJaRecebido() {
		ParcelaEmprestimo p = parcela(LocalDate.of(2026, 9, 1));
		p.aplicarEncargos(new BigDecimal("20.00"), BigDecimal.ZERO, autor);
		p.registrarRecebimento(new BigDecimal("110.00"), LocalDate.of(2026, 8, 20), autor);
		assertThrows(DadosInvalidosException.class, () -> p.aplicarEncargos(BigDecimal.ZERO, BigDecimal.ZERO, autor));
	}
}
