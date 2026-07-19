package br.app.criati.acesso.web;

import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import jakarta.validation.constraints.NotNull;

public record VincularUsuarioEmpresaRequest(
		@NotNull(message = "Usuario e obrigatorio") UUID usuarioId,
		@NotNull(message = "Empresa e obrigatoria") UUID empresaId,
		@NotNull(message = "Perfil e obrigatorio") PerfilUsuario perfil) {
}
