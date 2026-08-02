package br.app.criati.financeiro.service;

import java.util.UUID;

import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.TipoFinanceiro;

/**
 * Comando de entrada para criar/editar uma RegraClassificacaoImportacao.
 * padrao pode vir vazio - nesse caso o service deriva o padrao normalizado
 * diretamente de descricaoReferencia (caso de uso mais comum: usuario aponta
 * a transacao de origem e o sistema propoe o padrao).
 */
public record DadosRegraClassificacaoImportacao(
		UUID contaId,
		UUID categoriaId,
		UUID pessoaFinanceiraId,
		String descricaoReferencia,
		String padrao,
		EstrategiaComparacaoRegraImportacao estrategiaComparacao,
		int prioridade,
		TipoFinanceiro tipo,
		FormaPagamentoLancamento formaPagamento,
		EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
		NivelConfiancaRegraImportacao nivelConfianca,
		AplicacaoRegraClassificacaoImportacao aplicacao) {
}
