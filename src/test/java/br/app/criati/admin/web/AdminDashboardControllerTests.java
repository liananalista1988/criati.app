package br.app.criati.admin.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDashboardControllerTests {

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
	void deveRetornarNumerosReaisDoDashboard() throws Exception {
		empresaRepository.saveAndFlush(new Empresa("Empresa Dashboard Ativa", null, "60111111000101", StatusCadastro.ATIVO));
		empresaRepository.saveAndFlush(new Empresa("Empresa Dashboard Inativa", null, "60222222000102", StatusCadastro.INATIVO));

		MockHttpSession session = loginSuperAdministrador("superadmin.dashboard@criati.test");

		mockMvc.perform(get("/api/admin/dashboard").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresasAtivas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.empresasInativas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.totalEmpresas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.empresasRecentes").isArray());
	}

	@Test
	void anonimoRecebe401ParaDashboard() throws Exception {
		mockMvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403ParaDashboard() throws Exception {
		MockHttpSession session = login("usuario.comum.dashboard@criati.test", false);
		mockMvc.perform(get("/api/admin/dashboard").session(session)).andExpect(status().isForbidden());
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
