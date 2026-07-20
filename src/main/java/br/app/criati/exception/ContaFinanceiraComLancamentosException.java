package br.app.criati.exception;

public class ContaFinanceiraComLancamentosException extends RuntimeException {

	public ContaFinanceiraComLancamentosException() {
		super("Conta financeira possui lancamentos e nao pode ser editada");
	}
}
