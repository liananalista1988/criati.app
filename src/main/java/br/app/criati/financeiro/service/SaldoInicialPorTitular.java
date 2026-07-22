package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.UUID;

public record SaldoInicialPorTitular(UUID titularId, String titularNome, BigDecimal saldoInicial) {
}
