package br.app.criati.exception;

/**
 * Repeticao de transicao de status (inativar/reativar) sobre ContaFinanceira
 * ou CategoriaFinanceira ja no estado alvo. Mesmo padrao de mensagem
 * parametrizada de VinculoStatusInvalidoException/LancamentoStatusInvalidoException.
 */
public class FinanceiroStatusInvalidoException extends RuntimeException {

	public FinanceiroStatusInvalidoException(String mensagem) {
		super(mensagem);
	}
}
