package br.app.criati.exception;

public class InstituicaoFinanceiraNaoEncontradaException extends RuntimeException {

	public InstituicaoFinanceiraNaoEncontradaException() {
		super("Instituicao financeira nao encontrada");
	}
}
