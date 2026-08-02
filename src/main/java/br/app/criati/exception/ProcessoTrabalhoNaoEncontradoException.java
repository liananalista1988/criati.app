package br.app.criati.exception;

public class ProcessoTrabalhoNaoEncontradoException extends RuntimeException {

	public ProcessoTrabalhoNaoEncontradoException() {
		super("Processo nao encontrado");
	}
}
