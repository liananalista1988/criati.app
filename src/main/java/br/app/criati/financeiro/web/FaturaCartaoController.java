package br.app.criati.financeiro.web;

import java.math.BigDecimal;
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

import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.FaturaCartaoService;
import br.app.criati.financeiro.service.PagamentoFaturaCartaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/faturas")
public class FaturaCartaoController {

	private final FaturaCartaoService service;
	private final PagamentoFaturaCartaoService pagamentoService;
	private final ContextoFinanceiroService contextoFinanceiro;

	public FaturaCartaoController(FaturaCartaoService service, PagamentoFaturaCartaoService pagamentoService,
			ContextoFinanceiroService contextoFinanceiro) {
		this.service = service;
		this.pagamentoService = pagamentoService;
		this.contextoFinanceiro = contextoFinanceiro;
	}

	@GetMapping
	public List<FaturaCartaoResponse> listar(@RequestParam(required = false) UUID cartaoPrincipalId,
			@RequestParam(required = false) StatusFaturaCartao status, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return service.listar(contexto, cartaoPrincipalId, status).stream()
				.map(fatura -> responder(service.buscar(fatura.getId(), contexto), contexto))
				.toList();
	}

	@GetMapping("/{id}")
	public FaturaCartaoResponse buscar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return responder(service.buscar(id, contexto), contexto);
	}

	@PostMapping
	public ResponseEntity<FaturaCartaoResponse> abrir(@Valid @RequestBody AbrirFaturaCartaoRequest request,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		var resultado = service.abrir(request.cartaoId(), request.competencia(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(responder(resultado, contexto));
	}

	@PostMapping("/{id}/recompor")
	public FaturaCartaoResponse recompor(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return responder(service.recompor(id, contexto), contexto);
	}

	@PostMapping("/{id}/fechar")
	public FaturaCartaoResponse fechar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return responder(service.fechar(id, contexto), contexto);
	}

	@GetMapping("/{id}/pagamentos")
	public List<PagamentoFaturaCartaoResponse> listarPagamentos(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		return pagamentoService.listar(id, contexto).stream().map(PagamentoFaturaCartaoResponse::from).toList();
	}

	@PostMapping("/{id}/pagamentos")
	public ResponseEntity<PagamentoFaturaCartaoResponse> registrarPagamento(@PathVariable UUID id,
			@Valid @RequestBody RegistrarPagamentoFaturaRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		var pagamento = pagamentoService.registrarPagamento(id, request.contaPagamentoId(), request.dataPagamento(),
				request.valor(), request.tipo(), request.formaPagamento(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoFaturaCartaoResponse.from(pagamento));
	}

	@PostMapping("/{id}/encargos")
	public FaturaCartaoResponse aplicarEncargos(@PathVariable UUID id,
			@RequestBody(required = false) AplicarEncargosFaturaRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contexto(session, principal);
		BigDecimal juros = request == null ? null : request.juros();
		BigDecimal multa = request == null ? null : request.multa();
		FaturaCartao fatura = pagamentoService.aplicarEncargos(id, juros, multa, contexto);
		return responder(service.buscar(fatura.getId(), contexto), contexto);
	}

	private FaturaCartaoResponse responder(FaturaCartaoService.ResultadoFatura resultado,
			ContextoEmpresaAtual contexto) {
		BigDecimal valorPago = pagamentoService.totalPago(resultado.fatura().getId(), contexto);
		return FaturaCartaoResponse.from(resultado, valorPago);
	}

	private ContextoEmpresaAtual contexto(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiro.exigirAcesso(session, principal.getUsuario().getId());
	}
}
