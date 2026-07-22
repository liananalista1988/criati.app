package br.app.criati.exception;

public class UsuarioJaVinculadoPessoaFinanceiraException extends RuntimeException {

	public UsuarioJaVinculadoPessoaFinanceiraException() {
		super("Usuario ja esta vinculado a outra pessoa ativa nesta empresa");
	}
}
