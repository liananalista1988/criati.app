package br.app.criati.admin.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.usuario.web.UsuarioResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	@GetMapping("/me")
	public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.ok(new UsuarioResponse(
				principal.getUsuario().getId(),
				principal.getUsuario().getNome(),
				principal.getUsuario().getEmail(),
				principal.getUsuario().getStatus()));
	}
}
