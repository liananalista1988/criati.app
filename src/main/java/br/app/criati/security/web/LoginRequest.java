package br.app.criati.security.web;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "E-mail e obrigatorio") String email,
		@NotBlank(message = "Senha e obrigatoria") String senha) {
}
