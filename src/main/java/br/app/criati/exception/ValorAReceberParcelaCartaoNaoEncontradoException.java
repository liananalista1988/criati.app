package br.app.criati.exception;

public class ValorAReceberParcelaCartaoNaoEncontradoException extends RuntimeException {

	public ValorAReceberParcelaCartaoNaoEncontradoException() {
		super("Valor a receber de compra para terceiro nao encontrado");
	}
}
