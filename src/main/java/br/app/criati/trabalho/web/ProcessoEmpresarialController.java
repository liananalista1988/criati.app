package br.app.criati.trabalho.web;

import java.time.LocalDate;
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

import br.app.criati.aplicacao.service.ModuloDisponibilidadeService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoProcessoTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import br.app.criati.trabalho.model.ProcessoEmpresarial;
import br.app.criati.trabalho.service.PaginaProcessosEmpresariais;
import br.app.criati.trabalho.service.ProcessoEmpresarialService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/trabalho/processos")
public class ProcessoEmpresarialController {

	private final ProcessoEmpresarialService processoService;
	private final ContextoEmpresaService contextoEmpresaService;
	private final ModuloDisponibilidadeService moduloDisponibilidadeService;

	public ProcessoEmpresarialController(ProcessoEmpresarialService processoService,
			ContextoEmpresaService contextoEmpresaService,
			ModuloDisponibilidadeService moduloDisponibilidadeService) {
		this.processoService = processoService;
		this.contextoEmpresaService = contextoEmpresaService;
		this.moduloDisponibilidadeService = moduloDisponibilidadeService;
	}

	@GetMapping
	public ResponseEntity<PaginaProcessosEmpresariaisResponse> listar(
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) SituacaoProcessoTrabalho situacao,
			@RequestParam(required = false) PrioridadeTrabalho prioridade,
			@RequestParam(required = false) UUID responsavelId,
			@RequestParam(required = false) LocalDate prazoInicio,
			@RequestParam(required = false) LocalDate prazoFim,
			@RequestParam(required = false) Boolean atrasado,
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanho,
			@RequestParam(required = false) String ordenarPor,
			@RequestParam(defaultValue = "desc") String direcao,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		PaginaProcessosEmpresariais resultado = processoService.listarPagina(contexto, busca, situacao, prioridade,
				responsavelId, prazoInicio, prazoFim, atrasado, status, pagina, tamanho, ordenarPor, direcao);
		return ResponseEntity.ok(new PaginaProcessosEmpresariaisResponse(
				resultado.itens().stream().map(this::resposta).toList(), resultado.pagina(), resultado.tamanho(),
				resultado.totalElementos(), resultado.totalPaginas(), resultado.ordenarPor(), resultado.direcao()));
	}

	@PostMapping
	public ResponseEntity<ProcessoEmpresarialResponse> criar(@Valid @RequestBody ProcessoEmpresarialRequest request,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ProcessoEmpresarial processo = processoService.criar(request.titulo(), request.descricao(),
				request.responsavelId(), request.prioridade(), request.prazo(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(resposta(processo));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProcessoEmpresarialResponse> buscar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.buscar(id, contexto)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProcessoEmpresarialResponse> editar(@PathVariable UUID id,
			@Valid @RequestBody ProcessoEmpresarialRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ProcessoEmpresarial processo = processoService.editar(id, request.titulo(), request.descricao(),
				request.prioridade(), request.prazo(), contexto);
		return ResponseEntity.ok(resposta(processo));
	}

	@PostMapping("/{id}/responsavel")
	public ResponseEntity<ProcessoEmpresarialResponse> atribuirResponsavel(@PathVariable UUID id,
			@RequestBody AtribuirResponsavelRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.atribuirResponsavel(id, request.responsavelId(), contexto)));
	}

	@PostMapping("/{id}/iniciar")
	public ResponseEntity<ProcessoEmpresarialResponse> iniciar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.iniciar(id, contexto)));
	}

	@PostMapping("/{id}/concluir")
	public ResponseEntity<ProcessoEmpresarialResponse> concluir(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.concluir(id, contexto)));
	}

	@PostMapping("/{id}/reabrir")
	public ResponseEntity<ProcessoEmpresarialResponse> reabrir(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.reabrir(id, contexto)));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<ProcessoEmpresarialResponse> cancelar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.cancelar(id, contexto)));
	}

	@PostMapping("/{id}/inativar")
	public ResponseEntity<ProcessoEmpresarialResponse> inativar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(resposta(processoService.inativar(id, contexto)));
	}

	@GetMapping("/{id}/historico")
	public ResponseEntity<List<HistoricoTrabalhoResponse>> historico(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(processoService.historico(id, contexto).stream()
				.map(HistoricoTrabalhoResponse::from).toList());
	}

	private ProcessoEmpresarialResponse resposta(ProcessoEmpresarial processo) {
		return ProcessoEmpresarialResponse.from(processo, processoService.contarTarefas(processo));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		moduloDisponibilidadeService.exigirVisualizacao(CodigoAplicacao.TAREFAS_PROCESSOS, contexto);
		return contexto;
	}
}
