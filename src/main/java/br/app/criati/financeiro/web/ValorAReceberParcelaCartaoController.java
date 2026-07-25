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

import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.RessarcimentoParcelaCartaoService;
import br.app.criati.financeiro.service.ValorAReceberParcelaCartaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/valores-a-receber-cartao")
public class ValorAReceberParcelaCartaoController {

	private final ValorAReceberParcelaCartaoService valorAReceberService;
	private final RessarcimentoParcelaCartaoService ressarcimentoService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public ValorAReceberParcelaCartaoController(ValorAReceberParcelaCartaoService valorAReceberService,
			RessarcimentoParcelaCartaoService ressarcimentoService, ContextoFinanceiroService contextoFinanceiroService) {
		this.valorAReceberService = valorAReceberService;
		this.ressarcimentoService = ressarcimentoService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<ValorAReceberParcelaCartaoResponse>> listar(
			@RequestParam(required = false) UUID compraId,
			@RequestParam(required = false) StatusValorAReceberCompraCartao status,
			@RequestParam(required = false) Boolean atrasadas,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ValorAReceberParcelaCartaoResponse> resposta = valorAReceberService
				.listar(contexto, compraId, status, atrasadas).stream().map(ValorAReceberParcelaCartaoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/vencidas")
	public ResponseEntity<List<ValorAReceberParcelaCartaoResponse>> listarVencidas(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ValorAReceberParcelaCartaoResponse> resposta = valorAReceberService.listarVencidas(contexto).stream()
				.map(ValorAReceberParcelaCartaoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/proximas-vencimento")
	public ResponseEntity<List<ValorAReceberParcelaCartaoResponse>> listarProximasDoVencimento(
			@RequestParam(required = false) Integer dias,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ValorAReceberParcelaCartaoResponse> resposta = valorAReceberService
				.listarProximasDoVencimento(contexto, dias).stream().map(ValorAReceberParcelaCartaoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/resumo")
	public ResponseEntity<ResumoValoresAReceberCompraTerceiroResponse> resumir(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(ResumoValoresAReceberCompraTerceiroResponse.from(valorAReceberService.resumir(contexto)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ValorAReceberParcelaCartaoResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ValorAReceberParcelaCartao valor = valorAReceberService.buscar(id, contexto);
		return ResponseEntity.ok(ValorAReceberParcelaCartaoResponse.from(valor));
	}

	@PutMapping("/{id}/data-prometida")
	public ResponseEntity<ValorAReceberParcelaCartaoResponse> registrarDataPrometida(
			@PathVariable UUID id,
			@RequestBody DataPrometidaRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ValorAReceberParcelaCartao valor = valorAReceberService.registrarDataPrometida(id, request.dataPrometida(),
				contexto);
		return ResponseEntity.ok(ValorAReceberParcelaCartaoResponse.from(valor));
	}

	@GetMapping("/{id}/ressarcimentos")
	public ResponseEntity<List<RessarcimentoParcelaCartaoResponse>> listarRessarcimentos(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<RessarcimentoParcelaCartaoResponse> resposta = ressarcimentoService.listar(id, contexto).stream()
				.map(RessarcimentoParcelaCartaoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/{id}/receber-integral")
	public ResponseEntity<RessarcimentoParcelaCartaoResponse> receberIntegral(
			@PathVariable UUID id,
			@Valid @RequestBody ReceberIntegralCompraTerceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var ressarcimento = ressarcimentoService.receberIntegral(id, request.contaId(),
				request.dataRessarcimento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(RessarcimentoParcelaCartaoResponse.from(ressarcimento));
	}

	@PostMapping("/{id}/receber-parcial")
	public ResponseEntity<RessarcimentoParcelaCartaoResponse> receberParcial(
			@PathVariable UUID id,
			@Valid @RequestBody ReceberParcialCompraTerceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var ressarcimento = ressarcimentoService.receberParcial(id, request.contaId(),
				request.valor(), request.dataRessarcimento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(RessarcimentoParcelaCartaoResponse.from(ressarcimento));
	}

	@PostMapping("/{id}/ressarcimentos/{ressarcimentoId}/estornar")
	public ResponseEntity<RessarcimentoParcelaCartaoResponse> estornar(
			@PathVariable UUID id,
			@PathVariable UUID ressarcimentoId,
			@RequestBody(required = false) EstornarRessarcimentoCompraTerceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		String motivo = request == null ? null : request.motivo();
		var ressarcimento = ressarcimentoService.estornar(id, ressarcimentoId, motivo, contexto);
		return ResponseEntity.ok(RessarcimentoParcelaCartaoResponse.from(ressarcimento));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
