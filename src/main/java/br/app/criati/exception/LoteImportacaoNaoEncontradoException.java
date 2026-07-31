package br.app.criati.exception;

public class LoteImportacaoNaoEncontradoException extends RuntimeException {

	public LoteImportacaoNaoEncontradoException() {
		super("Lote de importacao nao encontrado");
	}
}
