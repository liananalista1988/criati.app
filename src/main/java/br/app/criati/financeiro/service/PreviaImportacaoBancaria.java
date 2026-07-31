package br.app.criati.financeiro.service;

import java.util.List;

import br.app.criati.financeiro.model.LoteImportacaoBancaria;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;

public record PreviaImportacaoBancaria(
		LoteImportacaoBancaria lote,
		List<TransacaoBancariaImportada> transacoes) {
}
