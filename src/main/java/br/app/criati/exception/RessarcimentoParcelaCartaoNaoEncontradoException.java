package br.app.criati.exception;

public class RessarcimentoParcelaCartaoNaoEncontradoException extends RuntimeException {

	public RessarcimentoParcelaCartaoNaoEncontradoException() {
		super("Ressarcimento de compra para terceiro nao encontrado");
	}
}
