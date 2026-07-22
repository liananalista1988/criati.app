package br.app.criati.exception;

public class ParteFinanceiraNaoEncontradaException extends RuntimeException {

	public ParteFinanceiraNaoEncontradaException() {
		super("Contato financeiro nao encontrado");
	}
}
