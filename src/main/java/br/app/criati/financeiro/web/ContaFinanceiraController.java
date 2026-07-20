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

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.service.ContaFinanceiraService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.SaldoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/contas")
public class ContaFinanceiraController {

	private final ContaFinanceiraService contaFinanceiraService;
	private final SaldoFinanceiroService saldoFinanceiroService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public ContaFinanceiraController(
			ContaFinanceiraService contaFinanceiraService,
			SaldoFinanceiroService saldoFinanceiroService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.contaFinanceiraService = contaFinanceiraService;
		this.saldoFinanceiroService = saldoFinanceiroService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<ContaFinanceiraResponse>> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) TipoContaFinanceira tipo,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ContaFinanceiraResponse> resposta = contaFinanceiraService.listar(contexto, status, tipo).stream()
				.map(this::paraResponse)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping
	public ResponseEntity<ContaFinanceiraResponse> criar(
			@Valid @RequestBody ContaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaFinanceiraService.criar(
				request.nome(), request.tipo(), request.saldoInicial(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(conta));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ContaFinanceiraResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaFinanceiraService.buscar(id, contexto);
		return ResponseEntity.ok(paraResponse(conta));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ContaFinanceiraResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody ContaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaFinanceiraService.editar(
				id, request.nome(), request.tipo(), request.saldoInicial(), contexto);
		return ResponseEntity.ok(paraResponse(conta));
	}

	@PostMapping("/{id}/inativar")
	public ResponseEntity<ContaFinanceiraResponse> inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaFinanceiraService.inativar(id, contexto);
		return ResponseEntity.ok(paraResponse(conta));
	}

	@PostMapping("/{id}/reativar")
	public ResponseEntity<ContaFinanceiraResponse> reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaFinanceiraService.reativar(id, contexto);
		return ResponseEntity.ok(paraResponse(conta));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}

	private ContaFinanceiraResponse paraResponse(ContaFinanceira conta) {
		return ContaFinanceiraResponse.from(conta, saldoFinanceiroService.calcularSaldoAtual(conta));
	}
}
