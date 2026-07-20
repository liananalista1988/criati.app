package br.app.criati.financeiro.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

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
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardFinanceiroControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_LANCAMENTOS = "/api/contexto/financeiro/lancamentos";
	private static final String URL_DASHBOARD = "/api/contexto/financeiro/dashboard";

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
	private CategoriaFinanceiraRepository categoriaFinanceiraRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCalcularReceitasDespesasResultadoEPendentesDaCompetencia() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000241");
		ContaFinanceira conta = criarConta(empresa, "Caixa", new BigDecimal("1000.00"));
		CategoriaFinanceira receita = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		CategoriaFinanceira despesa = criarCategoria(empresa, "Compras", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("dash.completo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		criarLancamento(session, conta, receita, "RECEITA", "500.00", "2026-07-05", "PAGO", "2026-07-05");
		criarLancamento(session, conta, despesa, "DESPESA", "200.00", "2026-07-06", "PAGO", "2026-07-06");
		criarLancamento(session, conta, receita, "RECEITA", "300.00", "2026-07-10", "PENDENTE", null);
		criarLancamento(session, conta, despesa, "DESPESA", "80.00", "2026-07-11", "PENDENTE", null);
		String canceladoId = criarLancamento(session, conta, receita, "RECEITA", "999.00", "2026-07-12", "PENDENTE", null);
		mockMvc.perform(post(URL_LANCAMENTOS + "/" + canceladoId + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get(URL_DASHBOARD).session(session).param("competencia", "2026-07"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencia").value("2026-07"))
				.andExpect(jsonPath("$.receitasPagas").value(500.00))
				.andExpect(jsonPath("$.despesasPagas").value(200.00))
				.andExpect(jsonPath("$.resultadoMes").value(300.00))
				.andExpect(jsonPath("$.totalPendenteReceber").value(300.00))
				.andExpect(jsonPath("$.totalPendentePagar").value(80.00))
				.andExpect(jsonPath("$.saldoInicialConsolidado").value(1000.00))
				.andExpect(jsonPath("$.saldoAtualConsolidado").value(1300.00))
				.andExpect(jsonPath("$.quantidadeContasAtivas").value(1))
				.andExpect(jsonPath("$.quantidadeLancamentosPeriodo").value(4))
				.andExpect(jsonPath("$.resumoPorCategoria.length()").value(2))
				.andExpect(jsonPath("$.ultimosLancamentos.length()").value(5));
	}

	@Test
	void competenciaSemLancamentosRetornaZerados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000242");
		criarConta(empresa, "Caixa", BigDecimal.ZERO);
		Usuario admin = criarUsuario("dash.vazio@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_DASHBOARD).session(session).param("competencia", "2020-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.receitasPagas").value(0))
				.andExpect(jsonPath("$.despesasPagas").value(0))
				.andExpect(jsonPath("$.resultadoMes").value(0))
				.andExpect(jsonPath("$.resumoPorCategoria.length()").value(0));
	}

	@Test
	void dashboardDaEmpresaANaoSomaDadosDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("33333333000243");
		Empresa empresaB = criarEmpresaComFinanceiro("44444444000244");
		ContaFinanceira contaA = criarConta(empresaA, "Caixa A", BigDecimal.ZERO);
		ContaFinanceira contaB = criarConta(empresaB, "Caixa B", BigDecimal.ZERO);
		CategoriaFinanceira receitaA = criarCategoria(empresaA, "Vendas A", TipoFinanceiro.RECEITA);
		CategoriaFinanceira receitaB = criarCategoria(empresaB, "Vendas B", TipoFinanceiro.RECEITA);

		Usuario adminA = criarUsuario("dash.isolamento.a@criati.test");
		Usuario adminB = criarUsuario("dash.isolamento.b@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());

		criarLancamento(sessaoA, contaA, receitaA, "RECEITA", "111.00", "2026-07-01", "PAGO", "2026-07-01");
		criarLancamento(sessaoB, contaB, receitaB, "RECEITA", "999.00", "2026-07-01", "PAGO", "2026-07-01");

		mockMvc.perform(get(URL_DASHBOARD).session(sessaoA).param("competencia", "2026-07"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.receitasPagas").value(111.00));
	}

	@Test
	void anonimoRecebe401() throws Exception {
		mockMvc.perform(get(URL_DASHBOARD)).andExpect(status().isUnauthorized());
	}

	private String criarLancamento(
			MockHttpSession session,
			ContaFinanceira conta,
			CategoriaFinanceira categoria,
			String tipo,
			String valor,
			String dataCompetencia,
			String status,
			String dataPagamento) throws Exception {
		StringBuilder corpo = new StringBuilder("{")
				.append("\"contaId\":\"").append(conta.getId()).append("\",")
				.append("\"categoriaId\":\"").append(categoria.getId()).append("\",")
				.append("\"tipo\":\"").append(tipo).append("\",")
				.append("\"descricao\":\"Lancamento dashboard\",")
				.append("\"valor\":").append(valor).append(",")
				.append("\"dataCompetencia\":\"").append(dataCompetencia).append("\"");
		if (status != null) {
			corpo.append(",\"status\":\"").append(status).append("\"");
		}
		if (dataPagamento != null) {
			corpo.append(",\"dataPagamento\":\"").append(dataPagamento).append("\"");
		}
		corpo.append("}");

		MvcResult resultado = mockMvc.perform(post(URL_LANCAMENTOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo.toString()))
				.andExpect(status().isCreated())
				.andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
	}

	private ContaFinanceira criarConta(Empresa empresa, String nome, BigDecimal saldoInicial) {
		return contaFinanceiraRepository.saveAndFlush(
				new ContaFinanceira(empresa, nome, TipoContaFinanceira.CAIXA, saldoInicial, StatusCadastro.ATIVO));
	}

	private CategoriaFinanceira criarCategoria(Empresa empresa, String nome, TipoFinanceiro tipo) {
		return categoriaFinanceiraRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, nome, tipo, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO);
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
