package br.app.criati.exception;

public class AcessoNegadoException extends RuntimeException {

	public AcessoNegadoException() {
		super("Acesso negado");
	}
}
