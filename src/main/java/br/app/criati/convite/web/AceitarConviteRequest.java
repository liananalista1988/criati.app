package br.app.criati.convite.web;

import jakarta.validation.constraints.NotBlank;

public record AceitarConviteRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		@NotBlank(message = "Senha e obrigatoria") String senha,
		@NotBlank(message = "Confirmacao de senha e obrigatoria") String confirmacaoSenha) {
}
