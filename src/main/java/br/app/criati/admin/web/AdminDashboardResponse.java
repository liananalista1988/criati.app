package br.app.criati.admin.web;

import java.util.List;

import br.app.criati.empresa.web.EmpresaResponse;

public record AdminDashboardResponse(
		long totalEmpresas,
		long empresasAtivas,
		long empresasInativas,
		long totalUsuarios,
		long usuariosAtivos,
		long totalVinculosAtivos,
		long convitesPendentes,
		long empresasComFinanceiroHabilitado,
		List<EmpresaResponse> empresasRecentes) {
}
