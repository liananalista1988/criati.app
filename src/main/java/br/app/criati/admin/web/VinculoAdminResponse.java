package br.app.criati.admin.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;

public record VinculoAdminResponse(
		UUID usuarioEmpresaId,
		UUID usuarioId,
		String usuarioNome,
		String usuarioEmail,
		UUID empresaId,
		String empresaNome,
		PerfilUsuario perfil,
		StatusCadastro status,
		OffsetDateTime criadoEm) {
}
