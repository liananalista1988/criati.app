package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.shared.enums.TipoFinanceiro;

public record ResumoCategoriaFinanceira(UUID categoriaId, String categoriaNome, TipoFinanceiro tipo, BigDecimal total) {
}
