package br.app.criati.convite.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.convite.model.Convite;
import br.app.criati.convite.service.ConviteCriado;
import br.app.criati.convite.service.ConviteService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/convites")
public class ContextoConviteController {

	private final ConviteService conviteService;
	private final ContextoEmpresaService contextoEmpresaService;
	private final boolean exporTokenBruto;

	public ContextoConviteController(
			ConviteService conviteService,
			ContextoEmpresaService contextoEmpresaService,
			@Value("${criati.convite.expor-token-bruto:false}") boolean exporTokenBruto) {
		this.conviteService = conviteService;
		this.contextoEmpresaService = contextoEmpresaService;
		this.exporTokenBruto = exporTokenBruto;
	}

	@PostMapping
	public ResponseEntity<CriarConviteResponse> criar(
			@Valid @RequestBody CriarConviteRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		ConviteCriado criado = conviteService.criar(request.email(), request.perfil(), contexto);

		CriarConviteResponse response = new CriarConviteResponse(
				paraResponse(criado.convite()),
				exporTokenBruto ? criado.tokenBruto() : null);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<ConviteResponse>> listar(
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		List<ConviteResponse> convites = conviteService.listarPorEmpresa(contexto).stream()
				.map(ContextoConviteController::paraResponse)
				.toList();
		return ResponseEntity.ok(convites);
	}

	@DeleteMapping("/{conviteId}")
	public ResponseEntity<Void> revogar(
			@PathVariable UUID conviteId,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		conviteService.revogar(conviteId, contexto);
		return ResponseEntity.noContent().build();
	}

	private static ConviteResponse paraResponse(Convite convite) {
		OffsetDateTime agora = OffsetDateTime.now();
		return new ConviteResponse(
				convite.getId(),
				convite.getEmail(),
				convite.getPerfil(),
				convite.getStatusEfetivo(agora),
				convite.getExpiraEm(),
				convite.getCriadoEm(),
				convite.getUtilizadoEm());
	}
}
