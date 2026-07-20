package br.app.criati.acesso.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import br.app.criati.acesso.service.GerenciarUsuarioEmpresaService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/usuarios")
public class ContextoUsuarioEmpresaController {

	private final GerenciarUsuarioEmpresaService gerenciarUsuarioEmpresaService;
	private final ContextoEmpresaService contextoEmpresaService;

	public ContextoUsuarioEmpresaController(
			GerenciarUsuarioEmpresaService gerenciarUsuarioEmpresaService,
			ContextoEmpresaService contextoEmpresaService) {
		this.gerenciarUsuarioEmpresaService = gerenciarUsuarioEmpresaService;
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@GetMapping
	public ResponseEntity<List<IntegranteEmpresaResponse>> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) PerfilUsuario perfil,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		List<IntegranteEmpresaResponse> integrantes = gerenciarUsuarioEmpresaService
				.listar(contexto, status, perfil, busca)
				.stream()
				.map(vinculo -> paraResponse(vinculo, contexto))
				.toList();
		return ResponseEntity.ok(integrantes);
	}

	@GetMapping("/{usuarioEmpresaId}")
	public ResponseEntity<IntegranteEmpresaResponse> consultar(
			@PathVariable UUID usuarioEmpresaId,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		UsuarioEmpresa vinculo = gerenciarUsuarioEmpresaService.buscar(usuarioEmpresaId, contexto);
		return ResponseEntity.ok(paraResponse(vinculo, contexto));
	}

	@PatchMapping("/{usuarioEmpresaId}/perfil")
	public ResponseEntity<IntegranteEmpresaResponse> alterarPerfil(
			@PathVariable UUID usuarioEmpresaId,
			@Valid @RequestBody AlterarPerfilRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		UsuarioEmpresa vinculo = gerenciarUsuarioEmpresaService.alterarPerfil(
				usuarioEmpresaId, request.perfil(), contexto);
		return ResponseEntity.ok(paraResponse(vinculo, contexto));
	}

	@PostMapping("/{usuarioEmpresaId}/suspender")
	public ResponseEntity<IntegranteEmpresaResponse> suspender(
			@PathVariable UUID usuarioEmpresaId,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		UsuarioEmpresa vinculo = gerenciarUsuarioEmpresaService.suspender(usuarioEmpresaId, contexto);
		return ResponseEntity.ok(paraResponse(vinculo, contexto));
	}

	@PostMapping("/{usuarioEmpresaId}/reativar")
	public ResponseEntity<IntegranteEmpresaResponse> reativar(
			@PathVariable UUID usuarioEmpresaId,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		UsuarioEmpresa vinculo = gerenciarUsuarioEmpresaService.reativar(usuarioEmpresaId, contexto);
		return ResponseEntity.ok(paraResponse(vinculo, contexto));
	}

	@DeleteMapping("/{usuarioEmpresaId}")
	public ResponseEntity<Void> remover(
			@PathVariable UUID usuarioEmpresaId,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		gerenciarUsuarioEmpresaService.remover(usuarioEmpresaId, contexto);
		return ResponseEntity.noContent().build();
	}

	private static IntegranteEmpresaResponse paraResponse(UsuarioEmpresa vinculo, ContextoEmpresaAtual contexto) {
		return new IntegranteEmpresaResponse(
				vinculo.getId(),
				vinculo.getUsuario().getId(),
				vinculo.getUsuario().getNome(),
				vinculo.getUsuario().getEmail(),
				vinculo.getPerfil(),
				vinculo.getStatus(),
				vinculo.getCriadoEm(),
				vinculo.getAtualizadoEm(),
				vinculo.getId().equals(contexto.usuarioEmpresaId()));
	}
}
