package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Cobre a correcao do contexto de empresa do Superadministrador (CRIATI-F4-009):
 * confirma que o skeleton do seletor de empresa na topbar nunca fica preso
 * (o script agora resolve o widget em todo ramo de iniciar(), e as paginas
 * administrativas passam a carregar a identidade do usuario), que o estado
 * vazio do dashboard diferencia o Superadministrador, e que o backend
 * continua sendo a unica fonte de verdade para contexto de empresa (o
 * Superadministrador nao ganha nenhum acesso novo por este ajuste).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaContextoSuperadminTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Test
	void paginasAdministrativasCarregamCriatiContextoEChamamIniciarIdentidade() throws Exception {
		MockHttpSession sessao = autenticarSuperAdministrador("contexto.admin1@criati.test");
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Contexto Ltda", "Empresa Contexto", "11444777000161",
						br.app.criati.shared.enums.StatusCadastro.ATIVO));

		String[] caminhos = {
				"/app/admin",
				"/app/admin/empresas",
				"/app/admin/empresas/nova",
				"/app/admin/empresas/" + empresa.getId(),
				"/app/admin/usuarios",
				"/app/admin/vinculos"
		};
		for (String caminho : caminhos) {
			String corpo = mockMvc.perform(get(caminho).session(sessao))
					.andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			assertThat(corpo)
					.as("pagina %s deve carregar criati-contexto.js e chamar iniciarIdentidade()", caminho)
					.contains("/js/criati-contexto.js")
					.contains("CriatiContexto.iniciarIdentidade();");
		}
	}

	@Test
	void dashboardContemElementosDeEstadoVazioDiferenciadoParaSuperadministrador() throws Exception {
		String corpo = obterCorpoAutenticadoComum("/app/dashboard", "contexto.dashboard@criati.test");

		assertThat(corpo)
				.contains("id=\"criati-dashboard-vazio-texto\"")
				.contains("id=\"criati-dashboard-vazio-acao\"")
				.contains("/app/admin/empresas");
	}

	@Test
	void scriptCriatiContextoResolveTopbarEmTodoRamoENuncaUsaConsoleLog() throws Exception {
		String script = mockMvc.perform(get("/js/criati-contexto.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script)
				.contains("function renderTopbarEmpresa")
				.contains("function limparTopbarEmpresa")
				.contains("function iniciarIdentidade")
				.contains("ehSuperAdministrador")
				.doesNotContain("console.log")
				.doesNotContain("setInterval")
				.doesNotContain("_csrf")
				.doesNotContain("senha")
				.doesNotContain("token");

		// renderTopbarEmpresa deve ser chamado antes do primeiro "return" do ramo
		// vazio (empresas.length === 0) - garante que o skeleton e resolvido
		// mesmo quando nao ha nenhuma empresa (caso permanente do Superadministrador).
		int indiceChamada = script.indexOf("renderTopbarEmpresa(empresas,");
		int indiceRamoVazio = script.indexOf("if (empresas.length === 0)");
		assertThat(indiceChamada).isPositive();
		assertThat(indiceRamoVazio).isPositive();
		assertThat(indiceChamada).isLessThan(indiceRamoVazio);
	}

	@Test
	void scriptCriatiContextoExpoeIniciarIdentidadeParaPaginasAdministrativas() throws Exception {
		String script = mockMvc.perform(get("/js/criati-contexto.js"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(script).contains("window.CriatiContexto = { iniciar: iniciar, iniciarIdentidade: iniciarIdentidade };");
	}

	@Test
	void superAdministradorSemVinculoRecebeListaVaziaENenhumContextoAtivo() throws Exception {
		MockHttpSession sessao = autenticarSuperAdministrador("contexto.vazio@criati.test");

		mockMvc.perform(get("/api/contexto/empresas").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessao))
				.andExpect(status().isNoContent());
	}

	@Test
	void superAdministradorNaoConsegueSelecionarEmpresaSemVinculoProprio() throws Exception {
		MockHttpSession sessao = autenticarSuperAdministrador("contexto.semvinculo@criati.test");

		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(sessao)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void superAdministradorConsultaProprioPerfilViaAuthMe() throws Exception {
		String email = "contexto.me@criati.test";
		MockHttpSession sessao = autenticarSuperAdministrador(email);

		mockMvc.perform(get("/api/auth/me").session(sessao))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	private MockHttpSession autenticarSuperAdministrador(String email) throws Exception {
		usuarioRepository.saveAndFlush(
				Usuario.criarSuperAdministrador("Superadmin Contexto", email, passwordEncoder.encode(SENHA)));
		return autenticar(email);
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

	private String obterCorpoAutenticadoComum(String caminho, String email) throws Exception {
		Usuario usuario = new Usuario("Usuario Comum Contexto", email, passwordEncoder.encode(SENHA),
				br.app.criati.shared.enums.StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(usuario);
		MockHttpSession sessao = autenticar(email);

		MvcResult resultado = mockMvc.perform(get(caminho).session(sessao))
				.andExpect(status().isOk())
				.andReturn();
		return resultado.getResponse().getContentAsString();
	}
}
