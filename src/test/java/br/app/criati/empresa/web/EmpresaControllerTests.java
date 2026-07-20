package br.app.criati.empresa.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmpresaControllerTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCadastrarEmpresaERetornar201QuandoSuperAdministrador() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.201@criati.test");

		mockMvc.perform(post("/api/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Criati Tecnologia Ltda",
						  "nomeFantasia": "Criati",
						  "cnpj": "12.345.678/0001-90"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Criati Tecnologia Ltda"))
				.andExpect(jsonPath("$.nomeFantasia").value("Criati"))
				.andExpect(jsonPath("$.cnpj").value("12345678000190"))
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void deveRetornar400ParaEntradaInvalida() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.400@criati.test");

		mockMvc.perform(post("/api/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "",
						  "cnpj": ""
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.nome").value("Nome e obrigatorio"))
				.andExpect(jsonPath("$.fieldErrors.cnpj").value("CNPJ e obrigatorio"));
	}

	@Test
	void deveRetornar409ParaCnpjDuplicado() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.409@criati.test");
		String corpo = """
				{
				  "nome": "Empresa Duplicada",
				  "cnpj": "98765432000110"
				}
				""";

		mockMvc.perform(post("/api/empresas").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/empresas").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("CNPJ ja cadastrado"));
	}

	@Test
	void deveRetornar401ParaRequisicaoAnonima() throws Exception {
		// CSRF valido incluido de proposito para isolar a checagem de
		// autenticacao: sem ele, o filtro de CSRF rejeitaria antes (403) sem
		// nunca chegar a verificar se ha usuario autenticado.
		mockMvc.perform(post("/api/empresas")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Empresa Anonima",
						  "cnpj": "11111111000101"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveRetornar403ParaUsuarioComumAutenticado() throws Exception {
		MockHttpSession session = login("usuario.comum@criati.test", false);

		mockMvc.perform(post("/api/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Empresa Bloqueada",
						  "cnpj": "22222222000102"
						}
						"""))
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
