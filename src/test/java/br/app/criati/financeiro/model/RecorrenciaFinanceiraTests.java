package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para as regras de calculo de recorrencia. */
class RecorrenciaFinanceiraTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "11111111000199", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.recorrencia@criati.test", "hash", StatusCadastro.ATIVO);
	private final ContaFinanceira conta = new ContaFinanceira(empresa, "Conta", TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO);
	private final CategoriaFinanceira categoriaReceita = new CategoriaFinanceira(empresa, "Salario", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO);
	private final CategoriaFinanceira categoriaDespesa = new CategoriaFinanceira(empresa, "Condominio", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO);
	private final PessoaFinanceira pessoa = new PessoaFinanceira(empresa, "Pessoa", null, null, autor);

	private RecorrenciaFinanceira mensal(int dia, LocalDate dataInicial) {
		return new RecorrenciaFinanceira(empresa, TipoFinanceiro.RECEITA, "Salario mensal", new BigDecimal("100.00"),
				conta, categoriaReceita, pessoa, null, null, PeriodicidadeRecorrencia.MENSAL, 1, dia, null,
				dataInicial, null, false, null, autor);
	}

	private RecorrenciaFinanceira anual(int dia, int mes, LocalDate dataInicial) {
		return new RecorrenciaFinanceira(empresa, TipoFinanceiro.DESPESA, "Assinatura anual", new BigDecimal("300.00"),
				conta, categoriaDespesa, pessoa, null, null, PeriodicidadeRecorrencia.ANUAL, 1, dia, mes,
				dataInicial, null, false, null, autor);
	}

	@Test
	void criaRecorrenciaMensalDeReceitaComCamposBasicos() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 7, 10));
		assertThat(r.getTipo()).isEqualTo(TipoFinanceiro.RECEITA);
		assertThat(r.getStatus()).isEqualTo(StatusRecorrencia.ATIVA);
		assertThat(r.getValorPadrao()).isEqualByComparingTo("100.00");
		assertThat(r.getPeriodicidade()).isEqualTo(PeriodicidadeRecorrencia.MENSAL);
		assertThat(r.getMesReferencia()).isNull();
	}

	@Test
	void criaRecorrenciaMensalDeDespesaComCompetenciaInicialIgualAoMesDaDataInicial() {
		RecorrenciaFinanceira r = new RecorrenciaFinanceira(empresa, TipoFinanceiro.DESPESA, "Condominio", new BigDecimal("500.00"),
				conta, categoriaDespesa, pessoa, null, null, PeriodicidadeRecorrencia.MENSAL, 1, 10, null,
				LocalDate.of(2026, 3, 15), null, false, null, autor);
		assertThat(r.competenciaInicial()).isEqualTo(YearMonth.of(2026, 3));
		assertThat(r.getProximaCompetencia()).isEqualTo(YearMonth.of(2026, 3));
	}

	@Test
	void descricaoNulaNaoEAceitaPelaEntidade() {
		org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new RecorrenciaFinanceira(
				empresa, TipoFinanceiro.RECEITA, null, new BigDecimal("100.00"), conta, categoriaReceita, pessoa,
				null, null, PeriodicidadeRecorrencia.MENSAL, 1, 5, null, LocalDate.of(2026, 1, 1), null, false,
				null, autor));
	}

	@Test
	void pessoaFinanceiraNulaNaoEAceitaPelaEntidade() {
		org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new RecorrenciaFinanceira(
				empresa, TipoFinanceiro.RECEITA, "Salario", new BigDecimal("100.00"), conta, categoriaReceita, null,
				null, null, PeriodicidadeRecorrencia.MENSAL, 1, 5, null, LocalDate.of(2026, 1, 1), null, false,
				null, autor));
	}

	@Test
	void criaRecorrenciaAnualComMesReferenciaAposDataInicialUsaOMesmoAno() {
		RecorrenciaFinanceira r = anual(15, 8, LocalDate.of(2026, 3, 1));
		assertThat(r.competenciaInicial()).isEqualTo(YearMonth.of(2026, 8));
	}

	@Test
	void criaRecorrenciaAnualComMesReferenciaAntesDaDataInicialRolaParaOProximoAno() {
		RecorrenciaFinanceira r = anual(15, 2, LocalDate.of(2026, 8, 1));
		assertThat(r.competenciaInicial()).isEqualTo(YearMonth.of(2027, 2));
	}

	@Test
	void calculaVencimentoNoDiaConfiguradoQuandoValidoParaOMes() {
		RecorrenciaFinanceira r = mensal(10, LocalDate.of(2026, 1, 1));
		assertThat(r.calcularVencimento(YearMonth.of(2026, 7))).isEqualTo(LocalDate.of(2026, 7, 10));
	}

	@Test
	void diaTrintaEUmEmFevereiroNaoBissextoUsaUltimoDiaDoMes() {
		RecorrenciaFinanceira r = mensal(31, LocalDate.of(2026, 1, 1));
		assertThat(r.calcularVencimento(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void diaTrintaEUmEmFevereiroBissextoUsaUltimoDiaDoMes() {
		RecorrenciaFinanceira r = mensal(31, LocalDate.of(2027, 1, 1));
		assertThat(r.calcularVencimento(YearMonth.of(2028, 2))).isEqualTo(LocalDate.of(2028, 2, 29));
	}

	@Test
	void diaTrintaEmFevereiroTambemUsaUltimoDiaDoMes() {
		RecorrenciaFinanceira r = mensal(30, LocalDate.of(2026, 1, 1));
		assertThat(r.calcularVencimento(YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28));
	}

	@Test
	void recorrenciaAnualEm29DeFevereiroUsaUltimoDiaDoMesEmAnoNaoBissexto() {
		RecorrenciaFinanceira r = anual(29, 2, LocalDate.of(2024, 1, 1));
		assertThat(r.calcularVencimento(YearMonth.of(2024, 2))).isEqualTo(LocalDate.of(2024, 2, 29));
		assertThat(r.calcularVencimento(YearMonth.of(2025, 2))).isEqualTo(LocalDate.of(2025, 2, 28));
	}

	@Test
	void calculaProximaCompetenciaMensalRespeitandoOIntervalo() {
		RecorrenciaFinanceira r = new RecorrenciaFinanceira(empresa, TipoFinanceiro.DESPESA, "Trimestral", new BigDecimal("50.00"),
				conta, categoriaDespesa, pessoa, null, null, PeriodicidadeRecorrencia.MENSAL, 3, 5, null,
				LocalDate.of(2026, 1, 1), null, false, null, autor);
		assertThat(r.calcularProximaCompetencia(YearMonth.of(2026, 1))).isEqualTo(YearMonth.of(2026, 4));
	}

	@Test
	void calculaProximaCompetenciaAnualMantendoOMes() {
		RecorrenciaFinanceira r = anual(10, 6, LocalDate.of(2026, 1, 1));
		assertThat(r.calcularProximaCompetencia(YearMonth.of(2026, 6))).isEqualTo(YearMonth.of(2027, 6));
	}

	@Test
	void avancarProximaCompetenciaAtualizaOCursorSemDuplicarLogica() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 1, 1));
		assertThat(r.getProximaCompetencia()).isEqualTo(YearMonth.of(2026, 1));
		r.avancarProximaCompetencia();
		assertThat(r.getProximaCompetencia()).isEqualTo(YearMonth.of(2026, 2));
	}

	@Test
	void dentroDoPeriodoRespeitaDataInicialEDataFinal() {
		RecorrenciaFinanceira r = new RecorrenciaFinanceira(empresa, TipoFinanceiro.RECEITA, "Bolsa", new BigDecimal("10.00"),
				conta, categoriaReceita, pessoa, null, null, PeriodicidadeRecorrencia.MENSAL, 1, 1, null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30), false, null, autor);
		assertThat(r.dentroDoPeriodo(YearMonth.of(2026, 2))).isFalse();
		assertThat(r.dentroDoPeriodo(YearMonth.of(2026, 3))).isTrue();
		assertThat(r.dentroDoPeriodo(YearMonth.of(2026, 6))).isTrue();
		assertThat(r.dentroDoPeriodo(YearMonth.of(2026, 7))).isFalse();
	}

	@Test
	void pausarRegistraUsuarioEData() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 1, 1));
		r.pausar(autor);
		assertThat(r.getStatus()).isEqualTo(StatusRecorrencia.PAUSADA);
		assertThat(r.getPausadaEm()).isNotNull();
		assertThat(r.getPausadaPor()).isEqualTo(autor);
	}

	@Test
	void retomarRecalculaProximaCompetenciaQuandoAtrasada() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 1, 1));
		r.pausar(autor);
		r.retomar(YearMonth.of(2026, 6), autor);
		assertThat(r.getStatus()).isEqualTo(StatusRecorrencia.ATIVA);
		assertThat(r.getProximaCompetencia()).isEqualTo(YearMonth.of(2026, 6));
	}

	@Test
	void retomarNaoRetrocedeQuandoProximaCompetenciaJaEstaAdiante() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 6, 1));
		r.pausar(autor);
		r.retomar(YearMonth.of(2026, 1), autor);
		assertThat(r.getProximaCompetencia()).isEqualTo(YearMonth.of(2026, 6));
	}

	@Test
	void encerrarRegistraUsuarioEData() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 1, 1));
		r.encerrar(autor);
		assertThat(r.getStatus()).isEqualTo(StatusRecorrencia.ENCERRADA);
		assertThat(r.getEncerradaEm()).isNotNull();
		assertThat(r.getEncerradaPor()).isEqualTo(autor);
	}

	@Test
	void elegivelParaGeracaoAutomaticaRequerAtivaEGerarAutomaticamenteEDentroDoPeriodo() {
		RecorrenciaFinanceira semAutomatica = mensal(5, LocalDate.of(2026, 1, 1));
		assertThat(semAutomatica.elegivelParaGeracaoAutomatica(YearMonth.of(2026, 1))).isFalse();

		RecorrenciaFinanceira automatica = new RecorrenciaFinanceira(empresa, TipoFinanceiro.RECEITA, "Auto", new BigDecimal("10.00"),
				conta, categoriaReceita, pessoa, null, null, PeriodicidadeRecorrencia.MENSAL, 1, 5, null,
				LocalDate.of(2026, 1, 1), null, true, null, autor);
		assertThat(automatica.elegivelParaGeracaoAutomatica(YearMonth.of(2025, 12))).isFalse();
		assertThat(automatica.elegivelParaGeracaoAutomatica(YearMonth.of(2026, 1))).isTrue();
		automatica.encerrar(autor);
		assertThat(automatica.elegivelParaGeracaoAutomatica(YearMonth.of(2026, 1))).isFalse();
	}

	@Test
	void atualizarSerieNaoAlteraProximaCompetenciaOuPeriodicidade() {
		RecorrenciaFinanceira r = mensal(5, LocalDate.of(2026, 1, 1));
		YearMonth proximaAntes = r.getProximaCompetencia();
		r.atualizarSerie("Salario reajustado", new BigDecimal("150.00"), conta, categoriaReceita, pessoa, null,
				null, 1, 15, null, null, false, "Ajuste anual", autor);
		assertThat(r.getProximaCompetencia()).isEqualTo(proximaAntes);
		assertThat(r.getPeriodicidade()).isEqualTo(PeriodicidadeRecorrencia.MENSAL);
		assertThat(r.getValorPadrao()).isEqualByComparingTo("150.00");
		assertThat(r.getDiaReferencia()).isEqualTo(15);
	}

	@Test
	void gerarDeRecorrenciaCriaLancamentoComOrigemRecorrenciaEReferenciaDaSerie() {
		RecorrenciaFinanceira r = mensal(10, LocalDate.of(2026, 1, 1));
		LancamentoFinanceiro gerado = LancamentoFinanceiro.gerarDeRecorrencia(r, LocalDate.of(2026, 7, 1),
				LocalDate.of(2026, 7, 10), autor);
		assertThat(gerado.getOrigem()).isEqualTo(OrigemLancamentoFinanceiro.RECORRENCIA);
		assertThat(gerado.getRecorrencia()).isEqualTo(r);
		assertThat(gerado.getDataCompetencia()).isEqualTo(LocalDate.of(2026, 7, 1));
		assertThat(gerado.getDataVencimento()).isEqualTo(LocalDate.of(2026, 7, 10));
		assertThat(gerado.getValor()).isEqualByComparingTo(r.getValorPadrao());
		assertThat(gerado.getConta()).isEqualTo(r.getConta());
		assertThat(gerado.getCategoria()).isEqualTo(r.getCategoria());
	}
}
