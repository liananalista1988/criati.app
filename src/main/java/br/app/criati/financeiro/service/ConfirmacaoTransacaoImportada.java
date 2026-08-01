package br.app.criati.financeiro.service;

import java.util.UUID;

public record ConfirmacaoTransacaoImportada(
		UUID transacaoId,
		UUID categoriaId,
		String descricaoFinal,
		boolean confirmarDuplicidade) {
}
