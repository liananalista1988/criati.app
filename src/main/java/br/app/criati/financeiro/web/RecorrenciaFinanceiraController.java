package br.app.criati.financeiro.web;

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

import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.RecorrenciaFinanceiraService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/recorrencias")
public class RecorrenciaFinanceiraController {

	private final RecorrenciaFinanceiraService recorrenciaFinanceiraService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public RecorrenciaFinanceiraController(
			RecorrenciaFinanceiraService recorrenciaFinanceiraService,
			ContextoFinanceiroService contextoFinanceiroService) {
		this.recorrenciaFinanceiraService = recorrenciaFinanceiraService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<RecorrenciaFinanceiraResponse>> listar(
			@RequestParam(required = false) StatusRecorrencia status,
			@RequestParam(required = false) TipoFinanceiro tipo,
			@RequestParam(required = false) PeriodicidadeRecorrencia periodicidade,
			@RequestParam(required = false) UUID pessoaId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<RecorrenciaFinanceiraResponse> resposta = recorrenciaFinanceiraService
				.listar(contexto, status, tipo, periodicidade, pessoaId, categoriaId, busca)
				.stream()
				.map(r -> paraResponse(r, contexto))
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<RecorrenciaFinanceiraResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		RecorrenciaFinanceira recorrencia = recorrenciaFinanceiraService.buscar(id, contexto);
		return ResponseEntity.ok(paraResponse(recorrencia, contexto));
	}

	@PostMapping
	public ResponseEntity<RecorrenciaFinanceiraResponse> criar(
			@Valid @RequestBody RecorrenciaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		RecorrenciaFinanceira recorrencia = recorrenciaFinanceiraService.criar(
				request.tipo(), request.descricao(), request.valorPadrao(), request.contaId(), request.categoriaId(),
				request.pessoaFinanceiraId(), request.parteFinanceiraId(), request.formaPagamento(),
				request.periodicidade(), request.intervalo(), request.dia(), request.mes(), request.dataInicial(),
				request.dataFinal(), request.gerarAutomaticamente(), request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(recorrencia, contexto));
	}

	@PutMapping("/{id}")
	public ResponseEntity<RecorrenciaFinanceiraResponse> editar(
			@PathVariable UUID id,
			@Valid @RequestBody RecorrenciaFinanceiraEdicaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		RecorrenciaFinanceira recorrencia = recorrenciaFinanceiraService.editar(
				id, request.descricao(), request.valorPadrao(), request.contaId(), request.categoriaId(),
				request.pessoaFinanceiraId(), request.parteFinanceiraId(), request.formaPagamento(),
				request.intervalo(), request.dia(), request.mes(), request.dataFinal(),
				request.gerarAutomaticamente(), request.observacao(), contexto);
		return ResponseEntity.ok(paraResponse(recorrencia, contexto));
	}

	@PostMapping("/{id}/pausar")
	public ResponseEntity<RecorrenciaFinanceiraResponse> pausar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(paraResponse(recorrenciaFinanceiraService.pausar(id, contexto), contexto));
	}

	@PostMapping("/{id}/retomar")
	public ResponseEntity<RecorrenciaFinanceiraResponse> retomar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(paraResponse(recorrenciaFinanceiraService.retomar(id, contexto), contexto));
	}

	@PostMapping("/{id}/encerrar")
	public ResponseEntity<RecorrenciaFinanceiraResponse> encerrar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(paraResponse(recorrenciaFinanceiraService.encerrar(id, contexto), contexto));
	}

	@PostMapping("/{id}/gerar")
	public ResponseEntity<LancamentoFinanceiroResponse> gerarOcorrencia(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(
				recorrenciaFinanceiraService.gerarOcorrencia(id, contexto)));
	}

	@PostMapping("/{id}/gerar-competencia")
	public ResponseEntity<LancamentoFinanceiroResponse> gerarCompetenciaEspecifica(
			@PathVariable UUID id,
			@Valid @RequestBody GerarCompetenciaRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(LancamentoFinanceiroResponse.from(
				recorrenciaFinanceiraService.gerarCompetenciaEspecifica(id, request.competencia(), contexto)));
	}

	@PostMapping("/gerar-automaticas")
	public ResponseEntity<GeracaoAutomaticaResponse> gerarAutomaticas(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		int geradas = recorrenciaFinanceiraService.gerarAutomaticas(contexto);
		return ResponseEntity.ok(new GeracaoAutomaticaResponse(geradas));
	}

	@GetMapping("/{id}/ocorrencias")
	public ResponseEntity<List<LancamentoFinanceiroResponse>> listarOcorrencias(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<LancamentoFinanceiroResponse> resposta = recorrenciaFinanceiraService.listarOcorrencias(id, contexto)
				.stream().map(LancamentoFinanceiroResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/resumo")
	public ResponseEntity<ResumoRecorrenciasResponse> resumir(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(ResumoRecorrenciasResponse.from(recorrenciaFinanceiraService.resumir(contexto)));
	}

	private RecorrenciaFinanceiraResponse paraResponse(RecorrenciaFinanceira recorrencia, ContextoEmpresaAtual contexto) {
		long quantidade = recorrenciaFinanceiraService.contarOcorrencias(recorrencia.getId());
		var ultimaCompetencia = recorrenciaFinanceiraService.ultimaCompetenciaGerada(recorrencia.getId(), contexto.empresaId());
		return RecorrenciaFinanceiraResponse.from(recorrencia, quantidade, ultimaCompetencia);
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}

	public record GeracaoAutomaticaResponse(int ocorrenciasGeradas) {
	}
}
