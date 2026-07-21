package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// Cobre as novas rotas de pagina do painel global do Superadministrador
// (/app/admin/**): autenticacao, autorizacao (ROLE_SUPERADMIN, nunca perfil
// empresarial) e a visibilidade condicional do bloco "Administracao da
// plataforma" na sidebar.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaAdminSegurancaTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void adminDashboardAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app/admin"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void adminDashboardSuperAdministradorRetorna200() throws Exception {
		MockHttpSession session = loginSuperAdministrador("pagina.admin.dashboard@criati.test");
		mockMvc.perform(get("/app/admin").session(session)).andExpect(status().isOk());
	}

	@Test
	void adminDashboardUsuarioComumRecebe403Json() throws Exception {
		MockHttpSession session = login("pagina.admin.dashboard.comum@criati.test", false);
		mockMvc.perform(get("/app/admin").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void adminEmpresasNovaExigeSuperAdministrador() throws Exception {
		MockHttpSession comum = login("pagina.admin.nova.comum@criati.test", false);
		mockMvc.perform(get("/app/admin/empresas/nova").session(comum)).andExpect(status().isForbidden());

		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.nova.super@criati.test");
		mockMvc.perform(get("/app/admin/empresas/nova").session(superAdmin)).andExpect(status().isOk());
	}

	@Test
	void adminEmpresaDetalheExigeSuperAdministrador() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Pagina Detalhe", null, "70111111000101", StatusCadastro.ATIVO));

		MockHttpSession comum = login("pagina.admin.detalhe.comum@criati.test", false);
		mockMvc.perform(get("/app/admin/empresas/" + empresa.getId()).session(comum)).andExpect(status().isForbidden());

		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.detalhe.super@criati.test");
		mockMvc.perform(get("/app/admin/empresas/" + empresa.getId()).session(superAdmin)).andExpect(status().isOk());
	}

	@Test
	void adminUsuariosExigeSuperAdministrador() throws Exception {
		MockHttpSession comum = login("pagina.admin.usuarios.comum@criati.test", false);
		mockMvc.perform(get("/app/admin/usuarios").session(comum)).andExpect(status().isForbidden());

		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.usuarios.super@criati.test");
		mockMvc.perform(get("/app/admin/usuarios").session(superAdmin)).andExpect(status().isOk());
	}

	@Test
	void adminVinculosExigeSuperAdministrador() throws Exception {
		MockHttpSession comum = login("pagina.admin.vinculos.comum@criati.test", false);
		mockMvc.perform(get("/app/admin/vinculos").session(comum)).andExpect(status().isForbidden());

		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.vinculos.super@criati.test");
		mockMvc.perform(get("/app/admin/vinculos").session(superAdmin)).andExpect(status().isOk());
	}

	@Test
	void sidebarMostraSecaoAdministrativaSomenteParaSuperAdministrador() throws Exception {
		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.sidebar.super@criati.test");
		MvcResult resultadoSuper = mockMvc.perform(get("/app/dashboard").session(superAdmin))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(resultadoSuper.getResponse().getContentAsString()).contains("Administracao da plataforma");

		MockHttpSession comum = login("pagina.admin.sidebar.comum@criati.test", false);
		MvcResult resultadoComum = mockMvc.perform(get("/app/dashboard").session(comum))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(resultadoComum.getResponse().getContentAsString()).doesNotContain("Administracao da plataforma");
	}

	@Test
	void naoExisteAcessoImplicitoDoSuperAdministradorAEmpresaAlheia() throws Exception {
		// Superadministrador sem vinculo empresarial continua sem contexto de
		// empresa ativa: paginas empresariais (/app/usuarios) permanecem 403,
		// mesmo autenticado como Superadministrador.
		MockHttpSession superAdmin = loginSuperAdministrador("pagina.admin.semvinculo@criati.test");
		mockMvc.perform(get("/app/usuarios").session(superAdmin))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	private MockHttpSession loginSuperAdministrador(String email) throws Exception {
		return login(email, true);
	}

	private MockHttpSession login(String email, boolean superAdministrador) throws Exception {
		Usuario usuario = superAdministrador
				? Usuario.criarSuperAdministrador("Usuario Teste", email, passwordEncoder.encode(SENHA))
				: new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(usuario);

		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "senha": "%s"
						}
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
