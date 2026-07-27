package br.app.criati.financeiro.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.FaturaCartaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/faturas")
public class FaturaCartaoController {

	private final FaturaCartaoService service;
	private final ContextoFinanceiroService contextoFinanceiro;

	public FaturaCartaoController(FaturaCartaoService service, ContextoFinanceiroService contextoFinanceiro) {
		this.service = service;
		this.contextoFinanceiro = contextoFinanceiro;
	}

	@GetMapping
	public List<FaturaCartaoResponse> listar(@RequestParam(required = false) UUID cartaoPrincipalId,
			@RequestParam(required = false) StatusFaturaCartao status, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return service.listar(contexto, cartaoPrincipalId, status).stream()
				.map(fatura -> FaturaCartaoResponse.from(service.buscar(fatura.getId(), contexto)))
				.toList();
	}

	@GetMapping("/{id}")
	public FaturaCartaoResponse buscar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return FaturaCartaoResponse.from(service.buscar(id, contexto(session, principal)));
	}

	@PostMapping
	public ResponseEntity<FaturaCartaoResponse> abrir(@Valid @RequestBody AbrirFaturaCartaoRequest request,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResponseEntity.status(HttpStatus.CREATED).body(FaturaCartaoResponse.from(
				service.abrir(request.cartaoId(), request.competencia(), contexto(session, principal))));
	}

	@PostMapping("/{id}/recompor")
	public FaturaCartaoResponse recompor(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return FaturaCartaoResponse.from(service.recompor(id, contexto(session, principal)));
	}

	@PostMapping("/{id}/fechar")
	public FaturaCartaoResponse fechar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return FaturaCartaoResponse.from(service.fechar(id, contexto(session, principal)));
	}

	private ContextoEmpresaAtual contexto(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiro.exigirAcesso(session, principal.getUsuario().getId());
	}
}
