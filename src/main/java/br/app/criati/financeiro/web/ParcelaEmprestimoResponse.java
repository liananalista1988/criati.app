package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.shared.enums.SituacaoParcelaEmprestimo;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;

public record ParcelaEmprestimoResponse(UUID id, UUID emprestimoId, int numero, int totalParcelas,
		BigDecimal valorPrincipal, LocalDate vencimento, LocalDate dataPrometida, LocalDate dataEfetivaPagamento,
		BigDecimal juros, BigDecimal multa, BigDecimal valorTotal, BigDecimal valorRecebido, BigDecimal saldoPendente,
		StatusParcelaEmprestimo status, SituacaoParcelaEmprestimo situacao, boolean atrasada, long diasEmAtraso,
		OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {

	public static ParcelaEmprestimoResponse from(ParcelaEmprestimo p) {
		LocalDate hoje = LocalDate.now();
		return new ParcelaEmprestimoResponse(p.getId(), p.getEmprestimo().getId(), p.getNumero(), p.getTotalParcelas(),
				p.getValorPrincipal(), p.getVencimento(), p.getDataPrometida(), p.getDataEfetivaPagamento(),
				p.getJuros(), p.getMulta(), p.getValorTotal(), p.getValorRecebido(), p.getSaldoPendente(),
				p.getStatus(), p.getSituacao(hoje), p.estaAtrasada(hoje), p.diasEmAtraso(hoje), p.getCriadoEm(),
				p.getAtualizadoEm());
	}
}
