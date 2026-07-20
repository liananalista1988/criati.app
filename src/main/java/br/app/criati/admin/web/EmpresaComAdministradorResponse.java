package br.app.criati.admin.web;

import br.app.criati.acesso.web.UsuarioEmpresaResponse;
import br.app.criati.empresa.web.EmpresaResponse;
import br.app.criati.usuario.web.UsuarioResponse;

public record EmpresaComAdministradorResponse(
		EmpresaResponse empresa,
		UsuarioResponse administrador,
		UsuarioEmpresaResponse vinculo) {
}
