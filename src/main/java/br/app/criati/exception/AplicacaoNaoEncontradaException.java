package br.app.criati.exception;

public class AplicacaoNaoEncontradaException extends RuntimeException {

	public AplicacaoNaoEncontradaException() {
		super("Aplicacao nao encontrada");
	}
}
