package br.app.criati.acesso.web;

import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;

public record UsuarioEmpresaResponse(
		UUID id,
		UUID usuarioId,
		UUID empresaId,
		PerfilUsuario perfil,
		StatusCadastro status) {
}
