package br.app.criati.exception;

public class AutoAlteracaoNaoPermitidaException extends RuntimeException {

	public AutoAlteracaoNaoPermitidaException() {
		super("Operacao nao pode ser realizada sobre o proprio vinculo");
	}
}
