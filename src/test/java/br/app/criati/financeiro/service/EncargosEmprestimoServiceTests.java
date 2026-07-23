package br.app.criati.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/**
 * Testes de dominio puros (sem Spring) para EncargosEmprestimoService: garante
 * que juros e multa so sao calculados quando a TipoCobrancaEmprestimo
 * configurada exige (criterio de aceite "Juros e multa so sao calculados
 * quando configurados").
 */
class EncargosEmprestimoServiceTests {

	private final EncargosEmprestimoService service = new EncargosEmprestimoService();
	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "33333333000199", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.encargos@criati.test", "hash", StatusCadastro.ATIVO);
	private final ParteFinanceira parte = new ParteFinanceira(empresa, "Devedor", TipoParteFinanceira.PESSOA, null, null, null, autor);
	private final CategoriaFinanceira categoria = new CategoriaFinanceira(empresa, "Emprestimos", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO);

	private final LocalDate dataConcessao = LocalDate.of(2026, 1, 1);
	private final LocalDate vencimento = LocalDate.of(2026, 2, 1);
	private final BigDecimal valorParcela = new BigDecimal("1000.00");

	private EmprestimoConcedido emprestimo(TipoCobrancaEmprestimo tipo, BigDecimal percentualJuros, BigDecimal percentualMulta) {
		return new EmprestimoConcedido(empresa, parte, categoria, null, new BigDecimal("1000.00"), dataConcessao, tipo,
				percentualJuros, percentualMulta, FormaPagamentoEmprestimo.UNICO, 1, autor);
	}

	@Test
	void semJurosNuncaGeraEncargoMesmoEmAtraso() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.SEM_JUROS, null, null);
		LocalDate referencia = vencimento.plusDays(30);
		assertThat(service.calcularJuros(e, valorParcela, vencimento, referencia)).isEqualByComparingTo("0.00");
		assertThat(service.calcularMulta(e, valorParcela, vencimento, referencia)).isEqualByComparingTo("0.00");
	}

	@Test
	void alertaAtrasoNuncaGeraEncargoMesmoEmAtraso() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.ALERTA_ATRASO, null, null);
		LocalDate referencia = vencimento.plusDays(30);
		assertThat(service.calcularJuros(e, valorParcela, vencimento, referencia)).isEqualByComparingTo("0.00");
		assertThat(service.calcularMulta(e, valorParcela, vencimento, referencia)).isEqualByComparingTo("0.00");
	}

	@Test
	void comJurosAcresceMesmoAntesDoVencimento() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.COM_JUROS, new BigDecimal("3.00"), null);
		LocalDate referencia = dataConcessao.plusDays(15);
		BigDecimal juros = service.calcularJuros(e, valorParcela, vencimento, referencia);
		assertThat(juros).isGreaterThan(BigDecimal.ZERO);
		// 15 dias, taxa mensal 3% (base 30 dias): 1000 * 0.03/30 * 15 = 15.00
		assertThat(juros).isEqualByComparingTo("15.00");
		assertThat(service.calcularMulta(e, valorParcela, vencimento, referencia)).isEqualByComparingTo("0.00");
	}

	@Test
	void comJurosNaoGeraValorAntesDaConcessao() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.COM_JUROS, new BigDecimal("3.00"), null);
		assertThat(service.calcularJuros(e, valorParcela, vencimento, dataConcessao)).isEqualByComparingTo("0.00");
	}

	@Test
	void multaAtrasoSoAcresceAposVencimento() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.MULTA_ATRASO, null, new BigDecimal("10.00"));
		assertThat(service.calcularMulta(e, valorParcela, vencimento, vencimento)).isEqualByComparingTo("0.00");
		BigDecimal multa = service.calcularMulta(e, valorParcela, vencimento, vencimento.plusDays(1));
		assertThat(multa).isEqualByComparingTo("100.00");
		assertThat(service.calcularJuros(e, valorParcela, vencimento, vencimento.plusDays(30))).isEqualByComparingTo("0.00");
	}

	@Test
	void multaAtrasoNaoCresceComMaisDiasDeAtraso() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.MULTA_ATRASO, null, new BigDecimal("10.00"));
		BigDecimal multaDoisDias = service.calcularMulta(e, valorParcela, vencimento, vencimento.plusDays(2));
		BigDecimal multaTrintaDias = service.calcularMulta(e, valorParcela, vencimento, vencimento.plusDays(30));
		assertThat(multaDoisDias).isEqualByComparingTo(multaTrintaDias);
	}

	@Test
	void jurosMoraAtrasoSoAcresceAposVencimentoEProRataDiaria() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.JUROS_MORA_ATRASO, new BigDecimal("3.00"), null);
		assertThat(service.calcularJuros(e, valorParcela, vencimento, vencimento)).isEqualByComparingTo("0.00");
		// 10 dias em atraso, taxa mensal 3% (base 30 dias): 1000 * 0.03/30 * 10 = 10.00
		BigDecimal juros = service.calcularJuros(e, valorParcela, vencimento, vencimento.plusDays(10));
		assertThat(juros).isEqualByComparingTo("10.00");
		assertThat(service.calcularMulta(e, valorParcela, vencimento, vencimento.plusDays(10))).isEqualByComparingTo("0.00");
	}

	@Test
	void jurosMoraAtrasoNaoConsideraDiasAntesDoVencimento() {
		EmprestimoConcedido e = emprestimo(TipoCobrancaEmprestimo.JUROS_MORA_ATRASO, new BigDecimal("3.00"), null);
		assertThat(service.calcularJuros(e, valorParcela, vencimento, dataConcessao)).isEqualByComparingTo("0.00");
	}
}
