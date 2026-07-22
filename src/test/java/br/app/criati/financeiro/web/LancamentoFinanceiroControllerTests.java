package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import org.springframework.test.util.ReflectionTestUtils;
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
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
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
class LancamentoFinanceiroControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/financeiro/lancamentos";

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
	private ParteFinanceiraRepository parteFinanceiraRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarReceitaPendente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000211");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.receita.pendente@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Venda 1", "valor": 150.00, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.dataPagamento").doesNotExist());
	}

	@Test
	void deveCriarDespesaPendente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000212");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Aluguel", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("lanc.despesa.pendente@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "DESPESA",
						  "descricao": "Aluguel loja", "valor": 800.00, "dataCompetencia": "2026-07-05"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDENTE"));
	}

	@Test
	void deveCriarLancamentoJaPago() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000213");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.ja.pago@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Venda a vista", "valor": 300.00, "dataCompetencia": "2026-07-01",
						  "status": "PAGO", "dataPagamento": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PAGO"))
				.andExpect(jsonPath("$.dataPagamento").value("2026-07-01"));
	}

	@Test
	void valorZeroOuNegativoRetorna400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000214");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.valor.invalido@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Invalido", "valor": 0, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Invalido", "valor": -10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void dataCompetenciaAusenteRetorna400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000215");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.sem.data@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA", "descricao": "Sem data", "valor": 10 }
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void contaDeOutraEmpresaRetorna404() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("66666666000216");
		Empresa empresaB = criarEmpresaComFinanceiro("77777777000217");
		ContaFinanceira contaB = criarConta(empresaB, "Caixa B");
		CategoriaFinanceira categoriaA = criarCategoria(empresaA, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.conta.outra.empresa@criati.test");
		criarVinculo(admin, empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresaA.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Tentativa cross-tenant", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(contaB.getId(), categoriaA.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void categoriaDeOutraEmpresaRetorna404() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("88888888000218");
		Empresa empresaB = criarEmpresaComFinanceiro("11111111000221");
		ContaFinanceira contaA = criarConta(empresaA, "Caixa A");
		CategoriaFinanceira categoriaB = criarCategoria(empresaB, "Categoria B", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.categoria.outra.empresa@criati.test");
		criarVinculo(admin, empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresaA.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Tentativa cross-tenant", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(contaA.getId(), categoriaB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void contaInativaNaoAceitaLancamento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000222");
		ContaFinanceira conta = criarConta(empresa, "Caixa Inativa");
		ReflectionTestUtils.setField(conta, "status", StatusCadastro.INATIVO);
		contaFinanceiraRepository.saveAndFlush(conta);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.conta.inativa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Bloqueado", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Conta financeira inativa"));
	}

	@Test
	void categoriaInativaNaoAceitaLancamento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000223");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Categoria Inativa", TipoFinanceiro.RECEITA);
		ReflectionTestUtils.setField(categoria, "status", StatusCadastro.INATIVO);
		categoriaFinanceiraRepository.saveAndFlush(categoria);
		Usuario admin = criarUsuario("lanc.categoria.inativa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Bloqueado", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Categoria financeira inativa"));
	}

	@Test
	void tipoIncompativelComCategoriaRetorna400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000224");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Aluguel", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("lanc.tipo.incompativel@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Incompativel", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoriaDespesa.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void devePagarReabrirECancelarComIdempotenciaDeConflito() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000225");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.ciclo.completo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult criado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Ciclo", "valor": 100.00, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE + "/" + id + "/pagar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "dataPagamento": "2026-07-02" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAGO"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/pagar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "dataPagamento": "2026-07-02" }
						"""))
				.andExpect(status().isConflict());

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.dataPagamento").doesNotExist());

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isConflict());

		mockMvc.perform(post(URL_BASE + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void gestorPagaMasNaoReabre() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000226");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario gestor = criarUsuario("lanc.gestor.paga@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		MockHttpSession session = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		MvcResult criado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Gestor cria", "valor": 50, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE + "/" + id + "/pagar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "dataPagamento": "2026-07-02" }
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumNaoPodeCriarLancamento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("77777777000227");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario usuario = criarUsuario("lanc.usuario.comum@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Bloqueado", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void naoPermiteEditarLancamentoCancelado() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("88888888000228");
		ContaFinanceira conta = criarConta(empresa, "Caixa");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("lanc.editar.cancelado@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult criado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s", "tipo": "RECEITA",
						  "descricao": "Sera cancelado", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(URL_BASE + "/" + id)
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "contaId": "%s", "categoriaId": "%s",
						  "descricao": "Tentativa de editar cancelado", "valor": 10, "dataCompetencia": "2026-07-01"
						}
						""".formatted(conta.getId(), categoria.getId())))
				.andExpect(status().isConflict());
	}

	@Test
	void saldoAtualDaContaReflitoSomenteLancamentosPagos() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000231");
		ContaFinanceira conta = criarConta(empresa, "Caixa Saldo");
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Compras", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("lanc.saldo.conta@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		criarLancamento(session, conta, categoriaReceita, "RECEITA", "300.00", "PAGO", "2026-07-01");
		criarLancamento(session, conta, categoriaDespesa, "DESPESA", "50.00", "PAGO", "2026-07-01");
		criarLancamento(session, conta, categoriaReceita, "RECEITA", "999.00", "PENDENTE", null);
		String idCancelado = criarLancamento(session, conta, categoriaReceita, "RECEITA", "777.00", "PENDENTE", null);
		mockMvc.perform(post(URL_BASE + "/" + idCancelado + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/financeiro/contas/" + conta.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saldoAtual").value(250.00));
	}

	@Test
	void anonimoRecebe401() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
	}

	@Test
	void criaLancamentoLiquidadoComPessoaParteDatasFormaOrigemEAuditoria() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22111111000231");
		Usuario admin = criarUsuario("lanc.f2005.completo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		ParteFinanceira parte = criarParte(empresa, admin, "Empregador");
		ContaFinanceira conta = criarConta(empresa, "Conta A");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salário", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s","parteFinanceiraId":"%s",
						 "tipo":"RECEITA","descricao":"  Salário   mensal ","valor":125.555,
						 "dataCompetencia":"2026-07-01","dataVencimento":"2026-07-05",
						 "status":"LIQUIDADO","dataLiquidacao":"2026-07-04","formaPagamento":"PIX"}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId(), parte.getId())))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("LIQUIDADO"))
				.andExpect(jsonPath("$.descricao").value("Salário mensal"))
				.andExpect(jsonPath("$.valor").value(125.56)).andExpect(jsonPath("$.pessoaFinanceiraNome").value("Pessoa A"))
				.andExpect(jsonPath("$.parteFinanceiraNome").value("Empregador"))
				.andExpect(jsonPath("$.dataVencimento").value("2026-07-05"))
				.andExpect(jsonPath("$.dataLiquidacao").value("2026-07-04"))
				.andExpect(jsonPath("$.formaPagamento").value("PIX"))
				.andExpect(jsonPath("$.origem").value("MANUAL"))
				.andExpect(jsonPath("$.impactoSaldo").value(125.56))
				.andExpect(jsonPath("$.criadoPorUsuarioId").value(admin.getId().toString()));
	}

	@Test
	void liquidarDesliquidarECancelarReverteSaldoSemDuplicar() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("23111111000232");
		Usuario admin = criarUsuario("lanc.f2005.ciclo@criati.test"); criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa"); ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Renda", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarLancamentoNovo(session, conta, categoria, pessoa, "RECEITA", "100.00", "PENDENTE", null, null);

		mockMvc.perform(post(URL_BASE + "/" + id + "/liquidar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"dataLiquidacao\":\"2026-07-02\",\"formaPagamento\":\"PIX\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("LIQUIDADO"));
		assertSaldo(session, conta, 100.00);
		mockMvc.perform(post(URL_BASE + "/" + id + "/desliquidar").session(session).with(csrf())).andExpect(status().isOk());
		assertSaldo(session, conta, 0.00);
		mockMvc.perform(post(URL_BASE + "/" + id + "/liquidar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"dataLiquidacao\":\"2026-07-03\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post(URL_BASE + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dataLiquidacao").doesNotExist());
		assertSaldo(session, conta, 0.00);
	}

	@Test
	void rejeitaPessoaEParteDeOutroTenant() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("24111111000233"), b = criarEmpresaComFinanceiro("25111111000234");
		Usuario adminA = criarUsuario("lanc.f2005.tenant.a@criati.test"); criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("lanc.f2005.tenant.b@criati.test"); criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoaB = criarPessoa(b, adminB, "Pessoa B"); ParteFinanceira parteB = criarParte(b, adminB, "Parte B");
		ContaFinanceira conta = criarConta(a, "Conta A"); CategoriaFinanceira categoria = criarCategoria(a, "Renda", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		String base = "{\"contaId\":\"%s\",\"categoriaId\":\"%s\",\"tipo\":\"RECEITA\",\"descricao\":\"Teste\",\"valor\":10,\"dataCompetencia\":\"2026-07-01\"".formatted(conta.getId(), categoria.getId());
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(base + ",\"pessoaFinanceiraId\":\"" + pessoaB.getId() + "\"}")) .andExpect(status().isNotFound());
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(base + ",\"pessoaFinanceiraId\":\"" + pessoaA.getId() + "\",\"parteFinanceiraId\":\"" + parteB.getId() + "\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void filtraPessoaParteVencidoECalculaResumoDaCompetencia() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("26111111000235"); Usuario admin = criarUsuario("lanc.f2005.filtros@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR); PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa");
		ParteFinanceira parte = criarParte(empresa, admin, "Mercado"); ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira receita = criarCategoria(empresa, "Renda", TipoFinanceiro.RECEITA);
		CategoriaFinanceira despesa = criarCategoria(empresa, "Mercado", TipoFinanceiro.DESPESA);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarLancamentoNovo(session, conta, receita, pessoa, "RECEITA", "500.00", "LIQUIDADO", "2026-07-02", null);
		criarLancamentoNovo(session, conta, despesa, pessoa, "DESPESA", "80.00", "PENDENTE", null, parte.getId());

		mockMvc.perform(get(URL_BASE).session(session).param("pessoaId", pessoa.getId().toString())
				.param("parteId", parte.getId().toString()).param("vencido", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].vencido").value(true));
		mockMvc.perform(get(URL_BASE + "/resumo").session(session).param("competencia", "2026-07")
				.param("pessoaId", pessoa.getId().toString())).andExpect(status().isOk())
				.andExpect(jsonPath("$.receitasLiquidadas").value(500.00))
				.andExpect(jsonPath("$.despesasPendentes").value(80.00))
				.andExpect(jsonPath("$.quantidadeVencidos").value(1))
				.andExpect(jsonPath("$.saldoConsolidado").value(500.00));
	}

	@Test
	void editarLiquidadoRecalculaValorEConta() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("27111111000236"); Usuario admin = criarUsuario("lanc.f2005.edicao@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR); PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa");
		ContaFinanceira contaA = criarConta(empresa, "Conta A"), contaB = criarConta(empresa, "Conta B");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Renda", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarLancamentoNovo(session, contaA, categoria, pessoa, "RECEITA", "100.00", "LIQUIDADO", "2026-07-02", null);
		mockMvc.perform(put(URL_BASE + "/" + id).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s","tipo":"RECEITA",
						 "descricao":"Renda corrigida","valor":150,"dataCompetencia":"2026-07-01","dataLiquidacao":"2026-07-02"}
						""".formatted(contaB.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.valor").value(150.00));
		assertSaldo(session, contaA, 0.00); assertSaldo(session, contaB, 150.00);
	}

	private String criarLancamentoNovo(MockHttpSession session, ContaFinanceira conta, CategoriaFinanceira categoria,
			PessoaFinanceira pessoa, String tipo, String valor, String statusLancamento, String liquidacao, UUID parteId) throws Exception {
		String corpo = "{\"contaId\":\"%s\",\"categoriaId\":\"%s\",\"pessoaFinanceiraId\":\"%s\",\"tipo\":\"%s\",\"descricao\":\"Teste F2-005\",\"valor\":%s,\"dataCompetencia\":\"2026-07-01\",\"dataVencimento\":\"2026-07-01\",\"status\":\"%s\"".formatted(conta.getId(), categoria.getId(), pessoa.getId(), tipo, valor, statusLancamento);
		if (liquidacao != null) corpo += ",\"dataLiquidacao\":\"" + liquidacao + "\"";
		if (parteId != null) corpo += ",\"parteFinanceiraId\":\"" + parteId + "\"";
		MvcResult r = mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo + "}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(r.getResponse().getContentAsString(), "$.id");
	}

	private void assertSaldo(MockHttpSession session, ContaFinanceira conta, double saldo) throws Exception {
		mockMvc.perform(get("/api/contexto/financeiro/contas/" + conta.getId()).session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saldoAtual").value(saldo));
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaFinanceiraRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private ParteFinanceira criarParte(Empresa empresa, Usuario autor, String nome) {
		return parteFinanceiraRepository.saveAndFlush(new ParteFinanceira(
				empresa, nome, TipoParteFinanceira.ESTABELECIMENTO, null, null, null, autor));
	}

	private String criarLancamento(
			MockHttpSession session,
			ContaFinanceira conta,
			CategoriaFinanceira categoria,
			String tipo,
			String valor,
			String status,
			String dataPagamento) throws Exception {
		StringBuilder corpo = new StringBuilder("{")
				.append("\"contaId\":\"").append(conta.getId()).append("\",")
				.append("\"categoriaId\":\"").append(categoria.getId()).append("\",")
				.append("\"tipo\":\"").append(tipo).append("\",")
				.append("\"descricao\":\"Lancamento teste\",")
				.append("\"valor\":").append(valor).append(",")
				.append("\"dataCompetencia\":\"2026-07-01\"");
		if (status != null) {
			corpo.append(",\"status\":\"").append(status).append("\"");
		}
		if (dataPagamento != null) {
			corpo.append(",\"dataPagamento\":\"").append(dataPagamento).append("\"");
		}
		corpo.append("}");

		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
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

	private ContaFinanceira criarConta(Empresa empresa, String nome) {
		return contaFinanceiraRepository.saveAndFlush(
				new ContaFinanceira(empresa, nome, TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO));
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
