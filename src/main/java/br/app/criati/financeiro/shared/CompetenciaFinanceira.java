package br.app.criati.financeiro.shared;

import java.time.YearMonth;
import java.util.Locale;
import java.util.Objects;

/**
 * Identifica um mes de competencia financeira sem dia, horario ou fuso.
 */
public record CompetenciaFinanceira(int ano, int mes) implements Comparable<CompetenciaFinanceira> {

	private static final int ANO_MINIMO = 1;
	private static final int ANO_MAXIMO = 9999;

	public CompetenciaFinanceira {
		if (ano < ANO_MINIMO || ano > ANO_MAXIMO) {
			throw new IllegalArgumentException("Ano da competencia deve estar entre 1 e 9999");
		}
		if (mes < 1 || mes > 12) {
			throw new IllegalArgumentException("Mes da competencia deve estar entre 1 e 12");
		}
	}

	public static CompetenciaFinanceira de(YearMonth competencia) {
		Objects.requireNonNull(competencia, "competencia nao pode ser nula");
		return new CompetenciaFinanceira(competencia.getYear(), competencia.getMonthValue());
	}

	public static CompetenciaFinanceira parse(String valor) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("Competencia e obrigatoria");
		}

		String[] partes = valor.split("/", -1);
		if (partes.length != 2 || partes[0].length() != 2 || partes[1].length() != 4) {
			throw new IllegalArgumentException("Competencia deve estar no formato MM/AAAA");
		}

		try {
			return new CompetenciaFinanceira(Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Competencia deve estar no formato MM/AAAA", exception);
		}
	}

	public YearMonth paraYearMonth() {
		return YearMonth.of(ano, mes);
	}

	public String formatar() {
		return String.format(Locale.ROOT, "%02d/%04d", mes, ano);
	}

	@Override
	public int compareTo(CompetenciaFinanceira outra) {
		Objects.requireNonNull(outra, "competencia comparada nao pode ser nula");
		int comparacaoAno = Integer.compare(ano, outra.ano);
		return comparacaoAno != 0 ? comparacaoAno : Integer.compare(mes, outra.mes);
	}

	@Override
	public String toString() {
		return formatar();
	}
}
