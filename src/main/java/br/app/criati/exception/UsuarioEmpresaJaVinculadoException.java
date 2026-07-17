package br.app.criati.exception;

public class UsuarioEmpresaJaVinculadoException extends RuntimeException {

	public UsuarioEmpresaJaVinculadoException() {
		super("Usuario ja vinculado a empresa");
	}
}
