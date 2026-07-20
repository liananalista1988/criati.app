package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaControllerTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void loginRetorna200ParaAnonimoComElementosEsperados() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn();

		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo)
				.contains("Criati")
				.contains("Transformando processos em resultados")
				.contains("id=\"email\"")
				.contains("id=\"senha\"")
				.contains("autocomplete=\"username\"")
				.contains("autocomplete=\"current-password\"")
				.contains("name=\"_csrf\"")
				.contains("criati-login-submit");
	}

	@Test
	void loginNaoExpoeSenhaOuHash() throws Exception {
		criarUsuarioAtivo("pagina.login@criati.test");

		MvcResult resultado = mockMvc.perform(get("/login")).andReturn();
		String corpo = resultado.getResponse().getContentAsString();

		assertThat(corpo).doesNotContain(SENHA).doesNotContain("$2a$").doesNotContain("$2b$");
	}

	@Test
	void loginAutenticadoRedirecionaParaDashboard() throws Exception {
		criarUsuarioAtivo("pagina.autenticado@criati.test");
		MockHttpSession sessao = autenticar("pagina.autenticado@criati.test");

		mockMvc.perform(get("/login").session(sessao))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/dashboard"));
	}

	@Test
	void appRedirecionaParaDashboardQuandoAutenticado() throws Exception {
		criarUsuarioAtivo("pagina.app@criati.test");
		MockHttpSession sessao = autenticar("pagina.app@criati.test");

		mockMvc.perform(get("/app").session(sessao))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/dashboard"));
	}

	@Test
	void dashboardRetorna200ParaAutenticadoComElementosDoLayout() throws Exception {
		criarUsuarioAtivo("pagina.dashboard@criati.test");
		MockHttpSession sessao = autenticar("pagina.dashboard@criati.test");

		MvcResult resultado = mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn();

		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo)
				.contains("criati-sidebar")
				.contains("Dashboard")
				.contains("Usuarios")
				.contains("Convites")
				.contains("criati-topbar")
				.contains("criati-logout-btn")
				.contains("name=\"_csrf\"")
				.contains("criati-card-empresa");
	}

	@Test
	void dashboardNaoExpoeSenhaOuHash() throws Exception {
		criarUsuarioAtivo("pagina.dashboard.seguro@criati.test");
		MockHttpSession sessao = autenticar("pagina.dashboard.seguro@criati.test");

		MvcResult resultado = mockMvc.perform(get("/app/dashboard").session(sessao)).andReturn();
		String corpo = resultado.getResponse().getContentAsString();

		assertThat(corpo).doesNotContain(SENHA).doesNotContain("$2a$").doesNotContain("$2b$");
	}

	private MockHttpSession autenticar(String email) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "senha": "%s"
						}
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	private Usuario criarUsuarioAtivo(String email) {
		Usuario usuario = new Usuario("Usuario Pagina", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}
}
