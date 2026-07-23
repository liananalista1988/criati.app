package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
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
class EmprestimosConcedidosControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String EMPRESTIMOS = "/api/contexto/financeiro/emprestimos-concedidos";
	private static final String PARCELAS = "/api/contexto/financeiro/parcelas-emprestimo";

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
	private ParteFinanceiraRepository parteFinanceiraRepository;
	@Autowired
	private AplicacaoService aplicacaoService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/* ==================== CADASTRO ==================== */

	@Test
	void deveCriarEmprestimoUnicoEGerarUmaParcela() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000511");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.unico@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","descricao":"Ajuda emergencial",
						 "valorPrincipal":500.00,"dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS",
						 "formaPagamento":"UNICO","quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.quantidadeParcelas").value(1))
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(EMPRESTIMOS + "/" + id + "/parcelas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].numero").value(1))
				.andExpect(jsonPath("$[0].valorPrincipal").value(500.00))
				.andExpect(jsonPath("$[0].vencimento").value("2026-09-01"))
				.andExpect(jsonPath("$[0].status").value("PENDENTE"));
	}

	@Test
	void deveCriarEmprestimoParceladoDividindoValorCorretamente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000512");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.parcelado@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS","formaPagamento":"PARCELADO",
						 "quantidadeParcelas":3}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantidadeParcelas").value(3))
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(EMPRESTIMOS + "/" + id + "/parcelas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].valorPrincipal").value(33.33))
				.andExpect(jsonPath("$[0].vencimento").value("2026-09-01"))
				.andExpect(jsonPath("$[1].valorPrincipal").value(33.33))
				.andExpect(jsonPath("$[1].vencimento").value("2026-10-01"))
				.andExpect(jsonPath("$[2].valorPrincipal").value(33.34))
				.andExpect(jsonPath("$[2].vencimento").value("2026-11-01"));
	}

	@Test
	void categoriaDeDespesaERejeitadaParaEmprestimo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000513");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Mercado", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("emp.categoria.invalida@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":1}
						""".formatted(parte.getId(), categoriaDespesa.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void comJurosSemPercentualERejeitado() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000514");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.juros.sem.percentual@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"COM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void pagamentoUnicoComMaisDeUmaParcelaERejeitado() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000515");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.unico.invalido@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":3}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveCancelarEmprestimoECascatearCancelamentoDeParcelasNaoPagas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000516");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.cancelar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarEmprestimoParcelado(session, categoria, parte, "200.00", "2026-08-01", 2);

		mockMvc.perform(post(EMPRESTIMOS + "/" + id + "/cancelar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"motivo":"Acordo desfeito"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELADO"));

		mockMvc.perform(get(EMPRESTIMOS + "/" + id + "/parcelas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("CANCELADO"))
				.andExpect(jsonPath("$[1].status").value("CANCELADO"));
	}

	/* ==================== RECEBIMENTO ==================== */

	@Test
	void recebimentoIntegralGeraLancamentoLiquidadoEQuitaParcelaEEmprestimo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("77777777000517");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.recebimento.integral@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "300.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-25","formaPagamento":"PIX"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.valor").value(300.00));

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAGO"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));

		mockMvc.perform(get(EMPRESTIMOS + "/" + emprestimoId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("QUITADO"));
	}

	@Test
	void recebimentosParciaisMultiplosAteQuitarERejeitaExcedente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("88888888000518");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.recebimento.parcial@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "500.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":600.00,"dataRecebimento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":200.00,"dataRecebimento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.status").value("PARCIALMENTE_PAGO"))
				.andExpect(jsonPath("$.saldoPendente").value(300.00));

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":300.00,"dataRecebimento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.status").value("PAGO"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));

		mockMvc.perform(get(PARCELAS + "/" + parcelaId + "/recebimentos").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void jurosComJurosAcresceMesmoAntesDoVencimentoEnquantoSemJurosNuncaAcresce() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000519");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.juros@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":1000.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"COM_JUROS","percentualJuros":3.00,
						 "formaPagamento":"UNICO","quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isCreated()).andReturn();
		String emprestimoComJuros = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		String parcelaComJuros = buscarPrimeiraParcela(session, emprestimoComJuros);

		// Recebido 15 dias apos a concessao, ainda antes do vencimento (2026-09-01):
		// 1000 * 0.03/30 * 15 = 15.00 de juros, mesmo sem atraso.
		mockMvc.perform(post(PARCELAS + "/" + parcelaComJuros + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-16"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valor").value(1015.00));

		String emprestimoSemJuros = criarEmprestimoUnico(session, categoria, parte, "1000.00", "2026-08-01");
		String parcelaSemJuros = buscarPrimeiraParcela(session, emprestimoSemJuros);

		// Recebido bem depois do vencimento: sem configuracao de juros/multa, valor permanece o principal.
		mockMvc.perform(post(PARCELAS + "/" + parcelaSemJuros + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-10-15"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valor").value(1000.00));
	}

	@Test
	void multaAtrasoSoAcresceAposVencimento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000520");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.multa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":200.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"MULTA_ATRASO","percentualMulta":10.00,
						 "formaPagamento":"UNICO","quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isCreated()).andReturn();
		String emprestimoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		// Vencimento e 2026-09-01: recebido um dia depois gera multa unica de 10% sobre o principal.
		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-09-02"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valor").value(220.00));
	}

	@Test
	void dataPrometidaNaoAlteraVencimentoOriginal() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000521");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.data.prometida@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "150.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		mockMvc.perform(put(PARCELAS + "/" + parcelaId + "/data-prometida").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"dataPrometida":"2026-09-20"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataPrometida").value("2026-09-20"))
				.andExpect(jsonPath("$.vencimento").value("2026-09-01"));
	}

	@Test
	void parcelasVencidasSaoIdentificadas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000522");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.vencidas@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		// Concedido em 2026-01-01: vencimento unico em 2026-02-01, ja passado.
		criarEmprestimoUnico(session, categoria, parte, "80.00", "2026-01-01");

		mockMvc.perform(get(PARCELAS + "/vencidas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].situacao").value("ATRASADO"));
	}

	@Test
	void estornoRevertaImpactoEReabreEmprestimoQuitado() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000523");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.estorno@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "120.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		MvcResult resultado = mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-20"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();
		String recebimentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(EMPRESTIMOS + "/" + emprestimoId).session(session))
				.andExpect(jsonPath("$.status").value("QUITADO"));

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/recebimentos/" + recebimentoId + "/estornar")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"motivo":"Recebimento em duplicidade"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ESTORNADO"));

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.saldoPendente").value(120.00));

		mockMvc.perform(get(EMPRESTIMOS + "/" + emprestimoId).session(session))
				.andExpect(jsonPath("$.status").value("ATIVO"));

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/recebimentos/" + recebimentoId + "/estornar")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void estornoExigePerfilAdministrador() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000524");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.estorno.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario gestor = criarUsuario("emp.estorno.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession sessionAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		MockHttpSession sessionGestor = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(sessionAdmin, categoria, parte, "60.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(sessionAdmin, emprestimoId);

		MvcResult resultado = mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral")
				.session(sessionAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-20"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();
		String recebimentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/recebimentos/" + recebimentoId + "/estornar")
				.session(sessionGestor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void resumoRetornaAgregados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000525");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.resumo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarEmprestimoUnico(session, categoria, parte, "100.00", "2026-08-01");
		criarEmprestimoUnico(session, categoria, parte, "50.00", "2026-08-05");

		mockMvc.perform(get(PARCELAS + "/resumo").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPrincipal").value(150.00))
				.andExpect(jsonPath("$.quantidadePendente").value(2));
	}

	/* ==================== CRIATI-FIN-010A: CONCORRENCIA E REGRESSAO ==================== */

	@Test
	void parcelaQuitadaRejeitaNovoRecebimento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("92222222000601");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.quitada.rejeita@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "80.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-20"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.status").value("PAGO"));

		// Parcela ja quitada: nem integral nem parcial podem gerar um novo recebimento.
		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","dataRecebimento":"2026-08-21"}
						""".formatted(conta.getId())))
				.andExpect(status().isConflict());

		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":1.00,"dataRecebimento":"2026-08-21"}
						""".formatted(conta.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId + "/recebimentos").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void segundoRecebimentoRespeitaSaldoAposPrimeiraLiquidacaoParcial() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("92222222000602");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.saldo.pos.parcial@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "100.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		// Primeira liquidacao parcial consome 60 dos 100: saldo cai para 40.
		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":60.00,"dataRecebimento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.saldoPendente").value(40.00));

		// 60.00 cabia no saldo ORIGINAL (100) mas nao cabe mais no saldo ATUAL (40):
		// a segunda liquidacao deve respeitar o saldo recalculado, nao o original.
		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":60.00,"dataRecebimento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isBadRequest());

		// 40.00 cabe exatamente no saldo atual e quita a parcela.
		mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":40.00,"dataRecebimento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/" + parcelaId).session(session))
				.andExpect(jsonPath("$.status").value("PAGO"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));
	}

	@Test
	void cadaRecebimentoGeraLancamentoFinanceiroDistintoEExclusivo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("92222222000603");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.lancamento.exclusivo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimoId = criarEmprestimoUnico(session, categoria, parte, "200.00", "2026-08-01");
		String parcelaId = buscarPrimeiraParcela(session, emprestimoId);

		MvcResult primeiro = mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":120.00,"dataRecebimento":"2026-08-05"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();
		MvcResult segundo = mockMvc.perform(post(PARCELAS + "/" + parcelaId + "/receber-parcial").session(session)
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":80.00,"dataRecebimento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated()).andReturn();

		String lancamento1 = com.jayway.jsonpath.JsonPath.read(primeiro.getResponse().getContentAsString(),
				"$.lancamentoFinanceiroId");
		String lancamento2 = com.jayway.jsonpath.JsonPath.read(segundo.getResponse().getContentAsString(),
				"$.lancamentoFinanceiroId");

		assertThat(lancamento1).isNotNull();
		assertThat(lancamento2).isNotNull();
		assertThat(lancamento1).isNotEqualTo(lancamento2);

		mockMvc.perform(get(PARCELAS + "/" + parcelaId + "/recebimentos").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].lancamentoFinanceiroId").exists())
				.andExpect(jsonPath("$[1].lancamentoFinanceiroId").exists());
	}

	@Test
	void parcelasProximasDoVencimentoSaoListadas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("92222222000604");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.proximas.vencimento@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		// Vencimento = dataConcessao + 1 mes. Usa a data real do sistema para que o
		// teste continue valido independentemente de quando for executado: uma
		// parcela vencendo daqui a 2 dias (dentro da janela padrao de 3 dias de
		// antecedencia) e outra vencendo daqui a 20 dias (fora da janela).
		LocalDate hoje = LocalDate.now();
		String dataConcessaoProxima = hoje.plusDays(2).minusMonths(1).toString();
		String dataConcessaoDistante = hoje.plusDays(20).minusMonths(1).toString();
		String emprestimoProximo = criarEmprestimoUnico(session, categoria, parte, "70.00", dataConcessaoProxima);
		criarEmprestimoUnico(session, categoria, parte, "90.00", dataConcessaoDistante);
		String parcelaProximaId = buscarPrimeiraParcela(session, emprestimoProximo);

		mockMvc.perform(get(PARCELAS + "/proximas-vencimento").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(parcelaProximaId))
				.andExpect(jsonPath("$[0].valorPrincipal").value(70.00));
	}

	@Test
	void resumoRetornaTotalRecebidoESaldoAReceberComValoresConcretos() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("92222222000605");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.resumo.valores@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String emprestimo1 = criarEmprestimoUnico(session, categoria, parte, "300.00", "2026-08-01");
		String emprestimo2 = criarEmprestimoUnico(session, categoria, parte, "100.00", "2026-08-05");
		String parcela1 = buscarPrimeiraParcela(session, emprestimo1);

		// Recebe 120.00 dos 300.00 do primeiro emprestimo; o segundo (100.00) fica
		// inteiramente em aberto. Total principal = 400.00, total recebido = 120.00,
		// saldo a receber = 280.00 (300-120 + 100).
		mockMvc.perform(post(PARCELAS + "/" + parcela1 + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","valor":120.00,"dataRecebimento":"2026-08-10"}
						""".formatted(conta.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(PARCELAS + "/resumo").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPrincipal").value(400.00))
				.andExpect(jsonPath("$.totalRecebido").value(120.00))
				.andExpect(jsonPath("$.saldoAReceber").value(280.00))
				.andExpect(jsonPath("$.quantidadeParcialmentePaga").value(1))
				.andExpect(jsonPath("$.quantidadePendente").value(1));
	}

	/* ==================== SEGURANCA / MULTIEMPRESA ==================== */

	@Test
	void emprestimoDeOutroTenantResultaEmNaoEncontrado() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("91111111000526");
		Empresa b = criarEmpresaComFinanceiro("91111111000527");
		Usuario adminA = criarUsuario("emp.tenant.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("emp.tenant.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoriaA = criarCategoria(a, "Emprestimos", TipoFinanceiro.RECEITA);
		ParteFinanceira parteA = criarParte(a, adminA, "Amigo A");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		MockHttpSession sessionB = autenticarNaEmpresa(adminB.getEmail(), b.getId());
		String idA = criarEmprestimoUnico(sessionA, categoriaA, parteA, "100.00", "2026-08-01");

		mockMvc.perform(get(EMPRESTIMOS + "/" + idA).session(sessionB)).andExpect(status().isNotFound());
	}

	@Test
	void endpointsExigemAutenticacao() throws Exception {
		mockMvc.perform(get(EMPRESTIMOS)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(PARCELAS)).andExpect(status().isUnauthorized());
	}

	@Test
	void escritaSemTokenCsrfERejeitada() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000528");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.csrf@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(EMPRESTIMOS).session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumNaoPodeCriarEmprestimo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("91111111000529");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Emprestimos", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("emp.perfil.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario comum = criarUsuario("emp.perfil.usuario@criati.test");
		criarVinculo(comum, empresa, PerfilUsuario.USUARIO);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(comum.getEmail(), empresa.getId());

		mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":100.00,
						 "dataConcessao":"2026-08-01","tipoCobranca":"SEM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId())))
				.andExpect(status().isForbidden());
	}

	/* ==================== HELPERS ==================== */

	private String criarEmprestimoUnico(MockHttpSession session, CategoriaFinanceira categoria, ParteFinanceira parte,
			String valor, String dataConcessao) throws Exception {
		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":%s,
						 "dataConcessao":"%s","tipoCobranca":"SEM_JUROS","formaPagamento":"UNICO",
						 "quantidadeParcelas":1}
						""".formatted(parte.getId(), categoria.getId(), valor, dataConcessao)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarEmprestimoParcelado(MockHttpSession session, CategoriaFinanceira categoria, ParteFinanceira parte,
			String valor, String dataConcessao, int quantidadeParcelas) throws Exception {
		MvcResult resultado = mockMvc.perform(post(EMPRESTIMOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"parteFinanceiraId":"%s","categoriaId":"%s","valorPrincipal":%s,
						 "dataConcessao":"%s","tipoCobranca":"SEM_JUROS","formaPagamento":"PARCELADO",
						 "quantidadeParcelas":%d}
						""".formatted(parte.getId(), categoria.getId(), valor, dataConcessao, quantidadeParcelas)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String buscarPrimeiraParcela(MockHttpSession session, String emprestimoId) throws Exception {
		MvcResult resultado = mockMvc.perform(get(EMPRESTIMOS + "/" + emprestimoId + "/parcelas").session(session))
				.andExpect(status().isOk()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$[0].id");
	}

	private ParteFinanceira criarParte(Empresa empresa, Usuario autor, String nome) {
		return parteFinanceiraRepository.saveAndFlush(
				new ParteFinanceira(empresa, nome, TipoParteFinanceira.PESSOA, null, null, null, autor));
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
