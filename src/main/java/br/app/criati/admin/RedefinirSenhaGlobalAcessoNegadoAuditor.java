package br.app.criati.admin;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import br.app.criati.security.JsonAccessDeniedHandler;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
import br.app.criati.shared.web.IpOrigemResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Decora o {@link JsonAccessDeniedHandler} generico da aplicacao para tambem
 * auditar, especificamente, tentativas HTTP de redefinicao GLOBAL de senha
 * (CRIATI-SEG-001) negadas pelo {@code SecurityConfig} por falta de
 * {@code ROLE_SUPERADMIN}. Essas requisicoes nunca chegam ao
 * controller/service - o guard redundante de
 * {@link RedefinirSenhaGlobalService#redefinirSenha} so e exercitavel
 * chamando o service diretamente (defesa em profundidade, ver
 * RedefinirSenhaGlobalServiceTests) - entao este e o unico ponto onde a
 * negacao real via HTTP pode ser capturada e auditada, sem liberar acesso ao
 * endpoint (a resposta 403 e sempre delegada, inalterada, ao
 * {@link JsonAccessDeniedHandler}) e sem duplicar o evento (o service nunca
 * chega a rodar para esta requisicao).
 *
 * <p>So audita quando o verbo/rota correspondem exatamente ao endpoint de
 * redefinicao global; qualquer outra rota de {@code /api/admin/**} negada
 * recebe apenas o 403 padrao, sem auditoria - a acao de auditoria disponivel
 * ({@link br.app.criati.shared.enums.AcaoAuditoriaSegurancaGlobal}) e
 * exclusiva dessa operacao.
 */
@Component
public class RedefinirSenhaGlobalAcessoNegadoAuditor implements AccessDeniedHandler {

	private static final Pattern REDEFINIR_SENHA_PATH =
			Pattern.compile("^/api/admin/usuarios/([^/]+)/redefinir-senha$");

	private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
	private final RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService;
	private final IpOrigemResolver ipOrigemResolver;

	public RedefinirSenhaGlobalAcessoNegadoAuditor(JsonAccessDeniedHandler jsonAccessDeniedHandler,
			RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService, IpOrigemResolver ipOrigemResolver) {
		this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
		this.auditoriaFalhaService = auditoriaFalhaService;
		this.ipOrigemResolver = ipOrigemResolver;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		auditarSeForTentativaDeRedefinicaoGlobalDeSenha(request);
		jsonAccessDeniedHandler.handle(request, response, accessDeniedException);
	}

	private void auditarSeForTentativaDeRedefinicaoGlobalDeSenha(HttpServletRequest request) {
		if (!"POST".equalsIgnoreCase(request.getMethod())) {
			return;
		}
		Matcher matcher = REDEFINIR_SENHA_PATH.matcher(request.getRequestURI());
		if (!matcher.matches()) {
			return;
		}
		UUID usuarioAlvoId = extrairUuid(matcher.group(1));
		if (usuarioAlvoId == null) {
			return;
		}
		UsuarioPrincipal chamador = extrairChamadorAutenticado();
		if (chamador == null) {
			return;
		}
		auditoriaFalhaService.registrarFalha(chamador.getUsuario(), usuarioAlvoId,
				ResultadoAuditoriaSeguranca.NEGADO, MotivoAuditoriaSeguranca.SEM_PERMISSAO,
				ipOrigemResolver.resolver(request));
	}

	private static UUID extrairUuid(String valor) {
		try {
			return UUID.fromString(valor);
		} catch (IllegalArgumentException excecaoFormatoInvalido) {
			return null;
		}
	}

	private static UsuarioPrincipal extrairChamadorAutenticado() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
			return null;
		}
		return principal;
	}
}
