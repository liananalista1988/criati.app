package br.app.criati.acesso.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.service.VincularUsuarioEmpresaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios-empresas")
public class UsuarioEmpresaController {

	private final VincularUsuarioEmpresaService vincularUsuarioEmpresaService;

	public UsuarioEmpresaController(VincularUsuarioEmpresaService vincularUsuarioEmpresaService) {
		this.vincularUsuarioEmpresaService = vincularUsuarioEmpresaService;
	}

	@PostMapping
	public ResponseEntity<UsuarioEmpresaResponse> vincular(
			@Valid @RequestBody VincularUsuarioEmpresaRequest request) {
		UsuarioEmpresa vinculo = vincularUsuarioEmpresaService.executar(
				request.usuarioId(), request.empresaId(), request.perfil());
		UsuarioEmpresaResponse response = new UsuarioEmpresaResponse(
				vinculo.getId(),
				vinculo.getUsuario().getId(),
				vinculo.getEmpresa().getId(),
				vinculo.getPerfil(),
				vinculo.getStatus());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
