package br.app.criati.acesso.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;

public record IntegranteEmpresaResponse(
		UUID usuarioEmpresaId,
		UUID usuarioId,
		String nome,
		String email,
		PerfilUsuario perfil,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm,
		boolean proprioUsuario) {
}
