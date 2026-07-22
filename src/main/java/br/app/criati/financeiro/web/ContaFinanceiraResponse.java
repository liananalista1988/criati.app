package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;

public record ContaFinanceiraResponse(
		UUID id,
		String nome,
		UUID titularId,
		String titularNome,
		UUID instituicaoId,
		String instituicaoNome,
		TipoContaFinanceira tipo,
		String moeda,
		BigDecimal saldoInicial,
		LocalDate dataSaldoInicial,
		boolean permiteConciliacao,
		BigDecimal saldoAtual,
		StatusCadastro status,
		boolean possivelDuplicidade,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static ContaFinanceiraResponse from(
			ContaFinanceira conta, BigDecimal saldoAtual, boolean possivelDuplicidade) {
		return new ContaFinanceiraResponse(
				conta.getId(),
				conta.getNome(),
				conta.getTitular() == null ? null : conta.getTitular().getId(),
				conta.getTitular() == null ? null : conta.getTitular().getNome(),
				conta.getInstituicao() == null ? null : conta.getInstituicao().getId(),
				conta.getInstituicao() == null ? null : conta.getInstituicao().getNome(),
				conta.getTipo(),
				conta.getMoeda(),
				conta.getSaldoInicial(),
				conta.getDataSaldoInicial(),
				conta.isPermiteConciliacao(),
				saldoAtual,
				conta.getStatus(),
				possivelDuplicidade,
				conta.getCriadoEm(),
				conta.getAtualizadoEm());
	}
}
