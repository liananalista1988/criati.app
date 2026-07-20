package br.app.criati.exception;

// Mensagem generica de proposito: cobre convite inexistente, expirado,
// utilizado, revogado ou de empresa inativa, sem distinguir o motivo.
public class ConviteInvalidoException extends RuntimeException {

	public ConviteInvalidoException() {
		super("Convite invalido ou expirado");
	}
}
