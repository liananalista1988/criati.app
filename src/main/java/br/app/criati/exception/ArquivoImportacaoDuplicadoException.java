package br.app.criati.exception;

public class ArquivoImportacaoDuplicadoException extends RuntimeException {

	public ArquivoImportacaoDuplicadoException() {
		super("Este arquivo ja foi importado para a empresa atual");
	}
}
