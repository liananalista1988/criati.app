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
			@RequestParam(required = false) UUID paiId,
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) Boolean principais,
			@RequestParam(required = false) Boolean permiteOrcamento,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<CategoriaFinanceiraResponse> resposta = categoriaFinanceiraService
				.listar(contexto, status, tipo, paiId, busca, principais, permiteOrcamento).stream()
				.map(c -> resposta(c))
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping
	public ResponseEntity<CategoriaFinanceiraResponse> criar(
			@Valid @RequestBody CategoriaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.criar(request.nome(), request.descricao(),
				request.categoriaPaiId(), request.tipo(), request.ordem(), request.permiteOrcamento(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta(categoria));
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoriaFinanceiraResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.buscar(id, contexto);
		return ResponseEntity.ok(resposta(categoria));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoriaFinanceiraResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody CategoriaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.editar(id, request.nome(), request.descricao(),
				request.categoriaPaiId(), request.tipo(), request.ordem(), request.permiteOrcamento(), contexto);
		return ResponseEntity.ok(resposta(categoria));
	}

	@PostMapping("/{id}/inativar")
	public ResponseEntity<CategoriaFinanceiraResponse> inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.inativar(id, contexto);
		return ResponseEntity.ok(resposta(categoria));
	}

	@PostMapping("/{id}/reativar")
	public ResponseEntity<CategoriaFinanceiraResponse> reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CategoriaFinanceira categoria = categoriaFinanceiraService.reativar(id, contexto);
		return ResponseEntity.ok(resposta(categoria));
	}

	@GetMapping("/resumo")
	public ResponseEntity<ResumoCategoriasResponse> resumir(HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.ok(ResumoCategoriasResponse.from(
				categoriaFinanceiraService.resumir(exigirAcesso(session, principal))));
	}

	private CategoriaFinanceiraResponse resposta(CategoriaFinanceira categoria) {
		return CategoriaFinanceiraResponse.from(categoria, categoriaFinanceiraService.contarSubcategorias(categoria));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
