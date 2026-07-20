package br.app.criati.tenant.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record SelecionarEmpresaAtivaRequest(
		@NotNull(message = "Empresa e obrigatoria") UUID empresaId) {
}
