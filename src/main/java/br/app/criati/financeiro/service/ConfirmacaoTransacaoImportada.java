package br.app.criati.financeiro.service;

import java.util.UUID;

/**
 * Comando de confirmacao de uma transacao bancaria importada. Exatamente um
 * entre categoriaId e faturaId deve ser informado (validado em
 * ConfirmacaoImportacaoBancariaService): categoriaId gera um LancamentoFinanceiro
 * comum; faturaId encaminha para PagamentoFaturaCartaoService.registrarPagamento,
 * nunca cria um LancamentoFinanceiro generico duplicando a despesa (CRIATI-IMP-002A).
 * regraClassificacaoId e opcional: id da regra que sugeriu/pre-preencheu esta
 * linha, se o usuario aceitou uma sugestao - usado so para decidir a origem da
 * classificacao e o contador de uso da regra, nunca para pular a confirmacao
 * explicita do usuario.
 */
public record ConfirmacaoTransacaoImportada(
		UUID transacaoId,
		UUID categoriaId,
		UUID faturaId,
		UUID regraClassificacaoId,
		String descricaoFinal,
		boolean confirmarDuplicidade) {
}
