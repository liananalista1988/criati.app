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

import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.service.CartaoCreditoService;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/contexto/financeiro/cartoes")
public class CartaoCreditoController {

	private final CartaoCreditoService cartaoService;
	private final ContextoFinanceiroService contextoService;

	public CartaoCreditoController(CartaoCreditoService cartaoService, ContextoFinanceiroService contextoService) {
		this.cartaoService = cartaoService;
		this.contextoService = contextoService;
	}

	@GetMapping
	public List<CartaoCreditoResponse> listar(
			@RequestParam(required = false) StatusCadastro status,
			@RequestParam(required = false) TipoCartao tipo,
			@RequestParam(required = false) UUID titularId,
			@RequestParam(required = false) UUID instituicaoId,
			@RequestParam(required = false) Boolean bloqueado,
			@RequestParam(required = false) String busca,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return cartaoService.listar(contexto, status, tipo, titularId, instituicaoId, bloqueado, busca).stream()
				.map(this::paraResponse).toList();
	}

	@GetMapping("/resumo")
	public ResumoCartoesCreditoResponse resumir(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return ResumoCartoesCreditoResponse.from(cartaoService.resumir(exigirAcesso(session, principal)));
	}

	@GetMapping("/{id}/virtuais")
	public List<CartaoCreditoResponse> listarVirtuais(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return cartaoService.listarVirtuais(id, contexto).stream().map(this::paraResponse).toList();
	}

	@GetMapping("/{id}")
	public CartaoCreditoResponse buscar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(cartaoService.buscar(id, exigirAcesso(session, principal)));
	}

	@PostMapping
	public ResponseEntity<CartaoCreditoResponse> criar(
			@Valid @RequestBody CartaoCreditoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		CartaoCredito cartao = cartaoService.criar(request.nome(), request.titularId(), request.instituicaoId(),
				request.tipo(), request.cartaoPrincipalId(), request.bandeira(), request.ultimosQuatroDigitos(),
				request.limiteTotal(), request.limiteSaudavel(), request.diaFechamento(), request.diaVencimento(),
				request.observacao(), contexto);
		return ResponseEntity.status(HttpStatus.CREATED).body(paraResponse(cartao));
	}

	@PutMapping("/{id}")
	public CartaoCreditoResponse editar(
			@PathVariable UUID id,
			@Valid @RequestBody CartaoCreditoEdicaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = exigirAcesso(session, principal);
		return paraResponse(cartaoService.editar(id, request.nome(), request.titularId(), request.instituicaoId(),
				request.bandeira(), request.ultimosQuatroDigitos(), request.limiteTotal(), request.limiteSaudavel(),
				request.diaFechamento(), request.diaVencimento(), request.observacao(), contexto));
	}

	@PostMapping("/{id}/inativar")
	public CartaoCreditoResponse inativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(cartaoService.inativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/reativar")
	public CartaoCreditoResponse reativar(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(cartaoService.reativar(id, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/bloquear")
	public CartaoCreditoResponse bloquear(
			@PathVariable UUID id,
			@RequestBody(required = false) BloquearCartaoRequest request,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		String motivo = request == null ? null : request.motivo();
		return paraResponse(cartaoService.bloquear(id, motivo, exigirAcesso(session, principal)));
	}

	@PostMapping("/{id}/desbloquear")
	public CartaoCreditoResponse desbloquear(
			@PathVariable UUID id, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		return paraResponse(cartaoService.desbloquear(id, exigirAcesso(session, principal)));
	}

	private ContextoEmpresaAtual exigirAcesso(HttpSession session, UsuarioPrincipal principal) {
		return contextoService.exigirAcesso(session, principal.getUsuario().getId());
	}

	private CartaoCreditoResponse paraResponse(CartaoCredito cartao) {
		long quantidadeVirtuais = cartao.ehPrincipal() ? cartaoService.contarVirtuais(cartao.getId()) : 0;
		return CartaoCreditoResponse.from(cartao, quantidadeVirtuais, cartaoService.possuiPossivelDuplicidade(cartao),
				cartaoService.limiteComprometido(cartao));
	}
}
