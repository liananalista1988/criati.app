package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;

public record ContaFinanceiraResponse(
		UUID id,
		String nome,
		TipoContaFinanceira tipo,
		BigDecimal saldoInicial,
		BigDecimal saldoAtual,
		StatusCadastro status) {

	public static ContaFinanceiraResponse from(ContaFinanceira conta, BigDecimal saldoAtual) {
		return new ContaFinanceiraResponse(
				conta.getId(), conta.getNome(), conta.getTipo(), conta.getSaldoInicial(), saldoAtual, conta.getStatus());
	}
}
