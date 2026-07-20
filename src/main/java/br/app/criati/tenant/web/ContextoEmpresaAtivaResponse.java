package br.app.criati.tenant.web;

import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.tenant.ContextoEmpresaAtual;

public record ContextoEmpresaAtivaResponse(
		UUID empresaId,
		UUID usuarioEmpresaId,
		PerfilUsuario perfil) {

	public static ContextoEmpresaAtivaResponse from(ContextoEmpresaAtual contexto) {
		return new ContextoEmpresaAtivaResponse(contexto.empresaId(), contexto.usuarioEmpresaId(), contexto.perfil());
	}
}
