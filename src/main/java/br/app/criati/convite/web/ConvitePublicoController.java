package br.app.criati.convite.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.convite.model.Convite;
import br.app.criati.convite.service.AceitarConviteService;
import br.app.criati.convite.service.ConviteService;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.web.UsuarioResponse;
import jakarta.validation.Valid;

// Rotas publicas (sem autenticacao). Aceitar convite nunca autentica
// automaticamente: nenhuma sessao e criada aqui, o convidado faz login
// normalmente depois pelo fluxo existente.
@RestController
@RequestMapping("/api/convites")
public class ConvitePublicoController {

	private final ConviteService conviteService;
	private final AceitarConviteService aceitarConviteService;

	public ConvitePublicoController(ConviteService conviteService, AceitarConviteService aceitarConviteService) {
		this.conviteService = conviteService;
		this.aceitarConviteService = aceitarConviteService;
	}

	@GetMapping("/{token}")
	public ResponseEntity<ConviteValidacaoPublicaResponse> validar(@PathVariable String token) {
		return conviteService.buscarValidoPeloToken(token)
				.map(ConvitePublicoController::paraValidacaoPublica)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.ok(ConviteValidacaoPublicaResponse.invalido()));
	}

	@PostMapping("/{token}/aceitar")
	public ResponseEntity<UsuarioResponse> aceitar(
			@PathVariable String token,
			@Valid @RequestBody AceitarConviteRequest request) {
		Usuario usuario = aceitarConviteService.aceitar(
				token, request.nome(), request.senha(), request.confirmacaoSenha());
		UsuarioResponse response = new UsuarioResponse(
				usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getStatus());
		return ResponseEntity.ok(response);
	}

	private static ConviteValidacaoPublicaResponse paraValidacaoPublica(Convite convite) {
		return ConviteValidacaoPublicaResponse.valido(
				convite.getEmpresa().getNome(),
				mascarar(convite.getEmail()),
				convite.getPerfil(),
				convite.getExpiraEm());
	}

	private static String mascarar(String email) {
		int arroba = email.indexOf('@');
		if (arroba <= 0) {
			return "***";
		}
		String parteLocal = email.substring(0, arroba);
		String dominio = email.substring(arroba);
		int visivel = Math.min(2, parteLocal.length());
		return parteLocal.substring(0, visivel) + "***" + dominio;
	}
}
