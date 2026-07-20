package br.app.criati.usuario.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.service.CadastrarUsuarioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final CadastrarUsuarioService cadastrarUsuarioService;

	public UsuarioController(CadastrarUsuarioService cadastrarUsuarioService) {
		this.cadastrarUsuarioService = cadastrarUsuarioService;
	}

	@PostMapping
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastrarUsuarioRequest request) {
		Usuario usuario = cadastrarUsuarioService.executar(
				request.nome(), request.email(), request.senha());
		UsuarioResponse response = new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getStatus());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
