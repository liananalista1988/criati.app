package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.util.ReflectionTestUtils;
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
class PaginaUsuariosConvitesSegurancaTests {

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
	void usuariosAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app/usuarios"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void convitesAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app/convites"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void administradorAcessaUsuariosEConvites() throws Exception {
		Empresa empresa = criarEmpresa("11111111000311");
		Usuario admin = criarUsuario("pagina.acessos.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.contentTypeCompatibleWith(MediaType.TEXT_HTML));
		mockMvc.perform(get("/app/convites").session(session))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@Test
	void gestorRecebe403AoAcessarUsuariosEConvites() throws Exception {
		Empresa empresa = criarEmpresa("22222222000312");
		Usuario gestor = criarUsuario("pagina.acessos.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
		mockMvc.perform(get("/app/convites").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void usuarioComumRecebe403AoAcessarUsuariosEConvites() throws Exception {
		Empresa empresa = criarEmpresa("33333333000313");
		Usuario usuario = criarUsuario("pagina.acessos.usuario@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
		mockMvc.perform(get("/app/convites").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void superAdministradorSemVinculoNaoAcessaImplicitamente() throws Exception {
		Usuario superAdmin = Usuario.criarSuperAdministrador(
				"Superadmin Teste", "pagina.acessos.super@criati.test", passwordEncoder.encode(SENHA));
		usuarioRepository.saveAndFlush(superAdmin);

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(superAdmin.getEmail(), SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void semEmpresaAtivaRecebeTratamentoApropriado() throws Exception {
		Usuario usuario = criarUsuario("pagina.acessos.sem.empresa@criati.test");
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(usuario.getEmail(), SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void empresaInativaBloqueiaAcesso() throws Exception {
		Empresa empresa = criarEmpresa("44444444000314");
		Usuario admin = criarUsuario("pagina.acessos.empresa.inativa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void vinculoInativoBloqueiaAcesso() throws Exception {
		Empresa empresa = criarEmpresa("55555555000315");
		Usuario admin = criarUsuario("pagina.acessos.vinculo.inativo@criati.test");
		UsuarioEmpresa vinculo = criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		ReflectionTestUtils.setField(vinculo, "status", StatusCadastro.INATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);

		mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void paginasNaoExpoemSenhaHashOuTokenHash() throws Exception {
		Empresa empresa = criarEmpresa("66666666000316");
		Usuario admin = criarUsuario("pagina.acessos.sem.senha@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		String corpoUsuarios = mockMvc.perform(get("/app/usuarios").session(session))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String corpoConvites = mockMvc.perform(get("/app/convites").session(session))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpoUsuarios)
				.doesNotContain(SENHA)
				.doesNotContain("$2a$")
				.doesNotContain("$2b$")
				.doesNotContain("tokenHash")
				.doesNotContain("token_hash");
		assertThat(corpoConvites)
				.doesNotContain(SENHA)
				.doesNotContain("$2a$")
				.doesNotContain("$2b$")
				.doesNotContain("tokenHash")
				.doesNotContain("token_hash");
	}

	@Test
	void apiContinuaRetornando401JsonParaAnonimo() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/api/contexto/usuarios"))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(resultado.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);

		MvcResult resultadoConvites = mockMvc.perform(get("/api/contexto/convites"))
				.andExpect(status().isUnauthorized())
				.andReturn();
		assertThat(resultadoConvites.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
	}

	@Test
	void csrfContinuaExigidoParaOperacoesMutaveis() throws Exception {
		Empresa empresa = criarEmpresa("77777777000317");
		Usuario admin = criarUsuario("pagina.acessos.csrf@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(
				criarUsuario("pagina.acessos.csrf.alvo@criati.test"), empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender").session(session))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "csrf.teste@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isForbidden());
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, status);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}
}
