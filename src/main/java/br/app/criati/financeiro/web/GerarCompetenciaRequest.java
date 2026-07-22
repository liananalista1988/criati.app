package br.app.criati.financeiro.web;

import java.time.YearMonth;

import jakarta.validation.constraints.NotNull;

public record GerarCompetenciaRequest(@NotNull(message = "Competencia e obrigatoria") YearMonth competencia) {
}
