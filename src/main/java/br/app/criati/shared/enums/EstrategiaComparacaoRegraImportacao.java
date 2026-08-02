package br.app.criati.shared.enums;

/**
 * Como o padrao normalizado de uma RegraClassificacaoImportacao e comparado
 * contra a descricao normalizada de uma transacao bancaria importada.
 * CONTEM nunca pode ser combinada com aplicacao AUTOMATICA (ver
 * RegraClassificacaoImportacaoService) - decisao de negocio explicita, nao
 * apenas uma questao de tamanho de texto.
 */
public enum EstrategiaComparacaoRegraImportacao {
	IGUAL,
	CONTEM,
	PREFIXO
}
