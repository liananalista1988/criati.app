package br.app.criati.exception;

public class CompromissoFinanceiroNaoEncontradoException extends RuntimeException {

	public CompromissoFinanceiroNaoEncontradoException() {
		super("Compromisso financeiro nao encontrado");
	}
}
