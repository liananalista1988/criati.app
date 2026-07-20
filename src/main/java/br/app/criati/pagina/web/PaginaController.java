package br.app.criati.pagina.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

@Controller
public class PaginaController {

	private final ContextoEmpresaService contextoEmpresaService;
	private final AplicacaoService aplicacaoService;

	public PaginaController(ContextoEmpresaService contextoEmpresaService, AplicacaoService aplicacaoService) {
		this.contextoEmpresaService = contextoEmpresaService;
		this.aplicacaoService = aplicacaoService;
	}

	@GetMapping("/login")
	public String login() {
		if (usuarioAutenticado()) {
			return "redirect:/app/dashboard";
		}
		return "login";
	}

	@GetMapping("/app")
	public String app() {
		// Alcancavel apenas por usuario autenticado (anyRequest().authenticated()
		// no SecurityConfig); anonimo e redirecionado para /login antes de chegar aqui.
		return "redirect:/app/dashboard";
	}

	@GetMapping("/app/dashboard")
	public String dashboard() {
		return "app/dashboard";
	}

	@GetMapping("/app/aplicacoes")
	public String aplicacoes() {
		return "app/aplicacoes";
	}

	// Contexto ausente ou aplicacao nao habilitada recebem exatamente o mesmo
	// redirecionamento generico: nunca revelar ao usuario qual das duas
	// condicoes falhou, mesmo padrao ja usado para os 403 genericos da API
	// (ver ContextoEmpresaService/AcessoNegadoException).
	@GetMapping("/app/financeiro")
	public String financeiro(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro";
	}

	@GetMapping("/app/clinica")
	public String clinica(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.CLINICA.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/clinica";
	}

	@GetMapping("/app/admin/empresas")
	public String adminEmpresas() {
		// Autorizacao real (ROLE_SUPERADMIN) e declarada no SecurityConfig, nao
		// checada aqui - mesmo padrao ja usado por /api/admin/**.
		return "app/admin-empresas";
	}

	private boolean possuiAplicacaoAtivaNaEmpresaAtiva(
			HttpSession session, UsuarioPrincipal principal, String codigoAplicacao) {
		return contextoEmpresaService.obterContextoAtual(session, principal.getUsuario().getId())
				.map(ContextoEmpresaAtual::empresaId)
				.map(empresaId -> aplicacaoService.possuiAplicacaoAtiva(empresaId, codigoAplicacao))
				.orElse(false);
	}

	private boolean usuarioAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
