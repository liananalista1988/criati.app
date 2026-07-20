package br.app.criati.aplicacao.web;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminEmpresaAplicacaoControllerTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private AplicacaoRepository aplicacaoRepository;

	@Autowired
	private EmpresaAplicacaoRepository empresaAplicacaoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void superAdministradorListaSituacaoDasAplicacoesDaEmpresa() throws Exception {
		Empresa empresa = criarEmpresa("11111111000121", StatusCadastro.ATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.situacao@criati.test");

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/aplicacoes").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.codigo == 'FINANCEIRO')].statusVinculo").value(org.hamcrest.Matchers.contains(
						org.hamcrest.Matchers.nullValue())));
	}

	@Test
	void deveHabilitarAplicacaoParaEmpresaAtiva() throws Exception {
		Empresa empresa = criarEmpresa("22222222000122", StatusCadastro.ATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.habilitar@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/habilitar")
				.session(session)
				.with(csrf()))
				.andExpect(status().isOk());

		Aplicacao financeiro = aplicacaoRepository.findByCodigo("FINANCEIRO").orElseThrow();
		assertThat(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresa.getId(), financeiro.getId(), StatusCadastro.ATIVO)).isTrue();
	}

	@Test
	void habilitarEIdempotenteENaoDuplicaVinculo() throws Exception {
		Empresa empresa = criarEmpresa("33333333000123", StatusCadastro.ATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.habilitar.idempotente@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/CLINICA/habilitar")
				.session(session).with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/CLINICA/habilitar")
				.session(session).with(csrf())).andExpect(status().isOk());

		Aplicacao clinica = aplicacaoRepository.findByCodigo("CLINICA").orElseThrow();
		assertThat(empresaAplicacaoRepository.findAllByEmpresaId(empresa.getId()).stream()
				.filter(v -> v.getAplicacao().getId().equals(clinica.getId()))
				.count()).isEqualTo(1);
	}

	@Test
	void deveDesabilitarAplicacaoDeFormaIdempotenteSemExcluirFisicamente() throws Exception {
		Empresa empresa = criarEmpresa("44444444000124", StatusCadastro.ATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.desabilitar@criati.test");
		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/habilitar")
				.session(session).with(csrf())).andExpect(status().isOk());

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/desabilitar")
				.session(session).with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/desabilitar")
				.session(session).with(csrf())).andExpect(status().isOk());

		Aplicacao financeiro = aplicacaoRepository.findByCodigo("FINANCEIRO").orElseThrow();
		assertThat(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresa.getId(), financeiro.getId(), StatusCadastro.ATIVO)).isFalse();
		assertThat(empresaAplicacaoRepository.findAllByEmpresaId(empresa.getId())).hasSize(1);
	}

	@Test
	void naoDeveHabilitarQuandoEmpresaInativa() throws Exception {
		Empresa empresa = criarEmpresa("55555555000125", StatusCadastro.INATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.empresa.inativa@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/habilitar")
				.session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Empresa inativa"));
	}

	@Test
	void naoDeveHabilitarQuandoAplicacaoInativa() throws Exception {
		Empresa empresa = criarEmpresa("66666666000126", StatusCadastro.ATIVO);
		Aplicacao financeiro = aplicacaoRepository.findByCodigo("FINANCEIRO").orElseThrow();
		ReflectionTestUtils.setField(financeiro, "status", StatusCadastro.INATIVO);
		aplicacaoRepository.saveAndFlush(financeiro);
		MockHttpSession session = loginSuperAdministrador("superadmin.aplicacao.inativa@criati.test");

		try {
			mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/habilitar")
					.session(session).with(csrf()))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value("Aplicacao inativa"));
		} finally {
			ReflectionTestUtils.setField(financeiro, "status", StatusCadastro.ATIVO);
			aplicacaoRepository.saveAndFlush(financeiro);
		}
	}

	@Test
	void habilitarSemTokenCsrfRecebe403() throws Exception {
		Empresa empresa = criarEmpresa("77777777000127", StatusCadastro.ATIVO);
		MockHttpSession session = loginSuperAdministrador("superadmin.sem.csrf@criati.test");

		mockMvc.perform(post("/api/admin/empresas/" + empresa.getId() + "/aplicacoes/FINANCEIRO/habilitar")
				.session(session))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonimoRecebe401() throws Exception {
		Empresa empresa = criarEmpresa("88888888000128", StatusCadastro.ATIVO);

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/aplicacoes"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403() throws Exception {
		Empresa empresa = criarEmpresa("99999999000129", StatusCadastro.ATIVO);
		MockHttpSession session = login("usuario.comum.aplicacoes@criati.test", false);

		mockMvc.perform(get("/api/admin/empresas/" + empresa.getId() + "/aplicacoes").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	private Empresa criarEmpresa(String cnpj, StatusCadastro status) {
		Empresa empresa = new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, status);
		return empresaRepository.saveAndFlush(empresa);
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
