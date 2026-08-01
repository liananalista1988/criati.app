package br.app.criati.financeiro.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.ConfirmacaoImportacaoBancariaService;
import br.app.criati.financeiro.service.ImportacaoBancariaService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/contexto/financeiro/importacoes-bancarias")
public class ImportacaoBancariaController {

	private final ImportacaoBancariaService service;
	private final ContextoFinanceiroService contextoService;
	private final ConfirmacaoImportacaoBancariaService confirmacaoService;

	public ImportacaoBancariaController(
			ImportacaoBancariaService service, ContextoFinanceiroService contextoService,
			ConfirmacaoImportacaoBancariaService confirmacaoService) {
		this.service = service;
		this.contextoService = contextoService;
		this.confirmacaoService = confirmacaoService;
	}

	@PostMapping(value = "/ofx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PreviaImportacaoBancariaResponse> importarOfx(
			@RequestParam UUID contaId,
			@RequestPart("arquivo") MultipartFile arquivo,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		PreviaImportacaoBancariaResponse resposta = PreviaImportacaoBancariaResponse.from(
				service.importar(contaId, arquivo, exigirAcesso(session, principal)));
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PreviaImportacaoBancariaResponse> importarCsv(
			@RequestParam UUID contaId,
			@RequestPart("arquivo") MultipartFile arquivo,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		PreviaImportacaoBancariaResponse resposta = PreviaImportacaoBancariaResponse.from(
				service.importarCsv(contaId, arquivo, exigirAcesso(session, principal)));
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@PostMapping(value = "/xlsx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<PreviaImportacaoBancariaResponse> importarXlsx(
			@RequestParam UUID contaId,
			@RequestPart("arquivo") MultipartFile arquivo,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		PreviaImportacaoBancariaResponse resposta = PreviaImportacaoBancariaResponse.from(
				service.importarXlsx(contaId, arquivo, exigirAcesso(session, principal)));
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
	}

	@GetMapping
	public List<LoteImportacaoBancariaResponse> listar(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return service.listar(exigirAcesso(session, principal)).stream()
				.map(LoteImportacaoBancariaResponse::from).toList();
	}

	@GetMapping("/{id}")
	public PreviaImportacaoBancariaResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return PreviaImportacaoBancariaResponse.from(service.buscar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/descartar")
	public PreviaImportacaoBancariaResponse descartar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return PreviaImportacaoBancariaResponse.from(service.descartar(id, exigirAcesso(session, principal)));
	}

	@GetMapping("/{id}/pendencias")
	public List<TransacaoBancariaImportadaResponse> listarPendencias(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return confirmacaoService.listarPendentes(id, exigirAcesso(session, principal)).stream()
				.map(TransacaoBancariaImportadaResponse::from).toList();
	}

	@PostMapping("/{id}/confirmacoes")
	public ResultadoConfirmacaoImportacaoResponse confirmar(
			@PathVariable UUID id, @jakarta.validation.Valid @RequestBody ConfirmarTransacoesImportadasRequest request,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResultadoConfirmacaoImportacaoResponse.from(confirmacaoService.confirmar(id,
				request.transacoes().stream().map(ConfirmacaoTransacaoImportadaRequest::toCommand).toList(),
				exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/transacoes/{transacaoId}/ignorar")
	public ResultadoConfirmacaoImportacaoResponse ignorar(
			@PathVariable UUID id, @PathVariable UUID transacaoId,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResultadoConfirmacaoImportacaoResponse.from(
				confirmacaoService.ignorar(id, transacaoId, exigirAcesso(session, principal)));
	}

	@GetMapping("/{id}/resumo")
	public ResumoImportacaoBancariaResponse resumir(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResumoImportacaoBancariaResponse.from(
				confirmacaoService.resumir(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
