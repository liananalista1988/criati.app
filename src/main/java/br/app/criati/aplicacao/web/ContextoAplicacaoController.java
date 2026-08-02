package br.app.criati.aplicacao.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.aplicacao.service.ModuloDisponibilidadeService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/contexto")
public class ContextoAplicacaoController {

	private final ModuloDisponibilidadeService moduloDisponibilidadeService;
	private final ContextoEmpresaService contextoEmpresaService;

	public ContextoAplicacaoController(
			ModuloDisponibilidadeService moduloDisponibilidadeService, ContextoEmpresaService contextoEmpresaService) {
		this.moduloDisponibilidadeService = moduloDisponibilidadeService;
		this.contextoEmpresaService = contextoEmpresaService;
	}

	@GetMapping("/aplicacoes")
	public ResponseEntity<List<AplicacaoContextoResponse>> listarAplicacoesDaEmpresaAtiva(
			HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());

		List<AplicacaoContextoResponse> resposta = moduloDisponibilidadeService.listarDisponiveis(contexto)
				.stream()
				.map(AplicacaoContextoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}
}
