package br.app.criati.admin.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.admin.AdminUsuarioService;
import br.app.criati.admin.AdminUsuarioService.UsuarioComResumo;
import br.app.criati.admin.AdminUsuarioService.UsuarioDetalheDados;
import br.app.criati.admin.RedefinirSenhaGlobalService;
import br.app.criati.convite.model.Convite;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.web.IpOrigemResolver;
import br.app.criati.usuario.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

// Consulta global de usuarios (nao empresa-scoped). Mutacoes globais
// consolidadas: redefinicao administrativa de senha (CRIATI-SEG-001, ver
// RedefinirSenhaGlobalService). Nenhuma ativacao/inativacao global de
// usuario ainda: ver docs/PAINEL_ADMINISTRATIVO.md - limitacao documentada,
// nao ha regra de negocio consolidada para essa acao.
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

	private final AdminUsuarioService adminUsuarioService;
	private final RedefinirSenhaGlobalService redefinirSenhaGlobalService;
	private final IpOrigemResolver ipOrigemResolver;

	public AdminUsuarioController(AdminUsuarioService adminUsuarioService,
			RedefinirSenhaGlobalService redefinirSenhaGlobalService, IpOrigemResolver ipOrigemResolver) {
		this.adminUsuarioService = adminUsuarioService;
		this.redefinirSenhaGlobalService = redefinirSenhaGlobalService;
		this.ipOrigemResolver = ipOrigemResolver;
	}

	@GetMapping
	public ResponseEntity<List<UsuarioAdminResponse>> listar(
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) StatusCadastro status) {
		List<UsuarioAdminResponse> usuarios = adminUsuarioService.listarTodos(busca, status).stream()
				.map(AdminUsuarioController::paraResponse)
				.toList();
		return ResponseEntity.ok(usuarios);
	}

	@GetMapping("/{usuarioId}")
	public ResponseEntity<UsuarioDetalheAdminResponse> detalhar(@PathVariable UUID usuarioId) {
		UsuarioDetalheDados dados = adminUsuarioService.buscarDetalhe(usuarioId);
		Usuario usuario = dados.usuario();

		List<UsuarioDetalheAdminResponse.VinculoResumo> vinculos = dados.vinculos().stream()
				.map(AdminUsuarioController::paraVinculoResumo)
				.toList();
		List<UsuarioDetalheAdminResponse.ConvitePendenteResumo> convites = dados.convitesPendentes().stream()
				.map(AdminUsuarioController::paraConviteResumo)
				.toList();

		UsuarioDetalheAdminResponse response = new UsuarioDetalheAdminResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getStatus(),
				usuario.getCriadoEm(),
				usuario.isSuperAdministrador(),
				vinculos,
				convites);
		return ResponseEntity.ok(response);
	}

	// CRIATI-SEG-001: contrato proprio (RedefinirSenhaGlobalRequest/Response),
	// nunca reaproveita UsuarioAdminResponse/UsuarioDetalheAdminResponse - a
	// senha e o evento de auditoria criado nunca aparecem na resposta.
	// Autorizacao real (ROLE_SUPERADMIN) ja garantida pelo SecurityConfig
	// para qualquer verbo em /api/admin/**, e reforcada de novo dentro do
	// service (defesa em profundidade, nunca so escondendo o botao na tela).
	@PostMapping("/{usuarioId}/redefinir-senha")
	public ResponseEntity<RedefinirSenhaGlobalResponse> redefinirSenha(
			@PathVariable UUID usuarioId,
			@Valid @RequestBody RedefinirSenhaGlobalRequest request,
			HttpServletRequest httpRequest,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		String ipOrigem = ipOrigemResolver.resolver(httpRequest);
		redefinirSenhaGlobalService.redefinirSenha(
				usuarioId, request.novaSenha(), request.confirmacaoSenha(), principal.getUsuario().getId(), ipOrigem);
		return ResponseEntity.ok(RedefinirSenhaGlobalResponse.sucesso());
	}

	private static UsuarioAdminResponse paraResponse(UsuarioComResumo resumo) {
		Usuario usuario = resumo.usuario();
		return new UsuarioAdminResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getStatus(),
				usuario.getCriadoEm(),
				resumo.quantidadeEmpresas(),
				resumo.vinculosAtivos());
	}

	private static UsuarioDetalheAdminResponse.VinculoResumo paraVinculoResumo(UsuarioEmpresa vinculo) {
		return new UsuarioDetalheAdminResponse.VinculoResumo(
				vinculo.getId(),
				vinculo.getEmpresa().getId(),
				vinculo.getEmpresa().getNome(),
				vinculo.getPerfil(),
				vinculo.getStatus(),
				vinculo.getCriadoEm());
	}

	private static UsuarioDetalheAdminResponse.ConvitePendenteResumo paraConviteResumo(Convite convite) {
		return new UsuarioDetalheAdminResponse.ConvitePendenteResumo(
				convite.getId(),
				convite.getEmpresa().getId(),
				convite.getEmpresa().getNome(),
				convite.getPerfil(),
				convite.getStatus(),
				convite.getExpiraEm(),
				convite.getCriadoEm());
	}
}
