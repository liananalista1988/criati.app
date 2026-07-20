package br.app.criati.pagina.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

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

	private boolean usuarioAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
