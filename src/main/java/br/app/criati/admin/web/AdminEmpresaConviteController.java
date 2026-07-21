package br.app.criati.admin.web;

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
import br.app.criati.convite.web.ConviteResponse;
import br.app.criati.convite.web.CriarConviteRequest;
import br.app.criati.convite.web.CriarConviteResponse;
import br.app.criati.security.UsuarioPrincipal;
import jakarta.validation.Valid;

// Convites de qualquer empresa, vistos/geridos pelo Superadministrador fora do
// contexto de sessao empresarial - nunca reaproveita /api/contexto/convites/**
// (aquele exige uma empresa ativa selecionada, que o Superadministrador nao
// possui). O empresaId vem sempre do path, nunca de um campo do corpo.
@RestController
@RequestMapping("/api/admin/empresas/{empresaId}/convites")
public class AdminEmpresaConviteController {

	private final ConviteService conviteService;
	private final boolean exporTokenBruto;

	public AdminEmpresaConviteController(
			ConviteService conviteService,
			@Value("${criati.convite.expor-token-bruto:false}") boolean exporTokenBruto) {
		this.conviteService = conviteService;
		this.exporTokenBruto = exporTokenBruto;
	}

	@GetMapping
	public ResponseEntity<List<ConviteResponse>> listar(@PathVariable UUID empresaId) {
		List<ConviteResponse> convites = conviteService.listarPorEmpresaComoSuperAdministrador(empresaId).stream()
				.map(AdminEmpresaConviteController::paraResponse)
				.toList();
		return ResponseEntity.ok(convites);
	}

	@PostMapping
	public ResponseEntity<CriarConviteResponse> criar(
			@PathVariable UUID empresaId,
			@Valid @RequestBody CriarConviteRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ConviteCriado criado = conviteService.criarComoSuperAdministrador(
				empresaId, request.email(), request.perfil(), principal.getUsuario().getId());

		CriarConviteResponse response = new CriarConviteResponse(
				paraResponse(criado.convite()),
				exporTokenBruto ? criado.tokenBruto() : null);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@DeleteMapping("/{conviteId}")
	public ResponseEntity<Void> revogar(@PathVariable UUID empresaId, @PathVariable UUID conviteId) {
		conviteService.revogarComoSuperAdministrador(empresaId, conviteId);
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
