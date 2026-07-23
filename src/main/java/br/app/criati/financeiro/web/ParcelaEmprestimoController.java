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

import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.ParcelaEmprestimoService;
import br.app.criati.financeiro.service.RecebimentoParcelaEmprestimoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/parcelas-emprestimo")
public class ParcelaEmprestimoController {

	private final ParcelaEmprestimoService parcelaEmprestimoService;
	private final RecebimentoParcelaEmprestimoService recebimentoParcelaEmprestimoService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public ParcelaEmprestimoController(ParcelaEmprestimoService parcelaEmprestimoService,
			RecebimentoParcelaEmprestimoService recebimentoParcelaEmprestimoService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.parcelaEmprestimoService = parcelaEmprestimoService;
		this.recebimentoParcelaEmprestimoService = recebimentoParcelaEmprestimoService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<ParcelaEmprestimoResponse>> listar(
			@RequestParam(required = false) UUID emprestimoId,
			@RequestParam(required = false) StatusParcelaEmprestimo status,
			@RequestParam(required = false) Boolean atrasadas,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ParcelaEmprestimoResponse> resposta = parcelaEmprestimoService
				.listar(contexto, emprestimoId, status, atrasadas).stream().map(ParcelaEmprestimoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/vencidas")
	public ResponseEntity<List<ParcelaEmprestimoResponse>> listarVencidas(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ParcelaEmprestimoResponse> resposta = parcelaEmprestimoService.listarVencidas(contexto).stream()
				.map(ParcelaEmprestimoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/proximas-vencimento")
	public ResponseEntity<List<ParcelaEmprestimoResponse>> listarProximasDoVencimento(
			@RequestParam(required = false) Integer dias,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ParcelaEmprestimoResponse> resposta = parcelaEmprestimoService.listarProximasDoVencimento(contexto, dias)
				.stream().map(ParcelaEmprestimoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/resumo")
	public ResponseEntity<ResumoEmprestimosConcedidosResponse> resumir(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(ResumoEmprestimosConcedidosResponse.from(parcelaEmprestimoService.resumir(contexto)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ParcelaEmprestimoResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ParcelaEmprestimo parcela = parcelaEmprestimoService.buscar(id, contexto);
		return ResponseEntity.ok(ParcelaEmprestimoResponse.from(parcela));
	}

	@PutMapping("/{id}/data-prometida")
	public ResponseEntity<ParcelaEmprestimoResponse> registrarDataPrometida(
			@PathVariable UUID id,
			@RequestBody DataPrometidaRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ParcelaEmprestimo parcela = parcelaEmprestimoService.registrarDataPrometida(id, request.dataPrometida(),
				contexto);
		return ResponseEntity.ok(ParcelaEmprestimoResponse.from(parcela));
	}

	@GetMapping("/{id}/recebimentos")
	public ResponseEntity<List<RecebimentoParcelaEmprestimoResponse>> listarRecebimentos(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<RecebimentoParcelaEmprestimoResponse> resposta = recebimentoParcelaEmprestimoService.listar(id, contexto)
				.stream().map(RecebimentoParcelaEmprestimoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/{id}/receber-integral")
	public ResponseEntity<RecebimentoParcelaEmprestimoResponse> receberIntegral(
			@PathVariable UUID id,
			@Valid @RequestBody ReceberIntegralRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var recebimento = recebimentoParcelaEmprestimoService.receberIntegral(id, request.contaId(),
				request.dataRecebimento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(RecebimentoParcelaEmprestimoResponse.from(recebimento));
	}

	@PostMapping("/{id}/receber-parcial")
	public ResponseEntity<RecebimentoParcelaEmprestimoResponse> receberParcial(
			@PathVariable UUID id,
			@Valid @RequestBody ReceberParcialRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var recebimento = recebimentoParcelaEmprestimoService.receberParcial(id, request.contaId(), request.valor(),
				request.dataRecebimento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(RecebimentoParcelaEmprestimoResponse.from(recebimento));
	}

	@PostMapping("/{id}/recebimentos/{recebimentoId}/estornar")
	public ResponseEntity<RecebimentoParcelaEmprestimoResponse> estornar(
			@PathVariable UUID id,
			@PathVariable UUID recebimentoId,
			@RequestBody(required = false) EstornarRecebimentoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		String motivo = request == null ? null : request.motivo();
		var recebimento = recebimentoParcelaEmprestimoService.estornar(id, recebimentoId, motivo, contexto);
		return ResponseEntity.ok(RecebimentoParcelaEmprestimoResponse.from(recebimento));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
