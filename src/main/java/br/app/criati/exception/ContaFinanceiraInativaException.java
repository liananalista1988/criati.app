package br.app.criati.exception;

public class ContaFinanceiraInativaException extends RuntimeException {

	public ContaFinanceiraInativaException() {
		super("Conta financeira inativa");
	}
}
