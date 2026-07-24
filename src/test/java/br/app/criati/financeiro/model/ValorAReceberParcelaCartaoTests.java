package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.SituacaoValorAReceberCompraCartao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para ValorAReceberParcelaCartao. */
class ValorAReceberParcelaCartaoTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "51111111000101", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.valorareceber@criati.test", "hash", StatusCadastro.ATIVO);
	private final PessoaFinanceira titular = new PessoaFinanceira(empresa, "Titular", null, null, autor);
	private final InstituicaoFinanceira instituicao = new InstituicaoFinanceira(empresa, "Banco Teste", "999", autor);
	private final CartaoCredito cartao = new CartaoCredito(empresa, titular, instituicao, "Cartao", TipoCartao.FISICO,
			null, Bandeira.VISA, "1234", new BigDecimal("5000.00"), null, 5, 12, null, autor);
	private final CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, "Compras", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO);
	private final ParteFinanceira terceiro = new ParteFinanceira(empresa, "Amigo", TipoParteFinanceira.PESSOA, null, null, null, autor);
	private final CompraCartao compra = new CompraCartao(empresa, cartao, cartao, titular, categoria, terceiro,
			"Presente", LocalDate.of(2026, 8, 1), new BigDecimal("300.00"), 1, null, autor);

	private ParcelaCompraCartao parcela(LocalDate competencia, BigDecimal valor) {
		return new ParcelaCompraCartao(empresa, compra, 1, 1, valor, competencia, autor);
	}

	private ValorAReceberParcelaCartao valorAReceber(LocalDate competencia, BigDecimal valor) {
		return new ValorAReceberParcelaCartao(empresa, parcela(competencia, valor), autor);
	}

	@Test
	void criaValorAReceberPendenteComSaldoIgualAoValorDaParcela() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.PENDENTE);
		assertThat(v.getValorTotal()).isEqualByComparingTo("100.00");
		assertThat(v.getSaldoPendente()).isEqualByComparingTo("100.00");
		assertThat(v.getValorRecebido()).isEqualByComparingTo("0.00");
		assertThat(v.getVencimento()).isEqualTo(LocalDate.of(2026, 9, 1));
	}

	@Test
	void recebimentoParcialDeixaStatusParcialmenteRessarcida() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA);
		assertThat(v.getSaldoPendente()).isEqualByComparingTo("60.00");
		assertThat(v.getDataEfetivaRecebimento()).isEqualTo(LocalDate.of(2026, 8, 20));
	}

	@Test
	void somaDeRecebimentosQuitaOValorAReceber() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		v.registrarRecebimento(new BigDecimal("60.00"), LocalDate.of(2026, 8, 25), autor);
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.RESSARCIDA);
		assertThat(v.getSaldoPendente()).isEqualByComparingTo("0.00");
	}

	@Test
	void recebimentoExcedenteAoSaldoERejeitado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		assertThrows(DadosInvalidosException.class,
				() -> v.registrarRecebimento(new BigDecimal("100.01"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void recebimentoComValorZeroOuNegativoERejeitado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		assertThrows(DadosInvalidosException.class,
				() -> v.registrarRecebimento(BigDecimal.ZERO, LocalDate.of(2026, 8, 20), autor));
		assertThrows(DadosInvalidosException.class,
				() -> v.registrarRecebimento(new BigDecimal("-10.00"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void segundoRecebimentoAposQuitacaoERejeitado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), autor);
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.RESSARCIDA);
		assertThrows(DadosInvalidosException.class,
				() -> v.registrarRecebimento(new BigDecimal("0.01"), LocalDate.of(2026, 8, 21), autor));
	}

	@Test
	void estornoDeRecebimentoParcialRecalculaSaldoEStatus() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("40.00"), LocalDate.of(2026, 8, 20), autor);
		v.estornarRecebimento(new BigDecimal("40.00"), autor);
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.PENDENTE);
		assertThat(v.getValorRecebido()).isEqualByComparingTo("0.00");
		assertThat(v.getDataEfetivaRecebimento()).isNull();
	}

	@Test
	void valorCanceladoNaoRecebeRessarcimento() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.cancelar(autor);
		assertThat(v.getStatus()).isEqualTo(StatusValorAReceberCompraCartao.CANCELADA);
		assertThrows(DadosInvalidosException.class,
				() -> v.registrarRecebimento(new BigDecimal("10.00"), LocalDate.of(2026, 8, 20), autor));
	}

	@Test
	void valorJaRessarcidoNaoPodeSerCancelado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("100.00"), LocalDate.of(2026, 8, 20), autor);
		assertThrows(DadosInvalidosException.class, () -> v.cancelar(autor));
	}

	@Test
	void estaAtrasadoQuandoVencimentoPassouESaldoPendente() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2020, 1, 1), new BigDecimal("100.00"));
		LocalDate hoje = LocalDate.of(2026, 8, 1);
		assertThat(v.estaAtrasada(hoje)).isTrue();
		assertThat(v.diasEmAtraso(hoje))
				.isEqualTo(java.time.temporal.ChronoUnit.DAYS.between(LocalDate.of(2020, 1, 1), hoje));
		assertThat(v.getSituacao(hoje)).isEqualTo(SituacaoValorAReceberCompraCartao.ATRASADA);
	}

	@Test
	void naoEstaAtrasadoQuandoRessarcidoMesmoComVencimentoPassado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2020, 1, 1), new BigDecimal("100.00"));
		v.registrarRecebimento(new BigDecimal("100.00"), LocalDate.of(2020, 1, 5), autor);
		LocalDate hoje = LocalDate.of(2026, 8, 1);
		assertThat(v.estaAtrasada(hoje)).isFalse();
		assertThat(v.getSituacao(hoje)).isEqualTo(SituacaoValorAReceberCompraCartao.RESSARCIDA);
	}

	@Test
	void naoEstaAtrasadoQuandoCancelado() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2020, 1, 1), new BigDecimal("100.00"));
		v.cancelar(autor);
		assertThat(v.estaAtrasada(LocalDate.of(2026, 8, 1))).isFalse();
		assertThat(v.getSituacao(LocalDate.of(2026, 8, 1))).isEqualTo(SituacaoValorAReceberCompraCartao.CANCELADA);
	}

	@Test
	void registrarDataPrometidaNaoAlteraVencimento() {
		ValorAReceberParcelaCartao v = valorAReceber(LocalDate.of(2026, 9, 1), new BigDecimal("100.00"));
		v.registrarDataPrometida(LocalDate.of(2026, 9, 15), autor);
		assertThat(v.getDataPrometida()).isEqualTo(LocalDate.of(2026, 9, 15));
		assertThat(v.getVencimento()).isEqualTo(LocalDate.of(2026, 9, 1));
	}
}
