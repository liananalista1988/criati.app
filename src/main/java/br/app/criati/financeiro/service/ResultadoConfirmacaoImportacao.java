package br.app.criati.financeiro.service;

import java.util.List;

import br.app.criati.financeiro.model.TransacaoBancariaImportada;

public record ResultadoConfirmacaoImportacao(
		List<TransacaoBancariaImportada> transacoes,
		ResumoImportacaoBancaria resumo) {
}
