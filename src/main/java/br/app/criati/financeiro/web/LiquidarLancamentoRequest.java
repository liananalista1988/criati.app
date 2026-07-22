package br.app.criati.financeiro.web;

import java.time.LocalDate;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import jakarta.validation.constraints.NotNull;

public record LiquidarLancamentoRequest(
		@NotNull(message = "Data de liquidacao e obrigatoria") LocalDate dataLiquidacao,
		FormaPagamentoLancamento formaPagamento) { }
