package br.app.criati.security;

import java.util.LinkedHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import br.app.criati.admin.RedefinirSenhaGlobalAcessoNegadoAuditor;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Argon2id exigiria a dependencia externa BouncyCastle, ausente do projeto
		// (NoClassDefFoundError comprovado em execucao); BCrypt e o fallback
		// documentado em docs/SEGURANCA.MD para essa incompatibilidade tecnica.
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityContextRepository securityContextRepository,
			JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
			RedefinirSenhaGlobalAcessoNegadoAuditor accessDeniedHandler) throws Exception {
		// A API (/api/**) sempre responde 401 em JSON (JsonAuthenticationEntryPoint).
		// Paginas Thymeleaf (ex.: /app/**) precisam de um 401 "navegavel": redirecionar
		// para /login, em vez de devolver um corpo JSON no navegador. As duas rotas
		// continuam usando o SecurityContextRepository/SessionAuthenticationStrategy
		// existentes; nada muda no fluxo de autenticacao ja coberto pelos testes atuais.
		LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPointsPorRota = new LinkedHashMap<>();
		entryPointsPorRota.put(PathPatternRequestMatcher.withDefaults().matcher("/api/**"), jsonAuthenticationEntryPoint);
		DelegatingAuthenticationEntryPoint authenticationEntryPoint =
				new DelegatingAuthenticationEntryPoint(entryPointsPorRota);
		authenticationEntryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/login", "/css/**", "/js/**", "/img/**", "/error")
								.permitAll()
						// Pagina publica de aceite de convite: so renderiza a view (nenhum
						// dado do convite e lido no controller); a validacao acontece
						// inteiramente no navegador, chamando a API publica ja liberada
						// abaixo (GET /api/convites/{token}, POST .../aceitar).
						.requestMatchers(HttpMethod.GET, "/convites/*").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/convites/*").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/convites/*/aceitar").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/empresas")
								.hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.requestMatchers(HttpMethod.POST, "/api/usuarios")
								.hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.requestMatchers("/api/admin/**").hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.requestMatchers(HttpMethod.GET, "/app/admin/**")
								.hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.anyRequest().authenticated())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						// login e aceitacao de convite precisam ser alcancaveis sem token
						// CSRF previamente emitido: em ambos os casos o cliente ainda nao
						// tem sessao/cookie desta aplicacao antes da chamada. O login
						// recria a sessao apos autenticar (mitiga fixacao); aceitar convite
						// nao autentica automaticamente, entao nao ha sessao a fixar.
						.ignoringRequestMatchers("/api/auth/login", "/api/convites/*/aceitar"))
					// a API nao usa redirecionamento pos-login (login e um endpoint JSON
					// proprio), entao o RequestCache padrao so criaria sessao anonima
					// desnecessaria a cada 401 sem nenhum uso real. Paginas tambem nao
					// precisam de replay pos-login nesta fase.
					.requestCache(cache -> cache.disable());
		return http.build();
	}
}
