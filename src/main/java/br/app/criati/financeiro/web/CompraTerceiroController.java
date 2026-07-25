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

import br.app.criati.financeiro.service.CompraTerceiroService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/compras-terceiros")
public class CompraTerceiroController {

	private final CompraTerceiroService service;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public CompraTerceiroController(CompraTerceiroService service, ContextoFinanceiroService contextoFinanceiroService) {
		this.service = service;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<CompraCartaoResponse>> listar(
			@RequestParam(required = false) UUID parteId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<CompraCartaoResponse> resposta = service.listar(contexto, parteId, busca).stream()
				.map(CompraCartaoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<CompraTerceiroResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(CompraTerceiroResponse.from(service.buscar(id, contexto)));
	}

	@PostMapping
	public ResponseEntity<ResultadoCompraTerceiroResponse> registrar(
			@Valid @RequestBody CompraTerceiroRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var resultado = service.registrar(request.cartaoId(), request.pessoaResponsavelId(), request.categoriaId(),
				request.parteFinanceiraId(), request.descricao(), request.dataCompra(), request.valorTotal(),
				request.quantidadeParcelas(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(ResultadoCompraTerceiroResponse.from(resultado));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<List<ValorAReceberParcelaCartaoResponse>> cancelar(
			@PathVariable UUID id,
			@Valid @RequestBody MotivoCompraCartaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<ValorAReceberParcelaCartaoResponse> resposta = service.cancelar(id, request.motivo(), contexto).stream()
				.map(ValorAReceberParcelaCartaoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
