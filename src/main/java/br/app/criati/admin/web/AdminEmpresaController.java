package br.app.criati.admin.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.web.UsuarioEmpresaResponse;
import br.app.criati.admin.AdminEmpresaService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.web.EmpresaResponse;
import br.app.criati.usuario.web.UsuarioResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/empresas")
public class AdminEmpresaController {

	private final AdminEmpresaService adminEmpresaService;

	public AdminEmpresaController(AdminEmpresaService adminEmpresaService) {
		this.adminEmpresaService = adminEmpresaService;
	}

	@PostMapping
	public ResponseEntity<EmpresaComAdministradorResponse> criar(
			@Valid @RequestBody CriarEmpresaComAdministradorRequest request) {
		UsuarioEmpresa vinculo = adminEmpresaService.criarComAdministrador(
				request.empresa().nome(),
				request.empresa().nomeFantasia(),
				request.empresa().cnpj(),
				request.administrador().nome(),
				request.administrador().email(),
				request.administrador().senha());

		EmpresaComAdministradorResponse response = new EmpresaComAdministradorResponse(
				paraEmpresaResponse(vinculo.getEmpresa()),
				new UsuarioResponse(
						vinculo.getUsuario().getId(),
						vinculo.getUsuario().getNome(),
						vinculo.getUsuario().getEmail(),
						vinculo.getUsuario().getStatus()),
				new UsuarioEmpresaResponse(
						vinculo.getId(),
						vinculo.getUsuario().getId(),
						vinculo.getEmpresa().getId(),
						vinculo.getPerfil(),
						vinculo.getStatus()));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<EmpresaResponse>> listar() {
		List<EmpresaResponse> empresas = adminEmpresaService.listarTodas().stream()
				.map(AdminEmpresaController::paraEmpresaResponse)
				.toList();
		return ResponseEntity.ok(empresas);
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
