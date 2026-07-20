package br.app.criati.exception;

public class CategoriaFinanceiraNomeDuplicadaException extends RuntimeException {

	public CategoriaFinanceiraNomeDuplicadaException() {
		super("Ja existe uma categoria financeira ativa com esse nome e tipo");
	}
}
