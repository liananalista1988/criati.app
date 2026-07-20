package br.app.criati.aplicacao.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/contexto")
public class ContextoAplicacaoController {

	private final AplicacaoService aplicacaoService;
	private final ContextoEmpresaService contextoEmpresaService;

	public ContextoAplicacaoController(
			AplicacaoService aplicacaoService, ContextoEmpresaService contextoEmpresaService) {
		this.aplicacaoService = aplicacaoService;
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@GetMapping("/aplicacoes")
	public ResponseEntity<List<AplicacaoContextoResponse>> listarAplicacoesDaEmpresaAtiva(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());

		List<AplicacaoContextoResponse> resposta = aplicacaoService.listarAtivasDaEmpresa(contexto.empresaId())
				.stream()
				.map(AplicacaoContextoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}
}
