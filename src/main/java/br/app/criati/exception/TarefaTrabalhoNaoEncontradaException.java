package br.app.criati.exception;

public class TarefaTrabalhoNaoEncontradaException extends RuntimeException {

	public TarefaTrabalhoNaoEncontradaException() {
		super("Tarefa nao encontrada");
	}
}
