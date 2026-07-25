package br.app.criati.admin.web;

// Resposta minima e propria: nunca inclui a senha, o hash, dados do usuario
// afetado ou o registro de auditoria criado - so a confirmacao de sucesso.
public record RedefinirSenhaGlobalResponse(String mensagem) {

	public static RedefinirSenhaGlobalResponse sucesso() {
		return new RedefinirSenhaGlobalResponse("Senha redefinida com sucesso.");
	}
}
