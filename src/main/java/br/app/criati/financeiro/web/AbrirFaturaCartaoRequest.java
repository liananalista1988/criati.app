package br.app.criati.financeiro.web;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AbrirFaturaCartaoRequest(
		@NotNull(message = "Cartao e obrigatorio") UUID cartaoId,
		@NotNull(message = "Competencia e obrigatoria") LocalDate competencia) {
}
