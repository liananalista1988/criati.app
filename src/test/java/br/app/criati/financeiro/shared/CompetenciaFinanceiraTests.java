package br.app.criati.financeiro.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

class CompetenciaFinanceiraTests {

	@Test
	void deveRepresentarCompetenciaSemDiaOuFuso() {
		CompetenciaFinanceira competencia = new CompetenciaFinanceira(2026, 7);

		assertThat(competencia.ano()).isEqualTo(2026);
		assertThat(competencia.mes()).isEqualTo(7);
		assertThat(competencia.paraYearMonth()).isEqualTo(YearMonth.of(2026, 7));
		assertThat(competencia).hasToString("07/2026");
	}

	@Test
	void deveCriarAPartirDeYearMonth() {
		assertThat(CompetenciaFinanceira.de(YearMonth.of(2025, 12)))
				.isEqualTo(new CompetenciaFinanceira(2025, 12));
	}

	@Test
	void deveInterpretarFormatoMesAno() {
		assertThat(CompetenciaFinanceira.parse("02/2024"))
				.isEqualTo(new CompetenciaFinanceira(2024, 2));
	}

	@Test
	void deveRejeitarMesInvalido() {
		assertThatThrownBy(() -> new CompetenciaFinanceira(2026, 13))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Mes da competencia deve estar entre 1 e 12");
	}

	@Test
	void deveRejeitarAnoForaDoIntervaloSuportado() {
		assertThatThrownBy(() -> new CompetenciaFinanceira(0, 1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Ano da competencia deve estar entre 1 e 9999");
	}

	@Test
	void deveRejeitarTextoEmFormatoInvalido() {
		assertThatThrownBy(() -> CompetenciaFinanceira.parse("2026-07"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Competencia deve estar no formato MM/AAAA");
	}

	@Test
	void deveOrdenarPorAnoEMes() {
		CompetenciaFinanceira janeiro = new CompetenciaFinanceira(2026, 1);
		CompetenciaFinanceira fevereiro = new CompetenciaFinanceira(2026, 2);
		CompetenciaFinanceira dezembroAnterior = new CompetenciaFinanceira(2025, 12);

		assertThat(dezembroAnterior).isLessThan(janeiro);
		assertThat(janeiro).isLessThan(fevereiro);
	}

	@Test
	void deveRejeitarCompetenciaNula() {
		assertThatThrownBy(() -> CompetenciaFinanceira.de(null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("competencia nao pode ser nula");
	}
}
