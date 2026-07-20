package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContaFinanceiraControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/financeiro/contas";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private ContaFinanceiraRepository contaFinanceiraRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void administradorCriaContaComSucesso() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000161");
		Usuario admin = criarUsuario("conta.admin.criar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE)
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Caixa Loja", "tipo": "CAIXA", "saldoInicial": 500.00 }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.nome").value("Caixa Loja"))
				.andExpect(jsonPath("$.saldoInicial").value(500.00))
				.andExpect(jsonPath("$.saldoAtual").value(500.00))
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void criarContaComSaldoInicialNegativoOuZeroEPermitido() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000162");
		Usuario admin = criarUsuario("conta.saldo.negativo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Conta Negativa", "tipo": "CONTA_CORRENTE", "saldoInicial": -100.00 }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saldoInicial").value(-100.00));

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Conta Zero", "tipo": "CONTA_CORRENTE", "saldoInicial": 0 }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.saldoInicial").value(0));
	}

	@Test
	void criarContaSemNomeRetorna400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000163");
		Usuario admin = criarUsuario("conta.sem.nome@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "", "tipo": "CAIXA", "saldoInicial": 10 }
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void naoDevePermitirNomeDuplicadoEntreContasAtivas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000164");
		Usuario admin = criarUsuario("conta.duplicada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Caixa Unico", "tipo": "CAIXA", "saldoInicial": 0 }
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Caixa Unico", "tipo": "CAIXA", "saldoInicial": 0 }
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void gestorNaoPodeCriarConta() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000165");
		Usuario gestor = criarUsuario("conta.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		MockHttpSession session = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Conta Gestor", "tipo": "CAIXA", "saldoInicial": 0 }
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumPodeListarContas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000166");
		Usuario usuario = criarUsuario("conta.usuario.lista@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO);
		criarConta(empresa, "Conta Vista", new java.math.BigDecimal("10.00"));
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_BASE).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void empresaANaoListaNemConsultaContaDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("77777777000167");
		Empresa empresaB = criarEmpresaComFinanceiro("88888888000168");
		ContaFinanceira contaB = criarConta(empresaB, "Conta B", java.math.BigDecimal.TEN);

		Usuario usuarioA = criarUsuario("conta.empresaA@criati.test");
		criarVinculo(usuarioA, empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(usuarioA.getEmail(), empresaA.getId());

		mockMvc.perform(get(URL_BASE).session(sessaoA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(get(URL_BASE + "/" + contaB.getId()).session(sessaoA))
				.andExpect(status().isNotFound());
	}

	@Test
	void empresaANaoAlteraContaDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("11111111000171");
		Empresa empresaB = criarEmpresaComFinanceiro("22222222000172");
		ContaFinanceira contaB = criarConta(empresaB, "Conta Alvo B", java.math.BigDecimal.ONE);

		Usuario usuarioA = criarUsuario("conta.altera.empresaA@criati.test");
		criarVinculo(usuarioA, empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(usuarioA.getEmail(), empresaA.getId());

		mockMvc.perform(put(URL_BASE + "/" + contaB.getId()).session(sessaoA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Hackeada", "tipo": "CAIXA", "saldoInicial": 0 }
						"""))
				.andExpect(status().isNotFound());

		mockMvc.perform(post(URL_BASE + "/" + contaB.getId() + "/inativar").session(sessaoA).with(csrf()))
				.andExpect(status().isNotFound());

		ContaFinanceira recarregada = contaFinanceiraRepository.findById(contaB.getId()).orElseThrow();
		assertThat(recarregada.getNome()).isEqualTo("Conta Alvo B");
		assertThat(recarregada.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void deveInativarEReativarConta() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000173");
		Usuario admin = criarUsuario("conta.inativar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = criarConta(empresa, "Conta Toggle", java.math.BigDecimal.ZERO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + conta.getId() + "/inativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INATIVO"));

		mockMvc.perform(post(URL_BASE + "/" + conta.getId() + "/inativar").session(session).with(csrf()))
				.andExpect(status().isConflict());

		mockMvc.perform(post(URL_BASE + "/" + conta.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));

		mockMvc.perform(post(URL_BASE + "/" + conta.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void anonimoRecebe401() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
	}

	private ContaFinanceira criarConta(Empresa empresa, String nome, java.math.BigDecimal saldoInicial) {
		return contaFinanceiraRepository.saveAndFlush(
				new ContaFinanceira(empresa, nome, TipoContaFinanceira.CAIXA, saldoInicial, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private MockHttpSession autenticarNaEmpresa(String email, java.util.UUID empresaId) throws Exception {
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
