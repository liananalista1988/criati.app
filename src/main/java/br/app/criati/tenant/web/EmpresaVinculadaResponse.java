package br.app.criati.tenant.web;

import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;

public record EmpresaVinculadaResponse(
		UUID usuarioEmpresaId,
		UUID empresaId,
		String nomeEmpresa,
		String cnpj,
		PerfilUsuario perfil,
		StatusCadastro status) {
}
