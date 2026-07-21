package br.app.criati.admin.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.admin.AdminUsuarioService;
import br.app.criati.admin.AdminUsuarioService.UsuarioComResumo;
import br.app.criati.admin.AdminUsuarioService.UsuarioDetalheDados;
import br.app.criati.convite.model.Convite;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;

// Consulta global de usuarios (nao empresa-scoped). So mutacoes ja
// consolidadas em outras regras sao expostas (nenhuma ativacao/inativacao
// global de usuario ainda: ver docs/PAINEL_ADMINISTRATIVO.md - limitacao
// documentada, nao ha regra de negocio consolidada para essa acao).
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {

	private final AdminUsuarioService adminUsuarioService;

	public AdminUsuarioController(AdminUsuarioService adminUsuarioService) {
		this.adminUsuarioService = adminUsuarioService;
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
