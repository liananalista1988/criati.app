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

import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.EmprestimoConcedidoService;
import br.app.criati.financeiro.service.ParcelaEmprestimoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/emprestimos-concedidos")
public class EmprestimoConcedidoController {

	private final EmprestimoConcedidoService emprestimoConcedidoService;
	private final ParcelaEmprestimoService parcelaEmprestimoService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public EmprestimoConcedidoController(EmprestimoConcedidoService emprestimoConcedidoService,
			ParcelaEmprestimoService parcelaEmprestimoService, ContextoFinanceiroService contextoFinanceiroService) {
		this.emprestimoConcedidoService = emprestimoConcedidoService;
		this.parcelaEmprestimoService = parcelaEmprestimoService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping
	public ResponseEntity<List<EmprestimoConcedidoResponse>> listar(
			@RequestParam(required = false) StatusEmprestimoConcedido status,
			@RequestParam(required = false) UUID parteId,
			@RequestParam(required = false) UUID categoriaId,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		List<EmprestimoConcedidoResponse> resposta = emprestimoConcedidoService
				.listar(contexto, status, parteId, categoriaId, busca).stream()
				.map(EmprestimoConcedidoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@GetMapping("/{id}")
	public ResponseEntity<EmprestimoConcedidoResponse> buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		EmprestimoConcedido emprestimo = emprestimoConcedidoService.buscar(id, contexto);
		return ResponseEntity.ok(EmprestimoConcedidoResponse.from(emprestimo));
	}

	@PostMapping
	public ResponseEntity<EmprestimoConcedidoResponse> criar(
			@Valid @RequestBody EmprestimoConcedidoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		EmprestimoConcedido emprestimo = emprestimoConcedidoService.criar(request.parteFinanceiraId(),
				request.categoriaId(), request.descricao(), request.valorPrincipal(), request.dataConcessao(),
				request.tipoCobranca(), request.percentualJuros(), request.percentualMulta(),
				request.formaPagamento(), request.quantidadeParcelas(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(EmprestimoConcedidoResponse.from(emprestimo));
	}

	@GetMapping("/{id}/parcelas")
	public ResponseEntity<List<ParcelaEmprestimoResponse>> listarParcelas(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		emprestimoConcedidoService.buscar(id, contexto);
		List<ParcelaEmprestimoResponse> resposta = parcelaEmprestimoService.listar(contexto, id, null, null).stream()
				.map(ParcelaEmprestimoResponse::from).toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/{id}/cancelar")
	public ResponseEntity<EmprestimoConcedidoResponse> cancelar(
			@PathVariable UUID id,
			@RequestBody(required = false) CancelarEmprestimoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		String motivo = request == null ? null : request.motivo();
		EmprestimoConcedido emprestimo = emprestimoConcedidoService.cancelar(id, motivo, contexto);
		return ResponseEntity.ok(EmprestimoConcedidoResponse.from(emprestimo));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
	}
}
