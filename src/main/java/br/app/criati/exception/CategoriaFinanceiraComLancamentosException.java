package br.app.criati.exception;

public class CategoriaFinanceiraComLancamentosException extends RuntimeException {

	public CategoriaFinanceiraComLancamentosException() {
		super("Categoria financeira possui lancamentos e nao pode ser editada");
	}
}
