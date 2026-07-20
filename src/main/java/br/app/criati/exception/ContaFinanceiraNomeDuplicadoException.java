package br.app.criati.exception;

public class ContaFinanceiraNomeDuplicadoException extends RuntimeException {

	public ContaFinanceiraNomeDuplicadoException() {
		super("Ja existe uma conta financeira ativa com esse nome");
	}
}
