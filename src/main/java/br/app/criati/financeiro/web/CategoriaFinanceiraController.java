package br.app.criati.financeiro.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.service.CategoriaFinanceiraService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/categorias")
public class CategoriaFinanceiraController {

	private final CategoriaFinanceiraService categoriaFinanceiraService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public CategoriaFinanceiraController(
			CategoriaFinanceiraService categoriaFinanceiraService, ContextoFinanceiroService contextoFinanceiroService) {
		this.categoriaFinanceiraService = categoriaFinanceiraService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<CategoriaFinanceiraResponse>> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) TipoFinanceiro tipo,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<CategoriaFinanceiraResponse> resposta = categoriaFinanceiraService.listar(contexto, status, tipo).stream()
				.map(CategoriaFinanceiraResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping
	public ResponseEntity<CategoriaFinanceiraResponse> criar(
			@Valid @RequestBody CategoriaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.criar(request.nome(), request.tipo(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaFinanceiraResponse.from(categoria));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoriaFinanceiraResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.buscar(id, contexto);
		return ResponseEntity.ok(CategoriaFinanceiraResponse.from(categoria));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoriaFinanceiraResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody CategoriaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.editar(
				id, request.nome(), request.tipo(), contexto);
		return ResponseEntity.ok(CategoriaFinanceiraResponse.from(categoria));
	}

	@PostMapping("/{id}/inativar")
	public ResponseEntity<CategoriaFinanceiraResponse> inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.inativar(id, contexto);
		return ResponseEntity.ok(CategoriaFinanceiraResponse.from(categoria));
	}

	@PostMapping("/{id}/reativar")
	public ResponseEntity<CategoriaFinanceiraResponse> reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.reativar(id, contexto);
		return ResponseEntity.ok(CategoriaFinanceiraResponse.from(categoria));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
