package br.app.criati.usuario.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CadastrarUsuarioRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		@NotBlank(message = "E-mail e obrigatorio")
		@Email(message = "E-mail deve ser valido") String email,
		@NotBlank(message = "Senha e obrigatoria") String senha) {
}
