package br.app.criati.exception;

public class CategoriaFinanceiraNaoEncontradaException extends RuntimeException {

	public CategoriaFinanceiraNaoEncontradaException() {
		super("Categoria financeira nao encontrada");
	}
}
