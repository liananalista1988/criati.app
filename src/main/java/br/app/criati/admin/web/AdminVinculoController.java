package br.app.criati.admin.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.web.AlterarPerfilRequest;
import br.app.criati.admin.AdminVinculoService;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import jakarta.validation.Valid;

// Gestao global de vinculos Usuario<->Empresa (nao empresa-scoped). Nunca
// aceita empresaId de rotas empresariais nem cria senha - so referencia
// usuarios/empresas ja existentes pelo id.
@RestController
@RequestMapping("/api/admin/vinculos")
public class AdminVinculoController {

	private final AdminVinculoService adminVinculoService;

	public AdminVinculoController(AdminVinculoService adminVinculoService) {
		this.adminVinculoService = adminVinculoService;
	}

	@GetMapping
	public ResponseEntity<List<VinculoAdminResponse>> listar(
			@RequestParam(required = false) UUID usuarioId,
			@RequestParam(required = false) UUID empresaId,
			@RequestParam(required = false) PerfilUsuario perfil,
			@RequestParam(required = false) StatusCadastro status) {
		List<VinculoAdminResponse> vinculos = adminVinculoService
				.listarTodos(usuarioId, empresaId, perfil, status).stream()
				.map(AdminVinculoController::paraResponse)
				.toList();
		return ResponseEntity.ok(vinculos);
	}

	@PostMapping
	public ResponseEntity<VinculoAdminResponse> criar(@Valid @RequestBody CriarVinculoAdminRequest request) {
		UsuarioEmpresa vinculo = adminVinculoService.criar(request.usuarioId(), request.empresaId(), request.perfil());
		return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(vinculo));
	}

	@PatchMapping("/{vinculoId}/perfil")
	public ResponseEntity<VinculoAdminResponse> alterarPerfil(
			@PathVariable UUID vinculoId, @Valid @RequestBody AlterarPerfilRequest request) {
		UsuarioEmpresa vinculo = adminVinculoService.alterarPerfil(vinculoId, request.perfil());
		return ResponseEntity.ok(paraResponse(vinculo));
	}

	@PostMapping("/{vinculoId}/suspender")
	public ResponseEntity<VinculoAdminResponse> suspender(@PathVariable UUID vinculoId) {
		return ResponseEntity.ok(paraResponse(adminVinculoService.suspender(vinculoId)));
	}

	@PostMapping("/{vinculoId}/reativar")
	public ResponseEntity<VinculoAdminResponse> reativar(@PathVariable UUID vinculoId) {
		return ResponseEntity.ok(paraResponse(adminVinculoService.reativar(vinculoId)));
	}

	@DeleteMapping("/{vinculoId}")
	public ResponseEntity<Void> remover(@PathVariable UUID vinculoId) {
		adminVinculoService.remover(vinculoId);
		return ResponseEntity.noContent().build();
	}

	private static VinculoAdminResponse paraResponse(UsuarioEmpresa vinculo) {
		return new VinculoAdminResponse(
				vinculo.getId(),
				vinculo.getUsuario().getId(),
				vinculo.getUsuario().getNome(),
				vinculo.getUsuario().getEmail(),
				vinculo.getEmpresa().getId(),
				vinculo.getEmpresa().getNome(),
				vinculo.getPerfil(),
				vinculo.getStatus(),
				vinculo.getCriadoEm());
	}
}
