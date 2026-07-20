package br.app.criati.convite.web;

import br.app.criati.shared.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarConviteRequest(
		@NotBlank(message = "E-mail e obrigatorio")
		@Email(message = "E-mail deve ser valido") String email,
		@NotNull(message = "Perfil e obrigatorio") PerfilUsuario perfil) {
}
