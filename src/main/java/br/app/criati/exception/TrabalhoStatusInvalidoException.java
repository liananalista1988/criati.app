package br.app.criati.exception;

/**
 * Transicao invalida de situacao/status sobre ProcessoEmpresarial ou
 * TarefaEmpresarial. Mesmo padrao de mensagem parametrizada de
 * FinanceiroStatusInvalidoException.
 */
public class TrabalhoStatusInvalidoException extends RuntimeException {

	public TrabalhoStatusInvalidoException(String mensagem) {
		super(mensagem);
	}
}
