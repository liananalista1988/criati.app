package br.app.criati.financeiro.service;

public record ResumoImportacaoBancaria(long pendentes, long confirmadas, long ignoradas, long duplicadas) {
}
