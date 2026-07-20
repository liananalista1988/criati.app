package br.app.criati.exception;

public class ContaFinanceiraNaoEncontradaException extends RuntimeException {

	public ContaFinanceiraNaoEncontradaException() {
		super("Conta financeira nao encontrada");
	}
}
