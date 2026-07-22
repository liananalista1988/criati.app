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
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.service.PessoaFinanceiraService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/pessoas")
public class PessoaFinanceiraController {

	private final PessoaFinanceiraService pessoaService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public PessoaFinanceiraController(
			PessoaFinanceiraService pessoaService, ContextoFinanceiroService contextoFinanceiroService) {
		this.pessoaService = pessoaService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public List<PessoaFinanceiraResponse> listar(
			@RequestParam(required = false) StatusCadastro status,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return pessoaService.listar(exigirAcesso(session, principal), status).stream()
				.map(PessoaFinanceiraResponse::from).toList();
	}

	@GetMapping("/usuarios-vinculaveis")
	public List<UsuarioVinculavelResponse> listarUsuariosVinculaveis(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return pessoaService.listarUsuariosVinculaveis(exigirAcesso(session, principal)).stream()
				.map(UsuarioVinculavelResponse::from).toList();
	}

	@GetMapping("/{id}")
	public PessoaFinanceiraResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return PessoaFinanceiraResponse.from(pessoaService.buscar(id, exigirAcesso(session, principal)));
	}

	@PostMapping
	public ResponseEntity<PessoaFinanceiraResponse> criar(
			@Valid @RequestBody PessoaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		PessoaFinanceira pessoa = pessoaService.criar(
				request.nome(), request.apelido(), request.usuarioId(), exigirAcesso(session, principal));
		return ResponseEntity.status(HttpStatus.CREATED).body(PessoaFinanceiraResponse.from(pessoa));
	}

	@PutMapping("/{id}")
	public PessoaFinanceiraResponse atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody PessoaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		return PessoaFinanceiraResponse.from(pessoaService.atualizar(
				id, request.nome(), request.apelido(), request.usuarioId(), exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/inativar")
	public PessoaFinanceiraResponse desativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return PessoaFinanceiraResponse.from(pessoaService.desativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/reativar")
	public PessoaFinanceiraResponse reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return PessoaFinanceiraResponse.from(pessoaService.reativar(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
