package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.UUID;

public record SaldoContaFinanceira(UUID contaId, String contaNome, BigDecimal saldoAtual) { }
