package br.app.criati.tenant;

import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;

public record ContextoEmpresaAtual(
		UUID usuarioId,
		UUID empresaId,
		UUID usuarioEmpresaId,
		PerfilUsuario perfil) {
}
