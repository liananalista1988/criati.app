package br.app.criati.admin.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminVinculoControllerTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarListarAlterarPerfilSuspenderReativarERemoverVinculo() throws Exception {
		Usuario usuario = usuarioRepository.saveAndFlush(
				new Usuario("Usuario Vinculo Http", "usuario.vinculo.http@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Vinculo Http", null, "50111111000101", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.vinculos.crud@criati.test");

		MvcResult criacao = mockMvc.perform(post("/api/admin/vinculos")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "usuarioId": "%s", "empresaId": "%s", "perfil": "USUARIO" }
						""".formatted(usuario.getId(), empresa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.perfil").value("USUARIO"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andReturn();

		com.jayway.jsonpath.DocumentContext json =
				com.jayway.jsonpath.JsonPath.parse(criacao.getResponse().getContentAsString());
		String vinculoId = json.read("$.usuarioEmpresaId");

		mockMvc.perform(get("/api/admin/vinculos").session(session).param("empresaId", empresa.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].usuarioNome").value("Usuario Vinculo Http"));

		mockMvc.perform(patch("/api/admin/vinculos/" + vinculoId + "/perfil")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "GESTOR" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfil").value("GESTOR"));

		mockMvc.perform(post("/api/admin/vinculos/" + vinculoId + "/suspender").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INATIVO"));

		mockMvc.perform(post("/api/admin/vinculos/" + vinculoId + "/reativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));

		mockMvc.perform(delete("/api/admin/vinculos/" + vinculoId).session(session).with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void naoDeveCriarVinculoDuplicado() throws Exception {
		Usuario usuario = usuarioRepository.saveAndFlush(
				new Usuario("Usuario Dup Vinculo", "usuario.dup.vinculo@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Dup Vinculo", null, "50222222000102", StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO));

		MockHttpSession session = loginSuperAdministrador("superadmin.vinculos.dup@criati.test");

		mockMvc.perform(post("/api/admin/vinculos")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "usuarioId": "%s", "empresaId": "%s", "perfil": "GESTOR" }
						""".formatted(usuario.getId(), empresa.getId())))
				.andExpect(status().isConflict());
	}

	@Test
	void naoDeveSuspenderUltimoAdministradorAtivo() throws Exception {
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Unico Admin", "unico.admin.vinculo@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Unico Admin", null, "50333333000103", StatusCadastro.ATIVO));
		UsuarioEmpresa vinculo = usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));

		MockHttpSession session = loginSuperAdministrador("superadmin.vinculos.ultimoadmin@criati.test");

		mockMvc.perform(post("/api/admin/vinculos/" + vinculo.getId() + "/suspender").session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Empresa deve manter ao menos um Administrador ativo"));
	}

	@Test
	void anonimoRecebe401ParaVinculosGlobais() throws Exception {
		mockMvc.perform(get("/api/admin/vinculos")).andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403ParaVinculosGlobais() throws Exception {
		MockHttpSession session = login("usuario.comum.vinculos.globais@criati.test", false);
		mockMvc.perform(get("/api/admin/vinculos").session(session)).andExpect(status().isForbidden());
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
