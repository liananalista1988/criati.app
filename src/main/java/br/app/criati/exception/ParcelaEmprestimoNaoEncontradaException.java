package br.app.criati.exception;

public class ParcelaEmprestimoNaoEncontradaException extends RuntimeException {

	public ParcelaEmprestimoNaoEncontradaException() {
		super("Parcela de emprestimo nao encontrada");
	}
}
