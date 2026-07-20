package br.app.criati.shared.enums;

/**
 * Natureza financeira compartilhada por CategoriaFinanceira e
 * LancamentoFinanceiro (RECEITA ou DESPESA). Um unico enum, em vez de dois
 * identicos ("TipoCategoriaFinanceira"/"TipoLancamentoFinanceiro"), porque o
 * tipo da categoria e o tipo do lancamento sao literalmente o mesmo conceito
 * e precisam ser comparados por igualdade na regra de compatibilidade
 * (LancamentoFinanceiroService); manter dois enums so duplicaria os mesmos
 * dois valores. Ver docs/DECISOES.md, secao "Financeiro".
 */
public enum TipoFinanceiro {
	RECEITA,
	DESPESA
}
