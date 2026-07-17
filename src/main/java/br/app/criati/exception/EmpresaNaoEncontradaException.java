package br.app.criati.exception;

public class EmpresaNaoEncontradaException extends RuntimeException {

	public EmpresaNaoEncontradaException() {
		super("Empresa nao encontrada");
	}
}
