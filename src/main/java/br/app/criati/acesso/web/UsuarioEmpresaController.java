package br.app.criati.acesso.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.service.VincularUsuarioEmpresaService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios-empresas")
public class UsuarioEmpresaController {

	private final VincularUsuarioEmpresaService vincularUsuarioEmpresaService;
	private final ContextoEmpresaService contextoEmpresaService;

	public UsuarioEmpresaController(
			VincularUsuarioEmpresaService vincularUsuarioEmpresaService,
			ContextoEmpresaService contextoEmpresaService) {
		this.vincularUsuarioEmpresaService = vincularUsuarioEmpresaService;
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@PostMapping
	public ResponseEntity<UsuarioEmpresaResponse> vincular(
			@Valid @RequestBody VincularUsuarioEmpresaRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		UsuarioEmpresa vinculo = vincularUsuarioEmpresaService.executar(
				request.usuarioId(), request.empresaId(), request.perfil(), contexto);
		UsuarioEmpresaResponse response = new UsuarioEmpresaResponse(
				vinculo.getId(),
				vinculo.getUsuario().getId(),
				vinculo.getEmpresa().getId(),
				vinculo.getPerfil(),
				vinculo.getStatus());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
