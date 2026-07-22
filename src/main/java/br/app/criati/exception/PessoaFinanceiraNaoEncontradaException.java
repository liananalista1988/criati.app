package br.app.criati.exception;

public class PessoaFinanceiraNaoEncontradaException extends RuntimeException {

	public PessoaFinanceiraNaoEncontradaException() {
		super("Pessoa financeira nao encontrada");
	}
}
