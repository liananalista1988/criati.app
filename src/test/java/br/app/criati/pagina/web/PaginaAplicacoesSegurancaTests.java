package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import br.app.criati.aplicacao.service.AplicacaoService;
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
class PaginaAplicacoesSegurancaTests {

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
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void aplicacoesAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app/aplicacoes"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void aplicacoesSemEmpresaAtivaVoltaParaSelecao() throws Exception {
		Usuario usuario = criarUsuario("pagina.aplicacoes@criati.test");
		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/app/aplicacoes").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/dashboard"));
	}

	@Test
	void financeiroAcessivelQuandoHabilitado() throws Exception {
		Usuario usuario = criarUsuario("pagina.financeiro.ok@criati.test");
		Empresa empresa = criarEmpresa("11111111000151");
		criarVinculo(usuario, empresa);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		MvcResult resultado = mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(resultado.getResponse().getContentAsString()).contains("Gerenciador Financeiro");
	}

	@Test
	void financeiroRedirecionaQuandoNaoHabilitado() throws Exception {
		Usuario usuario = criarUsuario("pagina.financeiro.bloqueado@criati.test");
		Empresa empresa = criarEmpresa("22222222000152");
		criarVinculo(usuario, empresa);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void clinicaAcessivelQuandoHabilitada() throws Exception {
		Usuario usuario = criarUsuario("pagina.clinica.ok@criati.test");
		Empresa empresa = criarEmpresa("33333333000153");
		criarVinculo(usuario, empresa);
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		MvcResult resultado = mockMvc.perform(get("/app/clinica").session(session))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(resultado.getResponse().getContentAsString()).contains("Gestao de Clinica");
	}

	@Test
	void empresaComFinanceiroNaoAcessaClinica() throws Exception {
		Usuario usuario = criarUsuario("pagina.so.financeiro@criati.test");
		Empresa empresa = criarEmpresa("44444444000154");
		criarVinculo(usuario, empresa);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		mockMvc.perform(get("/app/clinica").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void empresaComClinicaNaoAcessaFinanceiro() throws Exception {
		Usuario usuario = criarUsuario("pagina.so.clinica@criati.test");
		Empresa empresa = criarEmpresa("55555555000155");
		criarVinculo(usuario, empresa);
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void financeiroSemContextoRedireciona() throws Exception {
		Usuario usuario = criarUsuario("pagina.sem.contexto@criati.test");
		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void adminEmpresasAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app/admin/empresas"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void adminEmpresasSuperAdministradorRetorna200() throws Exception {
		Usuario superAdmin = Usuario.criarSuperAdministrador(
				"Superadmin Teste", "pagina.admin.super@criati.test", passwordEncoder.encode(SENHA));
		usuarioRepository.saveAndFlush(superAdmin);
		MockHttpSession session = login(superAdmin.getEmail());

		mockMvc.perform(get("/app/admin/empresas").session(session))
				.andExpect(status().isOk());
	}

	@Test
	void adminEmpresasUsuarioComumRecebe403Json() throws Exception {
		Usuario usuario = criarUsuario("pagina.admin.comum@criati.test");
		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/app/admin/empresas").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	private void selecionarEmpresa(MockHttpSession session, java.util.UUID empresaId) throws Exception {
		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
	}

	private MockHttpSession login(String email) throws Exception {
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

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private Empresa criarEmpresa(String cnpj) {
		Empresa empresa = new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO);
		return empresaRepository.saveAndFlush(empresa);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}
}
