package br.app.criati.exception;

public class TransacaoImportadaNaoEncontradaException extends RuntimeException {

	public TransacaoImportadaNaoEncontradaException() {
		super("Transacao importada nao encontrada");
	}
}
