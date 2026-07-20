package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Normaliza valores monetarios para escala 2 (centavos), sempre via BigDecimal. */
final class MoedaUtils {

	private MoedaUtils() {
	}

	static BigDecimal normalizar(BigDecimal valor) {
		return valor.setScale(2, RoundingMode.HALF_UP);
	}
}
