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
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para EmprestimoConcedido. */
class EmprestimoConcedidoTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "11111111000199", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.emprestimo@criati.test", "hash", StatusCadastro.ATIVO);
	private final ParteFinanceira parte = new ParteFinanceira(empresa, "Amigo Devedor", TipoParteFinanceira.PESSOA, null, null, null, autor);
	private final CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, "Emprestimos", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO);

	private EmprestimoConcedido emprestimo(TipoCobrancaEmprestimo tipoCobranca, BigDecimal percentualJuros,
			BigDecimal percentualMulta, FormaPagamentoEmprestimo formaPagamento, int quantidadeParcelas) {
		return new EmprestimoConcedido(empresa, parte, categoria, "Ajuda emergencial", new BigDecimal("1000.00"),
				LocalDate.of(2026, 8, 1), tipoCobranca, percentualJuros, percentualMulta, formaPagamento,
				quantidadeParcelas, autor);
	}

	@Test
	void criaEmprestimoAtivoSemJuros() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1);
		assertThat(e.getStatus()).isEqualTo(StatusEmprestimoConcedido.ATIVO);
		assertThat(e.getValorPrincipal()).isEqualByComparingTo("1000.00");
		assertThat(e.getQuantidadeParcelas()).isEqualTo(1);
	}

	@Test
	void valorPrincipalZeroOuNegativoERejeitado() {
		assertThrows(DadosInvalidosException.class, () -> new EmprestimoConcedido(empresa, parte, categoria, null,
				BigDecimal.ZERO, LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS, null, null,
				FormaPagamentoEmprestimo.UNICO, 1, autor));
	}

	@Test
	void comJurosExigePercentualJuros() {
		assertThrows(DadosInvalidosException.class,
				() -> emprestimo(TipoCobrancaEmprestimo.COM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1));
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.COM_JUROS, new BigDecimal("2.5"), null,
				FormaPagamentoEmprestimo.UNICO, 1);
		assertThat(e.getPercentualJuros()).isEqualByComparingTo("2.5000");
	}

	@Test
	void jurosMoraAtrasoExigePercentualJuros() {
		assertThrows(DadosInvalidosException.class, () -> emprestimo(TipoCobrancaEmprestimo.JUROS_MORA_ATRASO, null,
				null, FormaPagamentoEmprestimo.UNICO, 1));
	}

	@Test
	void multaAtrasoExigePercentualMulta() {
		assertThrows(DadosInvalidosException.class,
				() -> emprestimo(TipoCobrancaEmprestimo.MULTA_ATRASO, null, null, FormaPagamentoEmprestimo.UNICO, 1));
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.MULTA_ATRASO, null, new BigDecimal("10"),
				FormaPagamentoEmprestimo.UNICO, 1);
		assertThat(e.getPercentualMulta()).isEqualByComparingTo("10.0000");
	}

	@Test
	void percentualJurosInformadoIndevidamenteERejeitado() {
		assertThrows(DadosInvalidosException.class, () -> emprestimo(TipoCobrancaEmprestimo.SEM_JUROS,
				new BigDecimal("1.0"), null, FormaPagamentoEmprestimo.UNICO, 1));
	}

	@Test
	void percentualMultaInformadoIndevidamenteERejeitado() {
		assertThrows(DadosInvalidosException.class, () -> emprestimo(TipoCobrancaEmprestimo.ALERTA_ATRASO, null,
				new BigDecimal("5.0"), FormaPagamentoEmprestimo.UNICO, 1));
	}

	@Test
	void pagamentoUnicoExigeExatamenteUmaParcela() {
		assertThrows(DadosInvalidosException.class,
				() -> emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 3));
	}

	@Test
	void parceladoAceitaMultiplasParcelas() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null,
				FormaPagamentoEmprestimo.PARCELADO, 5);
		assertThat(e.getQuantidadeParcelas()).isEqualTo(5);
	}

	@Test
	void quantidadeDeParcelasMenorQueUmERejeitada() {
		assertThrows(DadosInvalidosException.class, () -> emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null,
				FormaPagamentoEmprestimo.PARCELADO, 0));
	}

	@Test
	void cancelarMarcaStatusEMotivo() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1);
		e.cancelar("Acordo desfeito", autor);
		assertThat(e.getStatus()).isEqualTo(StatusEmprestimoConcedido.CANCELADO);
		assertThat(e.getMotivoCancelamento()).isEqualTo("Acordo desfeito");
		assertThat(e.getCanceladoEm()).isNotNull();
	}

	@Test
	void cancelarDuasVezesERejeitado() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1);
		e.cancelar("Motivo", autor);
		assertThrows(DadosInvalidosException.class, () -> e.cancelar("Outro motivo", autor));
	}

	@Test
	void marcarQuitadoEReabrirAlternamStatus() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1);
		e.marcarQuitado(autor);
		assertThat(e.getStatus()).isEqualTo(StatusEmprestimoConcedido.QUITADO);
		e.reabrir(autor);
		assertThat(e.getStatus()).isEqualTo(StatusEmprestimoConcedido.ATIVO);
	}

	@Test
	void marcarQuitadoNaoReabreEmprestimoCancelado() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1);
		e.cancelar("Motivo", autor);
		e.marcarQuitado(autor);
		assertThat(e.getStatus()).isEqualTo(StatusEmprestimoConcedido.CANCELADO);
	}
}
