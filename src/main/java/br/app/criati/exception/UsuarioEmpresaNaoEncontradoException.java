package br.app.criati.exception;

public class UsuarioEmpresaNaoEncontradoException extends RuntimeException {

	public UsuarioEmpresaNaoEncontradoException() {
		super("Vinculo de usuario e empresa nao encontrado");
	}
}
