package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransacaoOfxExtraida(
		LocalDate data,
		BigDecimal valor,
		String tipoBancario,
		String descricao,
		String identificadorBancario,
		String documento) {
}
