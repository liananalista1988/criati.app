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

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.service.ContaFinanceiraService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.InstituicaoFinanceiraService;
import br.app.criati.financeiro.service.SaldoFinanceiroService;
import br.app.criati.financeiro.shared.service.PessoaFinanceiraService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/contas")
public class ContaFinanceiraController {

	private final ContaFinanceiraService contaService;
	private final InstituicaoFinanceiraService instituicaoService;
	private final PessoaFinanceiraService pessoaService;
	private final SaldoFinanceiroService saldoService;
	private final ContextoFinanceiroService contextoService;

	public ContaFinanceiraController(
			ContaFinanceiraService contaService,
			InstituicaoFinanceiraService instituicaoService,
			PessoaFinanceiraService pessoaService,
			SaldoFinanceiroService saldoService,
			ContextoFinanceiroService contextoService) {
		this.contaService = contaService;
		this.instituicaoService = instituicaoService;
		this.pessoaService = pessoaService;
		this.saldoService = saldoService;
		this.contextoService = contextoService;
	}

	@GetMapping
	public List<ContaFinanceiraResponse> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) TipoContaFinanceira tipo,
			@RequestParam(required = false) UUID titularId,
			@RequestParam(required = false) UUID instituicaoId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return contaService.listar(contexto, status, tipo, titularId, instituicaoId, busca).stream()
				.map(this::paraResponse).toList();
	}

	@GetMapping("/resumo")
	public ResumoContasFinanceirasResponse resumir(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResumoContasFinanceirasResponse.from(contaService.resumir(exigirAcesso(session, principal)));
	}

	@GetMapping("/titulares")
	public List<TitularContaResponse> listarTitulares(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return pessoaService.listar(exigirAcesso(session, principal), StatusCadastro.ATIVO).stream()
				.map(TitularContaResponse::from).toList();
	}

	@GetMapping("/instituicoes")
	public List<InstituicaoFinanceiraResponse> listarInstituicoes(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return instituicaoService.listar(exigirAcesso(session, principal)).stream()
				.map(InstituicaoFinanceiraResponse::from).toList();
	}

	@PostMapping("/instituicoes")
	public ResponseEntity<InstituicaoFinanceiraResponse> criarInstituicao(
			@Valid @RequestBody InstituicaoFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		InstituicaoFinanceira instituicao = instituicaoService.criarLocal(
				request.nome(), request.codigo(), exigirAcesso(session, principal));
		return ResponseEntity.status(HttpStatus.CREATED).body(InstituicaoFinanceiraResponse.from(instituicao));
	}

	@PostMapping
	public ResponseEntity<ContaFinanceiraResponse> criar(
			@Valid @RequestBody ContaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		ContaFinanceira conta = contaService.criar(
				request.nome(), request.titularId(), request.instituicaoId(), request.tipo(), request.moeda(),
				request.saldoInicial(), request.dataSaldoInicial(), request.permiteConciliacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(conta));
	}

	@GetMapping("/{id}")
	public ContaFinanceiraResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(contaService.buscar(id, exigirAcesso(session, principal)));
	}

	@PutMapping("/{id}")
	public ContaFinanceiraResponse editar(
			@PathVariable UUID id,
			@Valid @RequestBody ContaFinanceiraRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return paraResponse(contaService.editar(
				id, request.nome(), request.titularId(), request.instituicaoId(), request.tipo(), request.moeda(),
				request.saldoInicial(), request.dataSaldoInicial(), request.permiteConciliacao(), contexto));
	}

	@PostMapping("/{id}/inativar")
	public ContaFinanceiraResponse inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(contaService.inativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/reativar")
	public ContaFinanceiraResponse reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(contaService.reativar(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoService.exigirAcesso(session, principal.getUsuario().getId());
	}

	private ContaFinanceiraResponse paraResponse(ContaFinanceira conta) {
		return ContaFinanceiraResponse.from(
				conta, saldoService.calcularSaldoAtual(conta), contaService.possuiPossivelDuplicidade(conta));
	}
}
