package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.OrigemClassificacaoTransacaoImportada;
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
		OffsetDateTime ignoradaEm,
		UUID regraClassificacaoId,
		OrigemClassificacaoTransacaoImportada origemClassificacao,
		// Sugestao (nunca aplicada sozinha - so preenchida para transacao ainda
		// PENDENTE; ver RegraClassificacaoImportacaoService.sugerirParaTransacao):
		UUID regraSugeridaId,
		UUID categoriaSugeridaId,
		String categoriaSugeridaNome,
		UUID pessoaSugeridaId,
		String pessoaSugeridaNome,
		EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
		AplicacaoRegraClassificacaoImportacao aplicacaoSugestao) {

	public static TransacaoBancariaImportadaResponse from(
			TransacaoBancariaImportada transacao, RegraClassificacaoImportacao sugestao) {
		return new TransacaoBancariaImportadaResponse(transacao.getId(), transacao.getSequencia(),
				transacao.getDataTransacao(), transacao.getValor(), transacao.getTipoBancario(),
				transacao.getDescricao(), transacao.getIdentificadorBancario(), transacao.getDocumento(),
				transacao.isDuplicadaNoArquivo(), transacao.isPossivelmenteJaImportada(), transacao.getSituacao(),
				transacao.getLancamentoFinanceiro() == null ? null : transacao.getLancamentoFinanceiro().getId(),
				transacao.getConfirmadaEm(), transacao.getIgnoradaEm(),
				transacao.getRegraClassificacao() == null ? null : transacao.getRegraClassificacao().getId(),
				transacao.getOrigemClassificacao(),
				sugestao == null ? null : sugestao.getId(),
				sugestao == null ? null : sugestao.getCategoria().getId(),
				sugestao == null ? null : sugestao.getCategoria().getNome(),
				sugestao == null || sugestao.getPessoaFinanceira() == null ? null : sugestao.getPessoaFinanceira().getId(),
				sugestao == null || sugestao.getPessoaFinanceira() == null ? null : sugestao.getPessoaFinanceira().getNome(),
				sugestao == null ? null : sugestao.getEncaminhamentoSugerido(),
				sugestao == null ? null : sugestao.getAplicacao());
	}

	public static TransacaoBancariaImportadaResponse from(TransacaoBancariaImportada transacao) {
		return from(transacao, null);
	}

	@Override
	public String toString() {
		return "TransacaoBancariaImportadaResponse[id=" + id + ", sequencia=" + sequencia
				+ ", duplicadaNoArquivo=" + duplicadaNoArquivo
				+ ", possivelmenteJaImportada=" + possivelmenteJaImportada + "]";
	}
}
