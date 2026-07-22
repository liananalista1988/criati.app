package br.app.criati.financeiro.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
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
class ContasAPagarControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String COMPROMISSOS = "/api/contexto/financeiro/compromissos";
	private static final String OCORRENCIAS = "/api/contexto/financeiro/ocorrencias-compromisso";
	private static final String RECORRENCIAS = "/api/contexto/financeiro/recorrencias";

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
	private PessoaFinanceiraRepository pessoaFinanceiraRepository;
	@Autowired
	private AplicacaoService aplicacaoService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/* ==================== COMPROMISSO ==================== */

	@Test
	void deveCriarCompromissoFixoEVariavel() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000411");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Condominio", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.compromisso.fixo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Condominio","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "tipoValor":"FIXO","valorPadrao":450.00,"diaVencimentoPadrao":10}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoValor").value("FIXO"))
				.andExpect(jsonPath("$.ativo").value(true));

		mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Energia","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"VARIAVEL"}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoValor").value("VARIAVEL"));
	}

	@Test
	void categoriaDeReceitaERejeitadaParaCompromisso() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000412");
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("cap.compromisso.categoria.invalida@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Invalido","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"FIXO"}
						""".formatted(categoriaReceita.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveAtivarEDesativarCompromisso() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000413");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Assinatura", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.compromisso.ativacao@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarCompromissoAvulso(session, categoria, pessoa, "Assinatura streaming");

		mockMvc.perform(post(COMPROMISSOS + "/" + id + "/desativar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
		mockMvc.perform(post(COMPROMISSOS + "/" + id + "/ativar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(true));
	}

	@Test
	void compromissoDeOutroTenantResultaEmNaoEncontrado() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("44444444000414");
		Empresa b = criarEmpresaComFinanceiro("55555555000415");
		Usuario adminA = criarUsuario("cap.compromisso.tenant.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("cap.compromisso.tenant.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoriaA = criarCategoria(a, "Aluguel", TipoFinanceiro.DESPESA);
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		MockHttpSession sessionB = autenticarNaEmpresa(adminB.getEmail(), b.getId());
		String idA = criarCompromissoAvulso(sessionA, categoriaA, pessoaA, "Aluguel");

		mockMvc.perform(get(COMPROMISSOS + "/" + idA).session(sessionB)).andExpect(status().isNotFound());
	}

	/* ==================== OCORRENCIA AVULSA ==================== */

	@Test
	void deveCriarOcorrenciaAvulsaComJurosMultaEDesconto() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000416");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Manutencao", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.ocorrencia.avulsa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(OCORRENCIAS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Manutencao residencial","competencia":"2026-08","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","valorPrincipal":100.00,"vencimento":"2026-08-25",
						 "juros":5.00,"multa":10.00,"desconto":3.00}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.valorTotal").value(112.00))
				.andExpect(jsonPath("$.saldoPendente").value(112.00))
				.andExpect(jsonPath("$.compromissoId").doesNotExist());
	}

	@Test
	void vencimentoAusenteRetorna400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("77777777000417");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Manutencao", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.ocorrencia.sem.vencimento@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(OCORRENCIAS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Sem vencimento","competencia":"2026-08","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","valorPrincipal":100.00}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveCancelarOcorrenciaEImpedirNovosPagamentos() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("88888888000418");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Diversos", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.ocorrencia.cancelar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarOcorrenciaAvulsa(session, categoria, pessoa, "Obrigacao a cancelar", "50.00", "2026-08-10");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataPagamento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isConflict());
	}

	/* ==================== INTEGRACAO COM RECORRENCIA ==================== */

	@Test
	void deveGerarOcorrenciaPorRecorrenciaAvancandoCompetenciaSemDuplicar() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("99999999000419");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Internet", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.compromisso.recorrente@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		String recorrenciaId = criarRecorrencia(session, conta, categoria, pessoa, "Internet mensal", "120.00");
		String compromissoId = vincularCompromissoARecorrencia(session, categoria, pessoa, recorrenciaId);

		mockMvc.perform(post(OCORRENCIAS + "/gerar-por-compromisso/" + compromissoId).session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recorrenciaId").value(recorrenciaId))
				.andExpect(jsonPath("$.compromissoId").value(compromissoId))
				.andExpect(jsonPath("$.valorPrincipal").value(120.00));

		mockMvc.perform(get(OCORRENCIAS).session(session).param("compromissoId", compromissoId))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

		// A proxima chamada gera a competencia seguinte (a recorrencia avancou), nunca duplica a mesma competencia
		// (uq_ocorrencia_compromisso_recorrencia_competencia, provada em ContasAPagarJpaTests).
		mockMvc.perform(post(OCORRENCIAS + "/gerar-por-compromisso/" + compromissoId).session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.competencia").value("2026-02"));
		mockMvc.perform(get(OCORRENCIAS).session(session).param("compromissoId", compromissoId))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

		// Uma recorrencia vinculada a um compromisso nao pode mais gerar lancamento direto pelo fluxo antigo.
		mockMvc.perform(post(RECORRENCIAS + "/" + recorrenciaId + "/gerar").session(session).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	/* ==================== PAGAMENTO ==================== */

	@Test
	void pagamentoIntegralGeraLancamentoLiquidadoEQuitaOcorrencia() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("10101010000420");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Agua", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.pagamento.integral@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarOcorrenciaAvulsa(session, categoria, pessoa, "Agua", "80.00", "2026-08-20");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataPagamento":"2026-08-18","formaPagamento":"PIX"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.valor").value(80.00));

		mockMvc.perform(get(OCORRENCIAS + "/" + id).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAGA"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));
	}

	@Test
	void pagamentosParciaisMultiplosAteQuitarERejeitaExcedente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("12121212000421");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Plano de saude", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.pagamento.parcial@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarOcorrenciaAvulsa(session, categoria, pessoa, "Plano de saude", "500.00", "2026-08-20");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":600.00,"dataPagamento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":200.00,"dataPagamento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(OCORRENCIAS + "/" + id).session(session))
				.andExpect(jsonPath("$.status").value("PARCIALMENTE_PAGA"))
				.andExpect(jsonPath("$.saldoPendente").value(300.00));

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":300.00,"dataPagamento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(OCORRENCIAS + "/" + id).session(session))
				.andExpect(jsonPath("$.status").value("PAGA"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));

		mockMvc.perform(get(OCORRENCIAS + "/" + id + "/pagamentos").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void contaDeOutroTenantERejeitadaNoPagamento() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("13131313000422");
		Empresa b = criarEmpresaComFinanceiro("14141414000423");
		Usuario adminA = criarUsuario("cap.pagamento.tenant.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("cap.pagamento.tenant.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaB = criarConta(b);
		CategoriaFinanceira categoriaA = criarCategoria(a, "Diversos", TipoFinanceiro.DESPESA);
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		String id = criarOcorrenciaAvulsa(sessionA, categoriaA, pessoaA, "Obrigacao A", "90.00", "2026-08-20");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-integral").session(sessionA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataPagamento":"2026-08-10"}
						""".formatted(contaB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void estornoRevertaImpactoERecalculaStatus() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("15151515000424");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Internet", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.estorno@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarOcorrenciaAvulsa(session, categoria, pessoa, "Internet", "120.00", "2026-08-20");

		MvcResult resultado = mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-integral").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataPagamento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();
		String pagamentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagamentos/" + pagamentoId + "/estornar").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"motivo":"Pagamento em duplicidade"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ESTORNADO"));

		mockMvc.perform(get(OCORRENCIAS + "/" + id).session(session))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.saldoPendente").value(120.00));

		// Estornar novamente o mesmo pagamento e rejeitado.
		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagamentos/" + pagamentoId + "/estornar").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void estornoExigePerfilAdministrador() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("16161616000425");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Internet", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.estorno.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario gestor = criarUsuario("cap.estorno.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession sessionAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		MockHttpSession sessionGestor = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());
		String id = criarOcorrenciaAvulsa(sessionAdmin, categoria, pessoa, "Internet", "60.00", "2026-08-20");

		MvcResult resultado = mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagar-integral").session(sessionAdmin)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataPagamento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();
		String pagamentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(OCORRENCIAS + "/" + id + "/pagamentos/" + pagamentoId + "/estornar")
				.session(sessionGestor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	/* ==================== RESUMO / CALENDARIO / SEGURANCA ==================== */

	@Test
	void resumoRetornaAgregadosDoPeriodo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("17171717000426");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Diversos", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.resumo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarOcorrenciaAvulsa(session, categoria, pessoa, "Obrigacao 1", "100.00", "2026-08-05");
		criarOcorrenciaAvulsa(session, categoria, pessoa, "Obrigacao 2", "50.00", "2026-08-10");

		mockMvc.perform(get(OCORRENCIAS + "/resumo").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPrincipal").value(150.00))
				.andExpect(jsonPath("$.quantidadePendente").value(2));
	}

	@Test
	void calendarioListaOcorrenciasDoMes() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("18181818000427");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Diversos", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.calendario@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarOcorrenciaAvulsa(session, categoria, pessoa, "Obrigacao agosto", "70.00", "2026-08-12");

		mockMvc.perform(get(OCORRENCIAS + "/calendario").session(session).param("mes", "2026-08"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].vencimento").value("2026-08-12"));
	}

	@Test
	void endpointsExigemAutenticacao() throws Exception {
		mockMvc.perform(get(COMPROMISSOS)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(OCORRENCIAS)).andExpect(status().isUnauthorized());
	}

	@Test
	void escritaSemTokenCsrfERejeitada() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("19191919000428");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Diversos", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.csrf@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPROMISSOS).session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Sem csrf","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"FIXO"}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumNaoPodeCriarCompromisso() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("20202020000429");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Diversos", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("cap.perfil.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario comum = criarUsuario("cap.perfil.usuario@criati.test");
		criarVinculo(comum, empresa, PerfilUsuario.USUARIO);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(comum.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Bloqueado","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"FIXO"}
						""".formatted(categoria.getId(), pessoa.getId())))
				.andExpect(status().isForbidden());
	}

	/* ==================== HELPERS ==================== */

	private String criarCompromissoAvulso(MockHttpSession session, CategoriaFinanceira categoria,
			PessoaFinanceira pessoa, String descricao) throws Exception {
		MvcResult resultado = mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"FIXO"}
						""".formatted(descricao, categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String vincularCompromissoARecorrencia(MockHttpSession session, CategoriaFinanceira categoria,
			PessoaFinanceira pessoa, String recorrenciaId) throws Exception {
		MvcResult resultado = mockMvc.perform(post(COMPROMISSOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Internet","categoriaId":"%s","pessoaFinanceiraId":"%s","tipoValor":"FIXO",
						 "recorrenciaId":"%s"}
						""".formatted(categoria.getId(), pessoa.getId(), recorrenciaId)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarOcorrenciaAvulsa(MockHttpSession session, CategoriaFinanceira categoria,
			PessoaFinanceira pessoa, String descricao, String valor, String vencimento) throws Exception {
		MvcResult resultado = mockMvc.perform(post(OCORRENCIAS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"%s","competencia":"2026-08","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "valorPrincipal":%s,"vencimento":"%s"}
						""".formatted(descricao, categoria.getId(), pessoa.getId(), valor, vencimento)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarRecorrencia(MockHttpSession session, ContaFinanceira conta, CategoriaFinanceira categoria,
			PessoaFinanceira pessoa, String descricao, String valor) throws Exception {
		MvcResult resultado = mockMvc.perform(post(RECORRENCIAS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"DESPESA","descricao":"%s","valorPadrao":%s,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":10,
						 "dataInicial":"2026-01-01","gerarAutomaticamente":false}
						""".formatted(descricao, valor, conta.getId(), categoria.getId(), pessoa.getId()))
				)
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaFinanceiraRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
	}

	private ContaFinanceira criarConta(Empresa empresa) {
		return contaFinanceiraRepository.saveAndFlush(
				new ContaFinanceira(empresa, "Conta", TipoContaFinanceira.CAIXA, java.math.BigDecimal.ZERO, StatusCadastro.ATIVO));
	}

	private CategoriaFinanceira criarCategoria(Empresa empresa, String nome, TipoFinanceiro tipo) {
		return categoriaFinanceiraRepository.saveAndFlush(new CategoriaFinanceira(empresa, nome, tipo, StatusCadastro.ATIVO));
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
