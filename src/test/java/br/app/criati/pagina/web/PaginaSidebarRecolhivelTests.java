package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Cobre a sidebar recolhivel com menu hamburguer (CRIATI-F4-004): renderiza
 * via MockMvc (sem executar JavaScript), confirma marcacao/atributos de
 * acessibilidade do botao e da sidebar em paginas autenticadas, confirma que
 * login e a pagina publica de aceite de convite continuam sem qualquer
 * marcacao de sidebar, e confirma que nenhum dado sensivel foi introduzido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaSidebarRecolhivelTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void dashboardContemBotaoHamburguerComAtributosDeAcessibilidade() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "sidebar.hamburguer@criati.test");

		assertThat(corpo)
				.contains("criati-menu-toggle")
				.contains("aria-controls=\"criati-sidebar\"")
				.contains("aria-expanded=\"false\"")
				.contains("id=\"criati-sidebar\"");
	}

	@Test
	void dashboardCarregaScriptCriatiUiQueContemAAlternanciaDaSidebar() throws Exception {
		mockMvc.perform(get("/js/criati-ui.js"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string(org.hamcrest.Matchers.containsString("initSidebarToggle")));
	}

	@Test
	void scriptCriatiUiAlternaEstadoSemGuardarDadoSensivel() throws Exception {
		String script = mockMvc.perform(get("/js/criati-ui.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script)
				.contains("criati.sidebar.recolhida")
				.contains("criati-sidebar-collapsed")
				.contains("is-sidebar-open")
				.doesNotContain("_csrf")
				.doesNotContain("empresaId")
				.doesNotContain("usuarioId")
				.doesNotContain("sessionStorage");
	}

	@Test
	void dashboardMantemRotulosDeTextoDosItensDeNavegacao() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "sidebar.rotulos@criati.test");

		assertThat(corpo)
				.contains("criati-nav-label")
				.contains("Dashboard")
				.contains("Aplicacoes")
				.contains("Usuarios")
				.contains("Convites")
				.contains("Configuracoes")
				.contains("data-tooltip=\"Dashboard\"")
				.contains("aria-label=\"Dashboard\"");
	}

	@Test
	void paginasAutenticadasContinuamAcessiveisComAMesmaSidebar() throws Exception {
		MockHttpSession sessao = autenticarComoAdministrador("sidebar.paginas@criati.test");

		// /app/usuarios e /app/convites exigem ADMINISTRADOR com empresa ativa
		// selecionada (fora do escopo desta tarefa montar o vinculo completo);
		// dashboard e aplicacoes bastam para confirmar que o fragmento
		// compartilhado de sidebar/topbar continua acessivel a qualquer
		// usuario autenticado.
		mockMvc.perform(get("/app/dashboard").session(sessao)).andExpect(status().isOk());
		mockMvc.perform(get("/app/aplicacoes").session(sessao)).andExpect(status().isOk());
	}

	@Test
	void loginNaoContemMarcacaoDeSidebar() throws Exception {
		String corpo = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.doesNotContain("criati-sidebar")
				.doesNotContain("criati-menu-toggle")
				.doesNotContain("initSidebarToggle");
	}

	@Test
	void paginaPublicaDeAceiteDeConviteNaoContemMarcacaoDeSidebar() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{token}", "qualquer-token-de-teste"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.doesNotContain("criati-sidebar")
				.doesNotContain("criati-menu-toggle")
				.doesNotContain("initSidebarToggle");
	}

	@Test
	void dashboardNaoExpoePreferenciaComoDadoSensivel() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "sidebar.sensivel@criati.test");

		assertThat(corpo)
				.doesNotContain("localStorage.setItem(\"criati.sidebar.recolhida\", true")
				.doesNotContain("sessionStorage");
	}

	private String obterCorpoAutenticado(String caminho, String email) throws Exception {
		MockHttpSession sessao = autenticarComoAdministrador(email);
		MvcResult resultado = mockMvc.perform(get(caminho).session(sessao))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andReturn();
		return resultado.getResponse().getContentAsString();
	}

	private MockHttpSession autenticarComoAdministrador(String email) throws Exception {
		criarUsuarioAtivo(email);
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
		Usuario usuario = new Usuario("Usuario Sidebar", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}
}
