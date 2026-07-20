package br.app.criati.tenant.web;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto")
public class ContextoEmpresaController {

	private final ContextoEmpresaService contextoEmpresaService;

	public ContextoEmpresaController(ContextoEmpresaService contextoEmpresaService) {
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@GetMapping("/empresas")
	public ResponseEntity<List<EmpresaVinculadaResponse>> listarEmpresas(
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		List<EmpresaVinculadaResponse> empresas = contextoEmpresaService
				.listarVinculosAtivos(principal.getUsuario().getId())
				.stream()
				.map(ContextoEmpresaController::paraEmpresaVinculadaResponse)
				.toList();
		return ResponseEntity.ok(empresas);
	}

	@PostMapping("/empresa-ativa")
	public ResponseEntity<ContextoEmpresaAtivaResponse> selecionarEmpresaAtiva(
			@Valid @RequestBody SelecionarEmpresaAtivaRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.selecionarEmpresaAtiva(
				session, principal.getUsuario().getId(), request.empresaId());
		return ResponseEntity.ok(ContextoEmpresaAtivaResponse.from(contexto));
	}

	@GetMapping("/empresa-ativa")
	public ResponseEntity<ContextoEmpresaAtivaResponse> consultarEmpresaAtiva(
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		Optional<ContextoEmpresaAtual> contexto = contextoEmpresaService.obterContextoAtual(
				session, principal.getUsuario().getId());
		return contexto
				.map(ContextoEmpresaAtivaResponse::from)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@DeleteMapping("/empresa-ativa")
	public ResponseEntity<Void> limparEmpresaAtiva(HttpSession session) {
		contextoEmpresaService.limparContexto(session);
		return ResponseEntity.noContent().build();
	}

	private static EmpresaVinculadaResponse paraEmpresaVinculadaResponse(UsuarioEmpresa vinculo) {
		return new EmpresaVinculadaResponse(
				vinculo.getId(),
				vinculo.getEmpresa().getId(),
				vinculo.getEmpresa().getNome(),
				vinculo.getEmpresa().getCnpj(),
				vinculo.getPerfil(),
				vinculo.getStatus());
	}
}
