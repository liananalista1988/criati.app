package br.app.criati.exception;

public class RecorrenciaFinanceiraNaoEncontradaException extends RuntimeException {

	public RecorrenciaFinanceiraNaoEncontradaException() {
		super("Recorrencia financeira nao encontrada");
	}
}
