package br.app.criati.exception;

public class CategoriaFinanceiraInativaException extends RuntimeException {

	public CategoriaFinanceiraInativaException() {
		super("Categoria financeira inativa");
	}
}
