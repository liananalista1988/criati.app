package br.app.criati.financeiro.shared.web;

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

import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.service.ParteFinanceiraService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/contatos")
public class ParteFinanceiraController {

	private final ParteFinanceiraService parteService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public ParteFinanceiraController(
			ParteFinanceiraService parteService, ContextoFinanceiroService contextoFinanceiroService) {
		this.parteService = parteService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public List<ParteFinanceiraResponse> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) TipoParteFinanceira tipo,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return parteService.listar(exigirAcesso(session, principal), status, tipo, busca).stream()
				.map(ParteFinanceiraResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ParteFinanceiraResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ParteFinanceiraResponse.from(parteService.buscar(id, exigirAcesso(session, principal)));
	}

	@PostMapping
	public ResponseEntity<ParteFinanceiraResponse> criar(
			@Valid @RequestBody ParteFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ParteFinanceira parte = parteService.criar(
				request.nome(), request.tipo(), request.documento(), request.apelido(), request.observacao(),
				exigirAcesso(session, principal));
		return ResponseEntity.status(HttpStatus.CREATED).body(ParteFinanceiraResponse.from(parte));
	}

	@PutMapping("/{id}")
	public ParteFinanceiraResponse atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody ParteFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return ParteFinanceiraResponse.from(parteService.atualizar(
				id, request.nome(), request.tipo(), request.documento(), request.apelido(), request.observacao(),
				exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/inativar")
	public ParteFinanceiraResponse desativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ParteFinanceiraResponse.from(parteService.desativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/reativar")
	public ParteFinanceiraResponse reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ParteFinanceiraResponse.from(parteService.reativar(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
