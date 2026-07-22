package br.app.criati.exception;

public class CartaoCreditoNaoEncontradoException extends RuntimeException {

	public CartaoCreditoNaoEncontradoException() {
		super("Cartao de credito nao encontrado");
	}
}
