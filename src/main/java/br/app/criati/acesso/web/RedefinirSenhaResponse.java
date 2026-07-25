package br.app.criati.acesso.web;

// Resposta minima e propria: nunca inclui a senha, o hash, dados do usuario
// afetado ou o registro de auditoria criado - so a confirmacao de sucesso.
public record RedefinirSenhaResponse(String mensagem) {

	public static RedefinirSenhaResponse sucesso() {
		return new RedefinirSenhaResponse("Senha redefinida com sucesso.");
	}
}
