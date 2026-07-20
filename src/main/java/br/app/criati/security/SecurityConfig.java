package br.app.criati.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

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
			JsonAuthenticationEntryPoint authenticationEntryPoint,
			JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/empresas")
								.hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.requestMatchers(HttpMethod.POST, "/api/usuarios")
								.hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.requestMatchers("/api/admin/**").hasAuthority(UsuarioPrincipal.ROLE_SUPERADMIN)
						.anyRequest().authenticated())
				.securityContext(context -> context.securityContextRepository(securityContextRepository))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.csrf(csrf -> csrf
						.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
						// login precisa ser alcancavel sem token CSRF previamente emitido;
						// a sessao e recriada no login, o que mitiga fixacao.
						.ignoringRequestMatchers("/api/auth/login"))
					// a API nao usa redirecionamento pos-login (login e um endpoint JSON
					// proprio), entao o RequestCache padrao so criaria sessao anonima
					// desnecessaria a cada 401 sem nenhum uso real.
					.requestCache(cache -> cache.disable());
		return http.build();
	}
}
