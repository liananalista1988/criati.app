package br.app.criati.financeiro.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ResolverContaImportacaoRequest(@NotNull(message = "Conta e obrigatoria") UUID contaId) {
}
