package br.app.criati.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class AdminEmpresaOnboardingControllerTests {

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
	void deveCriarEmpresaComAdministradorExistenteERetornar201() throws Exception {
		Usuario existente = usuarioRepository.saveAndFlush(
				new Usuario("Admin Existente Http", "admin.existente.http@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.onboarding.existente@criati.test");

		mockMvc.perform(post("/api/admin/empresas/com-administrador-existente")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": { "nome": "Empresa Existente Http", "cnpj": "20111111000101" },
						  "aplicacoesIniciais": ["FINANCEIRO"],
						  "administradorUsuarioId": "%s"
						}
						""".formatted(existente.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empresa.status").value("ATIVO"))
				.andExpect(jsonPath("$.administrador.email").value("admin.existente.http@criati.test"))
				.andExpect(jsonPath("$.administrador.senha").doesNotExist())
				.andExpect(jsonPath("$.vinculo.perfil").value("ADMINISTRADOR"))
				.andExpect(jsonPath("$.aplicacoesHabilitadas[0]").value("FINANCEIRO"));
	}

	@Test
	void usuarioComumRecebe403AoCriarComAdministradorExistente() throws Exception {
		Usuario existente = usuarioRepository.saveAndFlush(
				new Usuario("Alvo", "alvo.onboarding@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		MockHttpSession session = login("usuario.comum.onboarding@criati.test", false);

		mockMvc.perform(post("/api/admin/empresas/com-administrador-existente")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": { "nome": "Empresa Bloqueada", "cnpj": "20222222000102" },
						  "administradorUsuarioId": "%s"
						}
						""".formatted(existente.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveCriarEmpresaComAdministradorConvidadoExpondoTokenSomenteEmTest() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.onboarding.convite@criati.test");

		MvcResult resultado = mockMvc.perform(post("/api/admin/empresas/com-administrador-convidado")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": { "nome": "Empresa Convite Http", "cnpj": "20333333000103" },
						  "aplicacoesIniciais": ["FINANCEIRO"],
						  "administrador": { "nome": "Convidado Http", "email": "convidado.http@criati.test" }
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.convite.email").value("convidado.http@criati.test"))
				.andExpect(jsonPath("$.convite.perfil").value("ADMINISTRADOR"))
				.andExpect(jsonPath("$.tokenBruto").exists())
				.andReturn();

		assertThat(resultado.getResponse().getContentAsString()).doesNotContain("tokenHash");
		assertThat(usuarioRepository.existsByEmailIgnoreCase("convidado.http@criati.test")).isFalse();
	}

	@Test
	void naoDeveConcluirCriacaoQuandoCnpjDuplicadoNoFluxoDeConvite() throws Exception {
		empresaRepository.saveAndFlush(new Empresa("Ja Existe Convite", null, "20444444000104", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.onboarding.convite.dup@criati.test");

		mockMvc.perform(post("/api/admin/empresas/com-administrador-convidado")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": { "nome": "Empresa Duplicada", "cnpj": "20444444000104" },
						  "administrador": { "nome": "X", "email": "x.convite.dup@criati.test" }
						}
						"""))
				.andExpect(status().isConflict());

		assertThat(usuarioRepository.existsByEmailIgnoreCase("x.convite.dup@criati.test")).isFalse();
	}

	@Test
	void deveDetalharEmpresaComSituacaoOperacional() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Detalhe Http", null, "20555555000105", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.detalhe@criati.test");

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Empresa Detalhe Http"))
				.andExpect(jsonPath("$.situacaoOperacional").value("PENDENTE"))
				.andExpect(jsonPath("$.administradoresAtivos").value(0));
	}

	@Test
	void deveRetornar404AoDetalharEmpresaInexistente() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.detalhe.404@criati.test");
		mockMvc.perform(get("/api/admin/empresas/" + java.util.UUID.randomUUID()).session(session))
				.andExpect(status().isNotFound());
	}

	@Test
	void deveAtivarEInativarEmpresaViaApi() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Toggle Http", null, "20666666000106", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.toggle@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/inativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INATIVO"));

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/ativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void naoDeveInativarEmpresaSemCsrf() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Sem Csrf", null, "20777777000107", StatusCadastro.ATIVO));
		MockHttpSession session = loginSuperAdministrador("superadmin.semcsrf@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/inativar").session(session))
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
