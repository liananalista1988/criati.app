package br.app.criati.shared.enums;

/**
 * Como a classificacao final de uma transacao bancaria importada foi
 * decidida. MANUAL tambem cobre o caso em que uma sugestao de regra existia
 * mas o usuario alterou a categoria (ou a fatura escolhida) antes de
 * confirmar - alteracao material invalida a sugestao, entao a regra nao e
 * considerada efetivamente usada (ver ConfirmacaoImportacaoBancariaService).
 */
public enum OrigemClassificacaoTransacaoImportada {
	MANUAL,
	REGRA_SUGERIDA,
	REGRA_AUTOMATICA
}
