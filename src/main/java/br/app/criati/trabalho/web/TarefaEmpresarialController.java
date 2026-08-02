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

import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import br.app.criati.trabalho.model.TarefaEmpresarial;
import br.app.criati.trabalho.service.PaginaTarefasEmpresariais;
import br.app.criati.trabalho.service.TarefaEmpresarialService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/trabalho/tarefas")
public class TarefaEmpresarialController {

	private final TarefaEmpresarialService tarefaService;
	private final ContextoEmpresaService contextoEmpresaService;

	public TarefaEmpresarialController(TarefaEmpresarialService tarefaService,
			ContextoEmpresaService contextoEmpresaService) {
		this.tarefaService = tarefaService;
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@GetMapping
	public ResponseEntity<PaginaTarefasEmpresariaisResponse> listar(
			@RequestParam(required = false) String busca,
			@RequestParam(required = false) UUID processoId,
			@RequestParam(required = false) SituacaoTarefaTrabalho situacao,
			@RequestParam(required = false) PrioridadeTrabalho prioridade,
			@RequestParam(required = false) UUID responsavelId,
			@RequestParam(defaultValue = "false") boolean minhasTarefas,
			@RequestParam(required = false) LocalDate prazoInicio,
			@RequestParam(required = false) LocalDate prazoFim,
			@RequestParam(required = false) Boolean atrasada,
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(defaultValue = "0") int pagina,
			@RequestParam(defaultValue = "20") int tamanho,
			@RequestParam(required = false) String ordenarPor,
			@RequestParam(defaultValue = "desc") String direcao,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		PaginaTarefasEmpresariais resultado = tarefaService.listarPagina(contexto, busca, processoId, situacao,
				prioridade, responsavelId, minhasTarefas, prazoInicio, prazoFim, atrasada, status, pagina, tamanho,
				ordenarPor, direcao);
		return ResponseEntity.ok(new PaginaTarefasEmpresariaisResponse(
				resultado.itens().stream().map(TarefaEmpresarialResponse::from).toList(), resultado.pagina(),
				resultado.tamanho(), resultado.totalElementos(), resultado.totalPaginas(), resultado.ordenarPor(),
				resultado.direcao()));
	}

	@PostMapping
	public ResponseEntity<TarefaEmpresarialResponse> criar(@Valid @RequestBody TarefaEmpresarialRequest request,
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		TarefaEmpresarial tarefa = tarefaService.criar(request.processoId(), request.titulo(), request.descricao(),
				request.responsavelId(), request.prioridade(), request.prazo(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(TarefaEmpresarialResponse.from(tarefa));
	}

	@GetMapping("/{id}")
	public ResponseEntity<TarefaEmpresarialResponse> buscar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.buscar(id, contexto)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TarefaEmpresarialResponse> editar(@PathVariable UUID id,
			@Valid @RequestBody TarefaEmpresarialRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		TarefaEmpresarial tarefa = tarefaService.editar(id, request.titulo(), request.descricao(),
				request.prioridade(), request.prazo(), contexto);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefa));
	}

	@PostMapping("/{id}/responsavel")
	public ResponseEntity<TarefaEmpresarialResponse> atribuirResponsavel(@PathVariable UUID id,
			@RequestBody AtribuirResponsavelRequest request, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(
				tarefaService.atribuirResponsavel(id, request.responsavelId(), contexto)));
	}

	@PostMapping("/{id}/iniciar")
	public ResponseEntity<TarefaEmpresarialResponse> iniciar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.iniciar(id, contexto)));
	}

	@PostMapping("/{id}/concluir")
	public ResponseEntity<TarefaEmpresarialResponse> concluir(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.concluir(id, contexto)));
	}

	@PostMapping("/{id}/reabrir")
	public ResponseEntity<TarefaEmpresarialResponse> reabrir(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.reabrir(id, contexto)));
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<TarefaEmpresarialResponse> cancelar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.cancelar(id, contexto)));
	}

	@PostMapping("/{id}/inativar")
	public ResponseEntity<TarefaEmpresarialResponse> inativar(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(TarefaEmpresarialResponse.from(tarefaService.inativar(id, contexto)));
	}

	@GetMapping("/{id}/historico")
	public ResponseEntity<List<HistoricoTrabalhoResponse>> historico(@PathVariable UUID id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return ResponseEntity.ok(tarefaService.historico(id, contexto).stream()
				.map(HistoricoTrabalhoResponse::from).toList());
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoEmpresaService.exigirContextoAtivo(session, principal.getUsuario().getId());
	}
}
