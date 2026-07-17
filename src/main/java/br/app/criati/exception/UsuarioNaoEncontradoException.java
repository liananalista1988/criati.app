package br.app.criati.exception;

public class UsuarioNaoEncontradoException extends RuntimeException {

	public UsuarioNaoEncontradoException() {
		super("Usuario nao encontrado");
	}
}
