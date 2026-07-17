package br.app.criati.exception;

public class EmailJaCadastradoException extends RuntimeException {

	public EmailJaCadastradoException() {
		super("E-mail ja cadastrado");
	}
}
