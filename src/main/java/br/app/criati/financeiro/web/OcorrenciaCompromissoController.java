package br.app.criati.financeiro.web;

import java.time.LocalDate;
import java.time.YearMonth;
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

import br.app.criati.financeiro.model.OcorrenciaCompromisso;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.OcorrenciaCompromissoService;
import br.app.criati.financeiro.service.PagamentoOcorrenciaCompromissoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/ocorrencias-compromisso")
public class OcorrenciaCompromissoController {

	private final OcorrenciaCompromissoService ocorrenciaCompromissoService;
	private final PagamentoOcorrenciaCompromissoService pagamentoOcorrenciaCompromissoService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public OcorrenciaCompromissoController(OcorrenciaCompromissoService ocorrenciaCompromissoService,
			PagamentoOcorrenciaCompromissoService pagamentoOcorrenciaCompromissoService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.ocorrenciaCompromissoService = ocorrenciaCompromissoService;
		this.pagamentoOcorrenciaCompromissoService = pagamentoOcorrenciaCompromissoService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<OcorrenciaCompromissoResponse>> listar(
			@RequestParam(required = false) YearMonth competencia,
			@RequestParam(required = false) LocalDate vencimentoInicio,
			@RequestParam(required = false) LocalDate vencimentoFim,
			@RequestParam(required = false) StatusOcorrenciaCompromisso status,
			@RequestParam(required = false) Boolean vencidas,
			@RequestParam(required = false) UUID pessoaId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) UUID parteId,
			@RequestParam(required = false) UUID compromissoId,
			@RequestParam(required = false) UUID contaPrevistaId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<OcorrenciaCompromissoResponse> resposta = ocorrenciaCompromissoService
				.listar(contexto, competencia, vencimentoInicio, vencimentoFim, status, vencidas, pessoaId,
						categoriaId, parteId, compromissoId, contaPrevistaId, busca)
				.stream().map(OcorrenciaCompromissoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/calendario")
	public ResponseEntity<List<OcorrenciaCompromissoResponse>> calendario(
			@RequestParam(required = false) YearMonth mes,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<OcorrenciaCompromissoResponse> resposta = ocorrenciaCompromissoService
				.listarCalendario(contexto, mes).stream().map(OcorrenciaCompromissoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/resumo")
	public ResponseEntity<ResumoContasAPagarResponse> resumir(
			@RequestParam(required = false) LocalDate vencimentoInicio,
			@RequestParam(required = false) LocalDate vencimentoFim,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(ResumoContasAPagarResponse.from(
				ocorrenciaCompromissoService.resumir(contexto, vencimentoInicio, vencimentoFim)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OcorrenciaCompromissoResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		OcorrenciaCompromisso ocorrencia = ocorrenciaCompromissoService.buscar(id, contexto);
		return ResponseEntity.ok(OcorrenciaCompromissoResponse.from(ocorrencia));
	}

	@PostMapping
	public ResponseEntity<OcorrenciaCompromissoResponse> criar(
			@Valid @RequestBody OcorrenciaCompromissoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		OcorrenciaCompromisso ocorrencia = ocorrenciaCompromissoService.criar(request.compromissoId(),
				request.descricao(), request.competencia(), request.categoriaId(), request.pessoaFinanceiraId(),
				request.parteFinanceiraId(), request.contaPrevistaId(), request.valorPrevisto(),
				request.valorPrincipal(), request.vencimento(), request.dataRecebimentoCobranca(), request.juros(),
				request.multa(), request.desconto(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(OcorrenciaCompromissoResponse.from(ocorrencia));
	}

	@PostMapping("/gerar-por-compromisso/{compromissoId}")
	public ResponseEntity<OcorrenciaCompromissoResponse> gerarPorRecorrencia(
			@PathVariable UUID compromissoId, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		OcorrenciaCompromisso ocorrencia = ocorrenciaCompromissoService.gerarPorRecorrencia(compromissoId, contexto);
		return ResponseEntity.ok(OcorrenciaCompromissoResponse.from(ocorrencia));
	}

	@PutMapping("/{id}")
	public ResponseEntity<OcorrenciaCompromissoResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody OcorrenciaCompromissoEdicaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		OcorrenciaCompromisso ocorrencia = ocorrenciaCompromissoService.editar(id, request.descricao(),
				request.categoriaId(), request.pessoaFinanceiraId(), request.parteFinanceiraId(),
				request.contaPrevistaId(), request.valorPrevisto(), request.valorPrincipal(), request.vencimento(),
				request.dataRecebimentoCobranca(), request.juros(), request.multa(), request.desconto(),
				request.observacao(), contexto);
		return ResponseEntity.ok(OcorrenciaCompromissoResponse.from(ocorrencia));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<OcorrenciaCompromissoResponse> cancelar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(OcorrenciaCompromissoResponse.from(ocorrenciaCompromissoService.cancelar(id, contexto)));
	}

	@GetMapping("/{id}/pagamentos")
	public ResponseEntity<List<PagamentoOcorrenciaResponse>> listarPagamentos(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<PagamentoOcorrenciaResponse> resposta = pagamentoOcorrenciaCompromissoService.listar(id, contexto)
				.stream().map(PagamentoOcorrenciaResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/{id}/pagar-integral")
	public ResponseEntity<PagamentoOcorrenciaResponse> pagarIntegral(
			@PathVariable UUID id,
			@Valid @RequestBody PagarIntegralRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var pagamento = pagamentoOcorrenciaCompromissoService.pagarIntegral(id, request.contaId(),
				request.dataPagamento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoOcorrenciaResponse.from(pagamento));
	}

	@PostMapping("/{id}/pagar-parcial")
	public ResponseEntity<PagamentoOcorrenciaResponse> pagarParcial(
			@PathVariable UUID id,
			@Valid @RequestBody PagarParcialRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var pagamento = pagamentoOcorrenciaCompromissoService.pagarParcial(id, request.contaId(), request.valor(),
				request.dataPagamento(), request.formaPagamento(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(PagamentoOcorrenciaResponse.from(pagamento));
	}

	@PostMapping("/{id}/pagamentos/{pagamentoId}/estornar")
	public ResponseEntity<PagamentoOcorrenciaResponse> estornar(
			@PathVariable UUID id,
			@PathVariable UUID pagamentoId,
			@Valid @RequestBody EstornarPagamentoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		var pagamento = pagamentoOcorrenciaCompromissoService.estornar(id, pagamentoId, request.motivo(), contexto);
		return ResponseEntity.ok(PagamentoOcorrenciaResponse.from(pagamento));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
