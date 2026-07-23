package br.app.criati.exception;

public class EmprestimoConcedidoNaoEncontradoException extends RuntimeException {

	public EmprestimoConcedidoNaoEncontradoException() {
		super("Emprestimo concedido nao encontrado");
	}
}
