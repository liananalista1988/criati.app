package br.app.criati.financeiro.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;

public record RegraClassificacaoImportacaoResponse(
		UUID id,
		UUID contaId,
		String contaNome,
		UUID categoriaId,
		String categoriaNome,
		UUID pessoaFinanceiraId,
		String pessoaFinanceiraNome,
		String descricaoReferencia,
		String padraoNormalizado,
		EstrategiaComparacaoRegraImportacao estrategiaComparacao,
		int prioridade,
		TipoFinanceiro tipo,
		FormaPagamentoLancamento formaPagamento,
		EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
		NivelConfiancaRegraImportacao nivelConfianca,
		AplicacaoRegraClassificacaoImportacao aplicacao,
		StatusCadastro status,
		int quantidadeUtilizacoes,
		OffsetDateTime ultimaUtilizacaoEm,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static RegraClassificacaoImportacaoResponse from(RegraClassificacaoImportacao regra) {
		return new RegraClassificacaoImportacaoResponse(
				regra.getId(),
				regra.getConta() == null ? null : regra.getConta().getId(),
				regra.getConta() == null ? null : regra.getConta().getNome(),
				regra.getCategoria().getId(),
				regra.getCategoria().getNome(),
				regra.getPessoaFinanceira() == null ? null : regra.getPessoaFinanceira().getId(),
				regra.getPessoaFinanceira() == null ? null : regra.getPessoaFinanceira().getNome(),
				regra.getDescricaoReferencia(),
				regra.getPadraoNormalizado(),
				regra.getEstrategiaComparacao(),
				regra.getPrioridade(),
				regra.getTipo(),
				regra.getFormaPagamento(),
				regra.getEncaminhamentoSugerido(),
				regra.getNivelConfianca(),
				regra.getAplicacao(),
				regra.getStatus(),
				regra.getQuantidadeUtilizacoes(),
				regra.getUltimaUtilizacaoEm(),
				regra.getCriadoEm(),
				regra.getAtualizadoEm());
	}
}
