package br.app.criati.acesso.web;

import br.app.criati.shared.enums.PerfilUsuario;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(
		@NotNull(message = "Perfil e obrigatorio") PerfilUsuario perfil) {
}
