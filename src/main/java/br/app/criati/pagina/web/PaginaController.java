package br.app.criati.pagina.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.PerfilUsuario;
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

	// Rota publica (permitAll declarado em SecurityConfig). O token nunca e
	// lido aqui - nem como @PathVariable, nem em log: a validacao completa
	// (GET /api/convites/{token}) e a aceitacao (POST .../aceitar) acontecem
	// inteiramente no navegador, contra a API publica ja existente. O
	// controller so decide qual view renderizar.
	@GetMapping("/convites/{token}")
	public String aceitarConvite() {
		return "convite/aceitar";
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

	@GetMapping("/app/financeiro/contas")
	public String financeiroContas(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-contas";
	}

	@GetMapping("/app/financeiro/categorias")
	public String financeiroCategorias(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-categorias";
	}

	@GetMapping("/app/financeiro/lancamentos")
	public String financeiroLancamentos(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-lancamentos";
	}

	@GetMapping("/app/financeiro/recorrencias")
	public String financeiroRecorrencias(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-recorrencias";
	}

	@GetMapping("/app/financeiro/pessoas")
	public String financeiroPessoas(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-pessoas";
	}

	@GetMapping("/app/financeiro/contatos")
	public String financeiroContatos(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.FINANCEIRO.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/financeiro-contatos";
	}

	@GetMapping("/app/clinica")
	public String clinica(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		if (!possuiAplicacaoAtivaNaEmpresaAtiva(session, principal, CodigoAplicacao.CLINICA.name())) {
			return "redirect:/app/aplicacoes";
		}
		return "app/clinica";
	}

	// Autorizacao real (ROLE_SUPERADMIN) e declarada no SecurityConfig para todo
	// o prefixo /app/admin/** ("/app/admin/**".hasAuthority(ROLE_SUPERADMIN)),
	// nao checada aqui - mesmo padrao ja usado por /api/admin/**.
	@GetMapping("/app/admin")
	public String adminDashboard() {
		return "app/admin-dashboard";
	}

	@GetMapping("/app/admin/empresas")
	public String adminEmpresas() {
		return "app/admin-empresas";
	}

	@GetMapping("/app/admin/empresas/nova")
	public String adminEmpresaNova() {
		return "app/admin-empresa-nova";
	}

	@GetMapping("/app/admin/empresas/{empresaId}")
	public String adminEmpresaDetalhe() {
		return "app/admin-empresa-detalhe";
	}

	@GetMapping("/app/admin/usuarios")
	public String adminUsuarios() {
		return "app/admin-usuarios";
	}

	@GetMapping("/app/admin/vinculos")
	public String adminVinculos() {
		return "app/admin-vinculos";
	}

	// ADMINISTRADOR/GESTOR/USUARIO sao perfis por empresa (UsuarioEmpresa), nao
	// authorities do Spring Security - por isso a checagem acontece aqui, nao
	// via hasAuthority(...) no SecurityConfig (que so conhece ROLE_SUPERADMIN).
	// AcessoNegadoException e tratada pelo GlobalExceptionHandler (403 JSON),
	// mesmo padrao ja usado pela API para nao revelar o motivo especifico
	// (sem contexto vs. perfil insuficiente) - inclui Superadministrador sem
	// vinculo empresarial, que ja falha em exigirContextoAtivo.
	@GetMapping("/app/usuarios")
	public String usuarios(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		exigirAdministradorNaEmpresaAtiva(session, principal);
		return "app/usuarios";
	}

	@GetMapping("/app/convites")
	public String convites(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		exigirAdministradorNaEmpresaAtiva(session, principal);
		return "app/convites";
	}

	private void exigirAdministradorNaEmpresaAtiva(HttpSession session, UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private boolean possuiAplicacaoAtivaNaEmpresaAtiva(
			HttpSession session, UsuarioPrincipal principal, String codigoAplicacao) {
		return contextoEmpresaService.obterContextoAtual(session, principal.getUsuario().getId())
				.map(ContextoEmpresaAtual::empresaId)
				.map(empresaId -> aplicacaoService.possuiAplicacaoAtiva(empresaId, codigoAplicacao))
				.orElse(false);
	}

	// Disponivel em todo template renderizado por este controller (inclusive
	// paginas publicas, onde o principal e nulo): usado pelo fragmento da
	// sidebar para mostrar a secao "Administracao da plataforma" somente para
	// quem tem ROLE_SUPERADMIN - nunca concede acesso, apenas exibe o link
	// (a autorizacao real continua inteiramente no SecurityConfig).
	@ModelAttribute("superAdministrador")
	public boolean superAdministrador(@AuthenticationPrincipal UsuarioPrincipal principal) {
		return principal != null && principal.getUsuario().isSuperAdministrador();
	}

	private boolean usuarioAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
