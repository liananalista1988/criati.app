package br.app.criati.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class AdminEmpresaConviteControllerTests {

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
	void deveCriarListarERevogarConviteDeEmpresaComoSuperAdministrador() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Convite Admin", null, "30111111000101", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.convite.empresa@criati.test");

		MvcResult criacao = mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado.empresa@criati.test", "perfil": "GESTOR" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.convite.email").value("convidado.empresa@criati.test"))
				.andExpect(jsonPath("$.tokenBruto").exists())
				.andReturn();

		String corpo = criacao.getResponse().getContentAsString();
		assertThat(corpo).doesNotContain("tokenHash");

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/convites").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("convidado.empresa@criati.test"))
				.andExpect(jsonPath("$[0].tokenBruto").doesNotExist());

		com.jayway.jsonpath.DocumentContext json = com.jayway.jsonpath.JsonPath.parse(corpo);
		String conviteId = json.read("$.convite.id");

		mockMvc.perform(delete("/api/admin/empresas/" + empresa.getId() + "/convites/" + conviteId)
				.session(session)
				.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/convites").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("REVOGADO"));
	}

	@Test
	void anonimoRecebe401AoListarConvitesDeEmpresa() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Convite Anon", null, "30222222000102", StatusCadastro.ATIVO));
		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/convites"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403AoCriarConviteDeEmpresa() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Convite Comum", null, "30333333000103", StatusCadastro.ATIVO));
		MockHttpSession session = login("usuario.comum.convite.empresa@criati.test", false);

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "x@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isForbidden());
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
