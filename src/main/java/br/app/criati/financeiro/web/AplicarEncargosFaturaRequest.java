package br.app.criati.financeiro.web;

import java.math.BigDecimal;

/**
 * juros/multa manuais (LES-F3-005) - nulo tratado como zero pelo service;
 * substituem sempre os valores vigentes, nunca acumulam entre chamadas.
 */
public record AplicarEncargosFaturaRequest(BigDecimal juros, BigDecimal multa) {
}
