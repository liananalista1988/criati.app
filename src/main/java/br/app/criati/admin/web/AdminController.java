package br.app.criati.admin.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.admin.AdminDashboardService;
import br.app.criati.admin.AdminDashboardService.AdminDashboardDados;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.web.EmpresaResponse;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.usuario.web.UsuarioResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminDashboardService adminDashboardService;

	public AdminController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping("/me")
	public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.ok(new UsuarioResponse(
				principal.getUsuario().getId(),
				principal.getUsuario().getNome(),
				principal.getUsuario().getEmail(),
				principal.getUsuario().getStatus()));
	}

	@GetMapping("/dashboard")
	public ResponseEntity<AdminDashboardResponse> dashboard() {
		AdminDashboardDados dados = adminDashboardService.calcular();
		AdminDashboardResponse response = new AdminDashboardResponse(
				dados.totalEmpresas(),
				dados.empresasAtivas(),
				dados.empresasInativas(),
				dados.totalUsuarios(),
				dados.usuariosAtivos(),
				dados.totalVinculosAtivos(),
				dados.convitesPendentes(),
				dados.empresasComFinanceiroHabilitado(),
				dados.empresasRecentes().stream().map(AdminController::paraEmpresaResponse).toList());
		return ResponseEntity.ok(response);
	}

	private static EmpresaResponse paraEmpresaResponse(Empresa empresa) {
		return new EmpresaResponse(
				empresa.getId(),
				empresa.getNome(),
				empresa.getNomeFantasia(),
				empresa.getCnpj(),
				empresa.getStatus());
	}
}
