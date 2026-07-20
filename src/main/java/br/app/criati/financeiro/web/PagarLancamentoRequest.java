package br.app.criati.financeiro.web;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record PagarLancamentoRequest(@NotNull(message = "Data de pagamento e obrigatoria") LocalDate dataPagamento) {
}
