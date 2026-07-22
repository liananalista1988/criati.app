package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

import br.app.criati.financeiro.model.OcorrenciaCompromisso;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;

public record OcorrenciaCompromissoResponse(UUID id, UUID compromissoId, String compromissoDescricao,
		UUID recorrenciaId, YearMonth competencia, String descricao, UUID categoriaId, String categoriaNome,
		UUID pessoaFinanceiraId, String pessoaFinanceiraNome, UUID parteFinanceiraId, String parteFinanceiraNome,
		UUID contaPrevistaId, String contaPrevistaNome, BigDecimal valorPrevisto, BigDecimal valorPrincipal,
		LocalDate vencimento, LocalDate dataRecebimentoCobranca, StatusOcorrenciaCompromisso status, boolean vencida,
		long diasEmAtraso, BigDecimal juros, BigDecimal multa, BigDecimal desconto, BigDecimal valorTotal,
		BigDecimal valorPago, BigDecimal saldoPendente, String observacao, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static OcorrenciaCompromissoResponse from(OcorrenciaCompromisso o) {
		LocalDate hoje = LocalDate.now();
		return new OcorrenciaCompromissoResponse(o.getId(),
				o.getCompromisso() == null ? null : o.getCompromisso().getId(),
				o.getCompromisso() == null ? null : o.getCompromisso().getDescricao(),
				o.getRecorrencia() == null ? null : o.getRecorrencia().getId(), o.getCompetencia(), o.getDescricao(),
				o.getCategoria().getId(), o.getCategoria().getNome(), o.getPessoaFinanceira().getId(),
				o.getPessoaFinanceira().getNome(),
				o.getParteFinanceira() == null ? null : o.getParteFinanceira().getId(),
				o.getParteFinanceira() == null ? null : o.getParteFinanceira().getNome(),
				o.getContaPrevista() == null ? null : o.getContaPrevista().getId(),
				o.getContaPrevista() == null ? null : o.getContaPrevista().getNome(), o.getValorPrevisto(),
				o.getValorPrincipal(), o.getVencimento(), o.getDataRecebimentoCobranca(), o.getStatus(),
				o.estaVencida(hoje), o.diasEmAtraso(hoje), o.getJuros(), o.getMulta(), o.getDesconto(),
				o.getValorTotal(), o.getValorPago(), o.getSaldoPendente(), o.getObservacao(), o.getCriadoEm(),
				o.getAtualizadoEm(), o.getCriadoPor() == null ? null : o.getCriadoPor().getId(),
				o.getAtualizadoPor() == null ? null : o.getAtualizadoPor().getId());
	}
}
