package br.app.criati.admin.web;

import java.util.List;

import br.app.criati.acesso.web.UsuarioEmpresaResponse;
import br.app.criati.empresa.web.EmpresaResponse;
import br.app.criati.usuario.web.UsuarioResponse;

public record EmpresaComAdministradorExistenteResponse(
		EmpresaResponse empresa,
		List<String> aplicacoesHabilitadas,
		UsuarioResponse administrador,
		UsuarioEmpresaResponse vinculo) {
}
