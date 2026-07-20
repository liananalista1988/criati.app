package br.app.criati.exception;

public class LancamentoFinanceiroNaoEncontradoException extends RuntimeException {

	public LancamentoFinanceiroNaoEncontradoException() {
		super("Lancamento financeiro nao encontrado");
	}
}
