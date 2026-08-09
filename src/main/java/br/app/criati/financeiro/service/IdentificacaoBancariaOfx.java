package br.app.criati.financeiro.service;

/**
 * Metadado de identificacao bancaria do extrato OFX (BANKID/BRANCHID/ACCTID/
 * ACCTTYPE, lidos de BANKACCTFROM ou CCACCTFROM), usado apenas para sugerir
 * automaticamente a conta financeira do lote inteiro - nunca repetido por
 * transacao, porque um extrato pertence a uma unica conta de origem
 * (CRIATI-IMP-FEAT-004). Qualquer campo pode ser nulo quando o banco emissor
 * nao inclui a respectiva tag.
 */
public record IdentificacaoBancariaOfx(String bankId, String branchId, String acctId, String acctType) {

	// Autodetecao exige banco + agencia + numero da conta completos - dado
	// bancario incompleto nunca escolhe conta automaticamente (item 6).
	public boolean completaParaAutodetecao() {
		return naoBranco(bankId) && naoBranco(branchId) && naoBranco(acctId);
	}

	private static boolean naoBranco(String valor) {
		return valor != null && !valor.isBlank();
	}
}
