package br.app.criati.exception;

public class UltimoAdministradorAtivoException extends RuntimeException {

	public UltimoAdministradorAtivoException() {
		super("Empresa deve manter ao menos um Administrador ativo");
	}
}
