package br.app.criati.exception;

public class FaturaCartaoNaoEncontradaException extends RuntimeException {

	public FaturaCartaoNaoEncontradaException() {
		super("Fatura nao encontrada");
	}
}
