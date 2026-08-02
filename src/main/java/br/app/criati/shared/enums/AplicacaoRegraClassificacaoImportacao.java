package br.app.criati.shared.enums;

/**
 * AUTOMATICA significa somente pre-preenchimento da linha de revisao da
 * importacao - nunca confirma, cria lancamento, liquida fatura ou movimenta
 * dinheiro sozinha. A confirmacao final e sempre uma acao explicita do
 * usuario (ConfirmacaoImportacaoBancariaService.confirmar), com ou sem
 * regra envolvida.
 */
public enum AplicacaoRegraClassificacaoImportacao {
	AUTOMATICA,
	SUGESTAO
}
