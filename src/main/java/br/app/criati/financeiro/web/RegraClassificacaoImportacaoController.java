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

import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.RegraClassificacaoImportacaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * CRUD de regras de classificacao de importacao bancaria (CRIATI-IMP-002A).
 * Escrita estrutural restrita a ADMINISTRADOR (mesmo padrao de
 * Contas/Categorias), verificado em RegraClassificacaoImportacaoService.
 * Nunca recebe empresaId do cliente - a empresa vem exclusivamente do
 * contexto da sessao (ContextoFinanceiroService.exigirAcesso).
 */
@RestController
@RequestMapping("/api/contexto/financeiro/regras-importacao")
public class RegraClassificacaoImportacaoController {

	private final RegraClassificacaoImportacaoService service;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public RegraClassificacaoImportacaoController(
			RegraClassificacaoImportacaoService service, ContextoFinanceiroService contextoFinanceiroService) {
		this.service = service;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public List<RegraClassificacaoImportacaoResponse> listar(
			@RequestParam(required = false) StatusCadastro status,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return service.listar(exigirAcesso(session, principal), status).stream()
				.map(RegraClassificacaoImportacaoResponse::from).toList();
	}

	@GetMapping("/{id}")
	public RegraClassificacaoImportacaoResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return RegraClassificacaoImportacaoResponse.from(service.buscar(id, exigirAcesso(session, principal)));
	}

	@PostMapping
	public ResponseEntity<RegraClassificacaoImportacaoResponse> criar(
			@Valid @RequestBody RegraClassificacaoImportacaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		RegraClassificacaoImportacao regra = service.criar(request.toCommand(), exigirAcesso(session, principal));
		return ResponseEntity.status(HttpStatus.CREATED).body(RegraClassificacaoImportacaoResponse.from(regra));
	}

	@PutMapping("/{id}")
	public RegraClassificacaoImportacaoResponse editar(
			@PathVariable UUID id,
			@Valid @RequestBody RegraClassificacaoImportacaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return RegraClassificacaoImportacaoResponse.from(
				service.editar(id, request.toCommand(), exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/inativar")
	public RegraClassificacaoImportacaoResponse inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return RegraClassificacaoImportacaoResponse.from(service.inativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/reativar")
	public RegraClassificacaoImportacaoResponse reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return RegraClassificacaoImportacaoResponse.from(service.reativar(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
