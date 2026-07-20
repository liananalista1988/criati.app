package br.app.criati.financeiro.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.LancamentoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/lancamentos")
public class LancamentoFinanceiroController {

	private final LancamentoFinanceiroService lancamentoFinanceiroService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public LancamentoFinanceiroController(
			LancamentoFinanceiroService lancamentoFinanceiroService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.lancamentoFinanceiroService = lancamentoFinanceiroService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<LancamentoFinanceiroResponse>> listar(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
			@RequestParam(required = false) TipoFinanceiro tipo,
			@RequestParam(required = false) StatusLancamentoFinanceiro status,
			@RequestParam(required = false) UUID contaId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<LancamentoFinanceiroResponse> resposta = lancamentoFinanceiroService
				.listar(contexto, dataInicial, dataFinal, tipo, status, contaId, categoriaId, busca)
				.stream()
				.map(LancamentoFinanceiroResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping
	public ResponseEntity<LancamentoFinanceiroResponse> criar(
			@Valid @RequestBody LancamentoFinanceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.criar(
				request.contaId(),
				request.categoriaId(),
				request.tipo(),
				request.descricao(),
				request.valor(),
				request.dataCompetencia(),
				request.status(),
				request.dataPagamento(),
				request.observacao(),
				contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(LancamentoFinanceiroResponse.from(lancamento));
	}

	@GetMapping("/{id}")
	public ResponseEntity<LancamentoFinanceiroResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.buscar(id, contexto);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(lancamento));
	}

	@PutMapping("/{id}")
	public ResponseEntity<LancamentoFinanceiroResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody LancamentoFinanceiroEdicaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.editar(
				id,
				request.contaId(),
				request.categoriaId(),
				request.descricao(),
				request.valor(),
				request.dataCompetencia(),
				request.observacao(),
				contexto);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(lancamento));
	}

	@PostMapping("/{id}/pagar")
	public ResponseEntity<LancamentoFinanceiroResponse> pagar(
			@PathVariable UUID id,
			@Valid @RequestBody PagarLancamentoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.pagar(id, request.dataPagamento(), contexto);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(lancamento));
	}

	@PostMapping("/{id}/reabrir")
	public ResponseEntity<LancamentoFinanceiroResponse> reabrir(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.reabrir(id, contexto);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(lancamento));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<LancamentoFinanceiroResponse> cancelar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		LancamentoFinanceiro lancamento = lancamentoFinanceiroService.cancelar(id, contexto);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(lancamento));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
