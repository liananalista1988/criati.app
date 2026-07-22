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

import br.app.criati.financeiro.model.CompromissoFinanceiro;
import br.app.criati.financeiro.service.CompromissoFinanceiroService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/compromissos")
public class CompromissoFinanceiroController {

	private final CompromissoFinanceiroService compromissoFinanceiroService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public CompromissoFinanceiroController(CompromissoFinanceiroService compromissoFinanceiroService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.compromissoFinanceiroService = compromissoFinanceiroService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<CompromissoFinanceiroResponse>> listar(
			@RequestParam(required = false) Boolean ativo,
			@RequestParam(required = false) UUID pessoaId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<CompromissoFinanceiroResponse> resposta = compromissoFinanceiroService
				.listar(contexto, ativo, pessoaId, categoriaId, busca).stream()
				.map(CompromissoFinanceiroResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CompromissoFinanceiroResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CompromissoFinanceiro compromisso = compromissoFinanceiroService.buscar(id, contexto);
		return ResponseEntity.ok(CompromissoFinanceiroResponse.from(compromisso));
	}

	@PostMapping
	public ResponseEntity<CompromissoFinanceiroResponse> criar(
			@Valid @RequestBody CompromissoFinanceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CompromissoFinanceiro compromisso = compromissoFinanceiroService.criar(request.descricao(),
				request.categoriaId(), request.pessoaFinanceiraId(), request.parteFinanceiraId(),
				request.contaPadraoId(), request.recorrenciaId(), request.tipoValor(), request.valorPadrao(),
				request.diaVencimentoPadrao(), request.formaPagamentoPadrao(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(CompromissoFinanceiroResponse.from(compromisso));
	}

	@PutMapping("/{id}")
	public ResponseEntity<CompromissoFinanceiroResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody CompromissoFinanceiroEdicaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CompromissoFinanceiro compromisso = compromissoFinanceiroService.editar(id, request.descricao(),
				request.categoriaId(), request.pessoaFinanceiraId(), request.parteFinanceiraId(),
				request.contaPadraoId(), request.tipoValor(), request.valorPadrao(), request.diaVencimentoPadrao(),
				request.formaPagamentoPadrao(), request.observacao(), contexto);
		return ResponseEntity.ok(CompromissoFinanceiroResponse.from(compromisso));
	}

	@PostMapping("/{id}/ativar")
	public ResponseEntity<CompromissoFinanceiroResponse> ativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(CompromissoFinanceiroResponse.from(compromissoFinanceiroService.ativar(id, contexto)));
	}

	@PostMapping("/{id}/desativar")
	public ResponseEntity<CompromissoFinanceiroResponse> desativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(CompromissoFinanceiroResponse.from(compromissoFinanceiroService.desativar(id, contexto)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
