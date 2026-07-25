package br.app.criati.exception;

public class AutoRedefinicaoSenhaNaoPermitidaException extends RuntimeException {

	public AutoRedefinicaoSenhaNaoPermitidaException() {
		super("Nao e possivel redefinir a propria senha por este fluxo");
	}
}
