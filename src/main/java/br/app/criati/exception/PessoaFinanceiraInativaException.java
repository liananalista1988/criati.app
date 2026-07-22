package br.app.criati.exception;

public class PessoaFinanceiraInativaException extends RuntimeException {

	public PessoaFinanceiraInativaException() {
		super("A pessoa titular deve estar ativa");
	}
}
