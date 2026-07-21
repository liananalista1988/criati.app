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
 * Cobre os temas claro/escuro/automatico (CRIATI-F4-005): renderiza via
 * MockMvc (sem executar JavaScript), confirma presenca do controle de tema e
 * do script de preload em paginas autenticadas e publicas, confirma que o
 * modulo criati-tema.js so aceita auto/light/dark e nao guarda dado sensivel,
 * e confirma que a sidebar continua restrita as paginas autenticadas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaTemaTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void dashboardContemControleDeTemaComAtributosDeAcessibilidade() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "tema.dashboard@criati.test");

		assertThat(corpo)
				.contains("criati-tema-controle")
				.contains("criati-tema-toggle")
				.contains("aria-haspopup=\"true\"")
				.contains("aria-controls=\"criati-tema-menu\"")
				.contains("id=\"criati-tema-menu\"")
				.contains("role=\"menu\"")
				.contains("role=\"menuitemradio\"")
				.contains("data-tema=\"auto\"")
				.contains("data-tema=\"light\"")
				.contains("data-tema=\"dark\"")
				.contains("Automatico")
				.contains("Claro")
				.contains("Escuro");
	}

	@Test
	void dashboardCarregaScriptDePreloadEDoModuloDeTema() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "tema.scripts@criati.test");

		assertThat(corpo)
				.contains("criati.tema")
				.contains("prefers-color-scheme: dark")
				.contains("/js/criati-tema.js");
	}

	@Test
	void loginContemControleDeTemaFlutuanteEScript() throws Exception {
		String corpo = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-tema-controle-flutuante")
				.contains("criati-tema-toggle")
				.contains("/js/criati-tema.js")
				.contains("criati.tema");
	}

	@Test
	void paginaPublicaDeAceiteContemControleDeTemaFlutuanteEScript() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{token}", "qualquer-token-de-teste"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-tema-controle-flutuante")
				.contains("criati-tema-toggle")
				.contains("/js/criati-tema.js")
				.contains("criati.tema");
	}

	@Test
	void scriptCriatiTemaSoAceitaValoresConhecidosENaoGuardaDadoSensivel() throws Exception {
		String script = mockMvc.perform(get("/js/criati-tema.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script)
				.contains("\"auto\"")
				.contains("\"light\"")
				.contains("\"dark\"")
				.contains("criati:tema-alterado")
				.contains("prefers-color-scheme: dark")
				.doesNotContain("_csrf")
				.doesNotContain("empresaId")
				.doesNotContain("usuarioId")
				.doesNotContain("senha")
				.doesNotContain("sessionStorage");
	}

	@Test
	void nenhumaPaginaTemMetaColorSchemeFixo() throws Exception {
		assertThat(mockMvc.perform(get("/login")).andReturn().getResponse().getContentAsString())
				.doesNotContain("name=\"color-scheme\"");

		String dashboard = obterCorpoAutenticado("/app/dashboard", "tema.colorscheme@criati.test");
		assertThat(dashboard).doesNotContain("name=\"color-scheme\"");
	}

	@Test
	void loginENaoRevelaNenhumaRotaPrivadaNovaAlemDoJaExistente() throws Exception {
		mockMvc.perform(get("/app/dashboard")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/api/contexto/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void loginNaoContemMarcacaoDeSidebarMesmoComControleDeTema() throws Exception {
		String corpo = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo).doesNotContain("criati-sidebar").doesNotContain("criati-menu-toggle");
	}

	private String obterCorpoAutenticado(String caminho, String email) throws Exception {
		Usuario usuario = new Usuario("Usuario Tema", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(usuario);

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
		MockHttpSession sessao = (MockHttpSession) loginResult.getRequest().getSession(false);

		MvcResult resultado = mockMvc.perform(get(caminho).session(sessao))
				.andExpect(status().isOk())
				.andReturn();
		return resultado.getResponse().getContentAsString();
	}
}
