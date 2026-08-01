package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;

public record TransacaoBancariaImportadaResponse(
		UUID id,
		int sequencia,
		LocalDate data,
		BigDecimal valor,
		String tipoBancario,
		String descricao,
		String identificadorBancario,
		String documento,
		boolean duplicadaNoArquivo,
		boolean possivelmenteJaImportada,
		SituacaoTransacaoImportada situacao,
		UUID lancamentoFinanceiroId,
		OffsetDateTime confirmadaEm,
		OffsetDateTime ignoradaEm) {

	public static TransacaoBancariaImportadaResponse from(TransacaoBancariaImportada transacao) {
		return new TransacaoBancariaImportadaResponse(transacao.getId(), transacao.getSequencia(),
				transacao.getDataTransacao(), transacao.getValor(), transacao.getTipoBancario(),
				transacao.getDescricao(), transacao.getIdentificadorBancario(), transacao.getDocumento(),
				transacao.isDuplicadaNoArquivo(), transacao.isPossivelmenteJaImportada(), transacao.getSituacao(),
				transacao.getLancamentoFinanceiro() == null ? null : transacao.getLancamentoFinanceiro().getId(),
				transacao.getConfirmadaEm(), transacao.getIgnoradaEm());
	}

	@Override
	public String toString() {
		return "TransacaoBancariaImportadaResponse[id=" + id + ", sequencia=" + sequencia
				+ ", duplicadaNoArquivo=" + duplicadaNoArquivo
				+ ", possivelmenteJaImportada=" + possivelmenteJaImportada + "]";
	}
}
