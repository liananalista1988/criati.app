package br.app.criati.financeiro.service;

import java.util.UUID;

/**
 * Comando de confirmacao de uma transacao bancaria importada. Exatamente um
 * entre categoriaId, faturaId e parcelaEmprestimoId deve ser informado
 * (validado em ConfirmacaoImportacaoBancariaService): categoriaId gera um
 * LancamentoFinanceiro comum; faturaId encaminha para
 * PagamentoFaturaCartaoService.registrarPagamento; parcelaEmprestimoId
 * encaminha para RecebimentoParcelaEmprestimoService.receberParcial (vinculo
 * persistente e auditavel com o emprestimo/parcela, via o mesmo
 * LancamentoFinanceiro referenciado pelos dois lados - CRIATI-FIN-FEAT-015).
 * Nenhum dos tres cria um LancamentoFinanceiro generico duplicando a despesa
 * ou o recebimento ja tratado pelo fluxo especifico (CRIATI-IMP-002A).
 * A associacao a uma parcela e sempre escolhida explicitamente pelo usuario -
 * nenhuma correspondencia automatica por descricao/valor e feita aqui.
 * regraClassificacaoId e opcional: id da regra que sugeriu/pre-preencheu esta
 * linha, se o usuario aceitou uma sugestao - usado so para decidir a origem da
 * classificacao e o contador de uso da regra, nunca para pular a confirmacao
 * explicita do usuario.
 */
public record ConfirmacaoTransacaoImportada(
		UUID transacaoId,
		UUID categoriaId,
		UUID faturaId,
		UUID parcelaEmprestimoId,
		UUID regraClassificacaoId,
		String descricaoFinal,
		boolean confirmarDuplicidade) {
}
