package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.UUID;

import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoFinanceiro;

public record RecorrenciaFinanceiraResponse(UUID id, TipoFinanceiro tipo, String descricao, BigDecimal valorPadrao,
		UUID contaId, String contaNome, UUID categoriaId, String categoriaNome, UUID pessoaFinanceiraId,
		String pessoaFinanceiraNome, UUID parteFinanceiraId, String parteFinanceiraNome,
		FormaPagamentoLancamento formaPagamento, PeriodicidadeRecorrencia periodicidade, int intervalo,
		int diaReferencia, Integer mesReferencia, LocalDate dataInicial, LocalDate dataFinal,
		YearMonth proximaCompetencia, StatusRecorrencia status, boolean gerarAutomaticamente, String observacao,
		long quantidadeOcorrencias, LocalDate ultimaCompetenciaGerada, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId,
		OffsetDateTime pausadaEm, OffsetDateTime encerradaEm) {

	public static RecorrenciaFinanceiraResponse from(RecorrenciaFinanceira r, long quantidadeOcorrencias,
			LocalDate ultimaCompetenciaGerada) {
		return new RecorrenciaFinanceiraResponse(r.getId(), r.getTipo(), r.getDescricao(), r.getValorPadrao(),
				r.getConta().getId(), r.getConta().getNome(), r.getCategoria().getId(), r.getCategoria().getNome(),
				r.getPessoaFinanceira().getId(), r.getPessoaFinanceira().getNome(),
				r.getParteFinanceira() == null ? null : r.getParteFinanceira().getId(),
				r.getParteFinanceira() == null ? null : r.getParteFinanceira().getNome(),
				r.getFormaPagamento(), r.getPeriodicidade(), r.getIntervalo(), r.getDiaReferencia(),
				r.getMesReferencia(), r.getDataInicial(), r.getDataFinal(), r.getProximaCompetencia(), r.getStatus(),
				r.isGerarAutomaticamente(), r.getObservacao(), quantidadeOcorrencias, ultimaCompetenciaGerada,
				r.getCriadoEm(), r.getAtualizadoEm(), r.getCriadoPor() == null ? null : r.getCriadoPor().getId(),
				r.getAtualizadoPor() == null ? null : r.getAtualizadoPor().getId(), r.getPausadaEm(), r.getEncerradaEm());
	}
}
