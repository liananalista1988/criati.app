package br.app.criati.exception;

public class PagamentoOcorrenciaNaoEncontradoException extends RuntimeException {

	public PagamentoOcorrenciaNaoEncontradoException() {
		super("Pagamento de ocorrencia nao encontrado");
	}
}
