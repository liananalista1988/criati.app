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
 * Cobre os estilos visuais Criati/Windows/Compacto (CRIATI-F4-007): renderiza
 * via MockMvc (sem executar JavaScript), confirma presenca do script de
 * preload e do controle de estilo em paginas autenticadas e publicas,
 * confirma que o modulo criati-estilo.js so aceita criati/windows/compact e
 * nao guarda dado sensivel, confirma que as variaveis de navegacao/densidade
 * estao definidas em criati-base.css para os 3 estilos, e confirma que o
 * estilo e completamente independente do tema (data-theme continua intocado).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaEstiloTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void dashboardCarregaScriptDePreloadEDoModuloDeEstilo() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "estilo.scripts@criati.test");

		assertThat(corpo)
				.contains("criati.estilo")
				.contains("data-style")
				.contains("/js/criati-estilo.js");
	}

	@Test
	void dashboardContemControleDeEstiloComAtributosDeAcessibilidade() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "estilo.dashboard@criati.test");

		assertThat(corpo)
				.contains("criati-estilo-controle")
				.contains("criati-estilo-toggle")
				.contains("aria-haspopup=\"true\"")
				.contains("aria-controls=\"criati-estilo-menu\"")
				.contains("id=\"criati-estilo-menu\"")
				.contains("role=\"menu\"")
				.contains("role=\"menuitemradio\"")
				.contains("data-estilo=\"criati\"")
				.contains("data-estilo=\"windows\"")
				.contains("data-estilo=\"compact\"")
				.contains("Criati")
				.contains("Windows")
				.contains("Compacto");
	}

	@Test
	void loginContemControleDeEstiloFlutuanteEScript() throws Exception {
		String corpo = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-estilo-controle-flutuante")
				.contains("criati-estilo-toggle")
				.contains("/js/criati-estilo.js")
				.contains("criati.estilo");
	}

	@Test
	void paginaPublicaDeAceiteContemControleDeEstiloFlutuanteEScript() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{token}", "qualquer-token-de-teste"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-estilo-controle-flutuante")
				.contains("criati-estilo-toggle")
				.contains("/js/criati-estilo.js")
				.contains("criati.estilo");
	}

	@Test
	void scriptCriatiEstiloSoAceitaValoresConhecidosENaoGuardaDadoSensivel() throws Exception {
		String script = mockMvc.perform(get("/js/criati-estilo.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script)
				.contains("\"criati\"")
				.contains("\"windows\"")
				.contains("\"compact\"")
				.contains("criati:estilo-alterado")
				.contains("data-style")
				.doesNotContain("_csrf")
				.doesNotContain("empresaId")
				.doesNotContain("usuarioId")
				.doesNotContain("senha")
				.doesNotContain("sessionStorage");
	}

	@Test
	void preloadDeEstiloValidaValorELePenasAChaveDeEstilo() throws Exception {
		String corpo = obterCorpoAutenticado("/app/dashboard", "estilo.preload@criati.test");
		int indiceInicio = corpo.indexOf("criati.estilo");
		assertThat(indiceInicio).isPositive();
		String trecho = corpo.substring(Math.max(0, indiceInicio - 400), Math.min(corpo.length(), indiceInicio + 400));

		assertThat(trecho)
				.contains("\"criati\"")
				.contains("\"windows\"")
				.contains("\"compact\"")
				.contains("setAttribute(\"data-style\"");
	}

	@Test
	void cssBaseDefineVariaveisDeNavegacaoParaOsTresEstilos() throws Exception {
		String css = mockMvc.perform(get("/css/criati-base.css"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(css)
				.contains("data-style=\"criati\"")
				.contains("data-style=\"windows\"")
				.contains("data-style=\"compact\"")
				.contains("--criati-nav-font-size")
				.contains("--criati-nav-section-font-size")
				.contains("--criati-nav-item-height")
				.contains("--criati-nav-icon-size")
				.contains("--criati-nav-gap")
				.contains("--criati-nav-font-weight")
				.contains("--criati-nav-padding-x")
				.contains("--criati-radius-md")
				.contains("--criati-space-xs")
				.contains("--criati-control-height")
				.contains("--criati-card-padding")
				.contains("--criati-table-cell-padding")
				.contains("--criati-shadow-soft");
	}

	@Test
	void nenhumaPaginaMisturaEstiloComAtributoDeTema() throws Exception {
		// O estilo (data-style) e o tema (data-theme) sao independentes: o
		// preload de estilo nunca deve tocar data-theme e vice-versa.
		String script = mockMvc.perform(get("/js/criati-estilo.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script).doesNotContain("data-theme");

		String scriptTema = mockMvc.perform(get("/js/criati-tema.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(scriptTema).doesNotContain("data-style");
	}

	@Test
	void loginENaoRevelaNenhumaRotaPrivadaNovaAlemDoJaExistente() throws Exception {
		mockMvc.perform(get("/app/dashboard")).andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/api/contexto/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void loginNaoContemMarcacaoDeSidebarMesmoComControleDeEstilo() throws Exception {
		String corpo = mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo).doesNotContain("criati-sidebar").doesNotContain("criati-menu-toggle");
	}

	@Test
	void paginasAdministrativasCarregamOsRecursosDeEstilo() throws Exception {
		String email = "estilo.admin@criati.test";
		usuarioRepository.saveAndFlush(Usuario.criarSuperAdministrador("Usuario Estilo Admin", email, passwordEncoder.encode(SENHA)));
		MockHttpSession sessao = autenticar(email);

		MvcResult resultado = mockMvc.perform(get("/app/admin").session(sessao))
				.andExpect(status().isOk())
				.andReturn();
		String corpo = resultado.getResponse().getContentAsString();

		assertThat(corpo)
				.contains("/js/criati-estilo.js")
				.contains("criati-estilo-controle")
				.contains("data-style");
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

	private String obterCorpoAutenticado(String caminho, String email) throws Exception {
		Usuario usuario = new Usuario("Usuario Estilo", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(usuario);
		MockHttpSession sessao = autenticar(email);

		MvcResult resultado = mockMvc.perform(get(caminho).session(sessao))
				.andExpect(status().isOk())
				.andReturn();
		return resultado.getResponse().getContentAsString();
	}
}
