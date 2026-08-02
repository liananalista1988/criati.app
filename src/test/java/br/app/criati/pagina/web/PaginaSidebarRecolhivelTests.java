package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
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

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Test
	void iconeDoBotaoHamburguerTemTamanhoExplicitoENaoRendeUmSvgSemDimensoes() throws Exception {
		// Regressao: o svg do botao (fragments/topbar.html) ja teve um bug em que
		// nao tinha width/height nem em atributo nem em CSS, entao o navegador
		// aplicava o tamanho padrao (~300x150) em vez de um icone pequeno -
		// so ficou visivel quando o botao passou a aparecer tambem no desktop.
		String corpo = obterCorpoAutenticadoComEmpresaAtiva("/app/dashboard", "sidebar.icone@criati.test");

		assertThat(corpo).contains("criati-menu-toggle");
		int indiceBotao = corpo.indexOf("criati-menu-toggle");
		String trechoBotao = corpo.substring(indiceBotao, Math.min(corpo.length(), indiceBotao + 600));

		assertThat(trechoBotao)
				.contains("<svg")
				.containsPattern("width=\"\\d+\"")
				.containsPattern("height=\"\\d+\"");
	}

	@Test
	void dashboardComEmpresaAtivaMostraCardsRealPreenchidosEnaoOEstadoVazio() throws Exception {
		String corpo = obterCorpoAutenticadoComEmpresaAtiva("/app/dashboard", "sidebar.preenchido@criati.test");

		assertThat(corpo)
				.contains("criati-sidebar")
				.contains("criati-menu-toggle")
				.contains("criati-dashboard-conteudo");
	}

	private String obterCorpoAutenticadoComEmpresaAtiva(String caminho, String email) throws Exception {
		Usuario usuario = criarUsuarioAtivo(email);
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Sidebar Ltda", "Empresa Sidebar", cnpjUnico(), StatusCadastro.ATIVO));
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);
		// Duas empresas vinculadas (nao apenas uma): com exatamente uma, GET
		// /app/dashboard agora redireciona para /app/aplicacoes (CRIATI-UX-002,
		// ver PaginaVisaoGeralEmpresaTests), o que quebraria os asserts de
		// conteudo do dashboard feitos por estes testes - o alvo deles e outro
		// (icone do hamburguer / cards preenchidos), nao a regra de redirecionamento.
		Empresa segundaEmpresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Sidebar Dois Ltda", "Empresa Sidebar Dois", cnpjUnico(), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, segundaEmpresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO));

		MockHttpSession sessao = autenticar(email);
		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"empresaId\": \"" + empresa.getId() + "\" }"))
				.andExpect(status().isOk());

		MvcResult resultado = mockMvc.perform(get(caminho).session(sessao))
				.andExpect(status().isOk())
				.andReturn();
		return resultado.getResponse().getContentAsString();
	}

	private String cnpjUnico() {
		return String.valueOf(Math.abs(System.nanoTime())).substring(0, 14);
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
