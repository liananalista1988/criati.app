package br.app.criati.financeiro.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record ConfirmarTransacoesImportadasRequest(
		@NotEmpty List<@Valid ConfirmacaoTransacaoImportadaRequest> transacoes) {
}
