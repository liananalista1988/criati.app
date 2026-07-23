package br.app.criati.exception;

public class RecebimentoParcelaEmprestimoNaoEncontradoException extends RuntimeException {

	public RecebimentoParcelaEmprestimoNaoEncontradoException() {
		super("Recebimento de parcela de emprestimo nao encontrado");
	}
}
