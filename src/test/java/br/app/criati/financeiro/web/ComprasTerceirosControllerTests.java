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
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ComprasTerceirosControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String COMPRAS_CARTAO = "/api/contexto/financeiro/compras-cartao";
	private static final String COMPRAS_TERCEIROS = "/api/contexto/financeiro/compras-terceiros";
	private static final String VALORES_A_RECEBER = "/api/contexto/financeiro/valores-a-receber-cartao";
	private static final String LANCAMENTOS = "/api/contexto/financeiro/lancamentos";

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
	private PessoaFinanceiraRepository pessoaFinanceiraRepository;
	@Autowired
	private InstituicaoFinanceiraRepository instituicaoFinanceiraRepository;
	@Autowired
	private br.app.criati.financeiro.repository.CartaoCreditoRepository cartaoCreditoRepository;
	@Autowired
	private AplicacaoService aplicacaoService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	/* ==================== CADASTRO ==================== */

	@Test
	void compraDaResidenciaSemTerceiroContinuaFuncionandoSemParteFinanceira() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("31111111000501");
		Usuario admin = criarUsuario("terc.residencia@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Mercado", TipoFinanceiro.DESPESA);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPRAS_CARTAO).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","descricao":"Mercado",
						 "dataCompra":"2026-08-01","valorTotal":150.00,"quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.compra.parteFinanceiraId").doesNotExist());
	}

	@Test
	void compraParaTerceiroExigeParteFinanceira() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("32222222000502");
		Usuario admin = criarUsuario("terc.exige.parte@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","descricao":"Presente",
						 "dataCompra":"2026-08-01","valorTotal":150.00,"quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void parteFinanceiraDeOutroTenantERejeitada() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("33333333000503");
		Empresa empresaB = criarEmpresaComFinanceiro("34444444000504");
		Usuario adminA = criarUsuario("terc.tenant.a@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("terc.tenant.b@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoaA = criarPessoa(empresaA, adminA, "Morador A");
		CategoriaFinanceira categoriaA = criarCategoria(empresaA, "Presentes", TipoFinanceiro.DESPESA);
		String cartaoA = criarCartao(empresaA, adminA, "5000.00");
		ParteFinanceira parteB = criarParte(empresaB, adminB, "Amigo de B");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());

		mockMvc.perform(post(COMPRAS_TERCEIROS).session(sessionA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s",
						 "parteFinanceiraId":"%s","descricao":"Presente","dataCompra":"2026-08-01",
						 "valorTotal":150.00,"quantidadeParcelas":1}
						""".formatted(cartaoA, pessoaA.getId(), categoriaA.getId(), parteB.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void valorAReceberCorrespondeAoValorDaParcelaUnica() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("35555555000505");
		Usuario admin = criarUsuario("terc.valor.unico@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "300.00", "2026-08-01", 1);

		mockMvc.perform(get(COMPRAS_TERCEIROS + "/" + compraId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valoresAReceber.length()").value(1))
				.andExpect(jsonPath("$.valoresAReceber[0].valorTotal").value(300.00))
				.andExpect(jsonPath("$.valoresAReceber[0].saldoPendente").value(300.00))
				.andExpect(jsonPath("$.valoresAReceber[0].status").value("PENDENTE"));
	}

	@Test
	void parcelamentoPreservaSomaComArredondamentoDeterministico() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("36666666000506");
		Usuario admin = criarUsuario("terc.parcelamento@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "100.00", "2026-08-01", 3);

		mockMvc.perform(get(COMPRAS_TERCEIROS + "/" + compraId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valoresAReceber.length()").value(3))
				.andExpect(jsonPath("$.valoresAReceber[0].valorTotal").value(33.33))
				.andExpect(jsonPath("$.valoresAReceber[1].valorTotal").value(33.33))
				.andExpect(jsonPath("$.valoresAReceber[2].valorTotal").value(33.34));
	}

	@Test
	void cancelamentoPreservaHistoricoDosValoresAReceber() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("37777777000507");
		Usuario admin = criarUsuario("terc.cancelar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "200.00", "2026-08-01", 2);

		mockMvc.perform(post(COMPRAS_TERCEIROS + "/" + compraId + "/cancelar").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"motivo":"Cancelado a pedido"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("CANCELADA"))
				.andExpect(jsonPath("$[1].status").value("CANCELADA"));

		// registro preservado, apenas com status alterado — nenhuma exclusao.
		mockMvc.perform(get(COMPRAS_TERCEIROS + "/" + compraId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valoresAReceber.length()").value(2));
	}

	/* ==================== RESSARCIMENTO ==================== */

	@Test
	void ressarcimentoIntegralGeraLancamentoDeReceitaEQuitaOValor() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("38888888000508");
		Usuario admin = criarUsuario("terc.ressarc.integral@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "300.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(session, compraId);

		MvcResult resultado = mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-integral")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","dataRessarcimento":"2026-08-25","formaPagamento":"PIX"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valor").value(300.00))
				.andReturn();
		String lancamentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(),
				"$.lancamentoFinanceiroId");
		assertThatLancamentoNaoENulo(lancamentoId);

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESSARCIDA"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));
	}

	@Test
	void ressarcimentosParciaisMultiplosAteQuitarERejeitaExcedente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("39999999000509");
		Usuario admin = criarUsuario("terc.ressarc.parcial@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "500.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(session, compraId);

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","valor":600.00,"dataRessarcimento":"2026-08-05"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","valor":0,"dataRessarcimento":"2026-08-05"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","valor":200.00,"dataRessarcimento":"2026-08-05"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId).session(session))
				.andExpect(jsonPath("$.status").value("PARCIALMENTE_RESSARCIDA"))
				.andExpect(jsonPath("$.saldoPendente").value(300.00));

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","valor":300.00,"dataRessarcimento":"2026-08-10"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId).session(session))
				.andExpect(jsonPath("$.status").value("RESSARCIDA"))
				.andExpect(jsonPath("$.saldoPendente").value(0.00));

		// segundo ressarcimento apos quitacao e rejeitado.
		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","dataRessarcimento":"2026-08-11"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isConflict());

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId + "/ressarcimentos").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void dataPrometidaNaoAlteraVencimentoOriginal() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("41111111000510");
		Usuario admin = criarUsuario("terc.data.prometida@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "150.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(session, compraId);

		mockMvc.perform(put(VALORES_A_RECEBER + "/" + valorId + "/data-prometida").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"dataPrometida":"2026-09-20"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataPrometida").value("2026-09-20"))
				// vencimento segue o ciclo de fechamento/vencimento do cartao (dia 5/dia 12),
				// nao "dataCompra + 1 mes": compra em 01/08 fecha em 05/08 (nao e posterior),
				// entao vence no proprio ciclo de agosto, dia 12.
				.andExpect(jsonPath("$.vencimento").value("2026-08-12"));
	}

	@Test
	void valoresAReceberVencidosSaoIdentificados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("42222222000511");
		Usuario admin = criarUsuario("terc.vencidas@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		// Cartao com fechamento dia 5 e vencimento dia 12: compra em 01/01/2026 nao
		// e posterior ao fechamento (05/01), entao vence no proprio ciclo de
		// janeiro/2026 — 12/01/2026, ja passado em relacao a "hoje" real.
		criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "80.00", "2026-01-01", 1);

		mockMvc.perform(get(VALORES_A_RECEBER + "/vencidas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].situacao").value("ATRASADA"));
	}

	@Test
	void valoresAReceberProximosDoVencimentoSaoListados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("43333333000512");
		Usuario admin = criarUsuario("terc.proximas@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		// Vencimento nao e "dataCompra + N dias": para controlar precisamente onde
		// cada parcela vence (dentro ou fora da janela padrao de 3 dias), cada
		// compra usa seu proprio cartao com diaFechamento=1 e diaVencimento igual
		// ao dia do mes do alvo desejado — ver dataCompraParaVencimento().
		LocalDate hoje = LocalDate.now();
		LocalDate alvoProximo = hoje.plusDays(2);
		LocalDate alvoDistante = hoje.plusDays(20);
		String cartaoProximo = criarCartao(empresa, admin, "5000.00", 1, alvoProximo.getDayOfMonth());
		String cartaoDistante = criarCartao(empresa, admin, "5000.00", 1, alvoDistante.getDayOfMonth());
		String compraProxima = criarCompraTerceiro(session, cartaoProximo, pessoa, categoria, parte, "70.00",
				dataCompraParaVencimento(alvoProximo).toString(), 1);
		criarCompraTerceiro(session, cartaoDistante, pessoa, categoria, parte, "90.00",
				dataCompraParaVencimento(alvoDistante).toString(), 1);
		String valorProximo = buscarPrimeiroValorAReceber(session, compraProxima);

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorProximo).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.vencimento").value(alvoProximo.toString()));

		mockMvc.perform(get(VALORES_A_RECEBER + "/proximas-vencimento").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(valorProximo));
	}

	@Test
	void resumoRetornaTotalRessarcidoESaldoAReceberComValoresConcretos() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000513");
		Usuario admin = criarUsuario("terc.resumo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compra1 = criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "300.00", "2026-08-01", 1);
		criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "100.00", "2026-08-05", 1);
		String valor1 = buscarPrimeiroValorAReceber(session, compra1);

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valor1 + "/receber-parcial").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","valor":120.00,"dataRessarcimento":"2026-08-10"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(VALORES_A_RECEBER + "/resumo").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalPrincipal").value(400.00))
				.andExpect(jsonPath("$.totalRessarcido").value(120.00))
				.andExpect(jsonPath("$.saldoAReceber").value(280.00))
				.andExpect(jsonPath("$.quantidadeParcialmenteRessarcida").value(1))
				.andExpect(jsonPath("$.quantidadePendente").value(1));
	}

	@Test
	void estornoRevertaImpactoEPreservaHistorico() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("45555555000514");
		Usuario admin = criarUsuario("terc.estorno@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "120.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(session, compraId);

		MvcResult resultado = mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-integral")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","dataRessarcimento":"2026-08-20"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated()).andReturn();
		String ressarcimentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/ressarcimentos/" + ressarcimentoId + "/estornar")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"motivo":"Ressarcimento em duplicidade"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ESTORNADO"));

		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId).session(session))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andExpect(jsonPath("$.saldoPendente").value(120.00));

		// registro do ressarcimento estornado continua visivel no historico, nao foi excluido.
		mockMvc.perform(get(VALORES_A_RECEBER + "/" + valorId + "/ressarcimentos").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("ESTORNADO"));

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/ressarcimentos/" + ressarcimentoId + "/estornar")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isConflict());
	}

	@Test
	void estornoExigePerfilAdministrador() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("46666666000515");
		Usuario admin = criarUsuario("terc.estorno.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario gestor = criarUsuario("terc.estorno.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession sessionAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		MockHttpSession sessionGestor = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(sessionAdmin, cartaoId, pessoa, categoriaDespesa, parte, "60.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(sessionAdmin, compraId);

		MvcResult resultado = mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-integral")
				.session(sessionAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","dataRessarcimento":"2026-08-20"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated()).andReturn();
		String ressarcimentoId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/ressarcimentos/" + ressarcimentoId + "/estornar")
				.session(sessionGestor).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	/* ==================== REGRA CONTABIL / IMPACTO CARTAO ==================== */

	@Test
	void compraParaTerceiroNaoGeraLancamentoDeDespesaDaResidencia() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("47777777000516");
		Usuario admin = criarUsuario("terc.sem.despesa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		criarCompraTerceiro(session, cartaoId, pessoa, categoria, parte, "300.00", "2026-08-01", 1);

		mockMvc.perform(get(LANCAMENTOS).session(session).param("tipo", "DESPESA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void ressarcimentoGeraLancamentoDeReceitaComOrigemDistinta() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("48888888000517");
		Usuario admin = criarUsuario("terc.origem.distinta@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Ressarcimentos", TipoFinanceiro.RECEITA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		ContaFinanceira conta = criarConta(empresa);
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String compraId = criarCompraTerceiro(session, cartaoId, pessoa, categoriaDespesa, parte, "150.00", "2026-08-01", 1);
		String valorId = buscarPrimeiroValorAReceber(session, compraId);

		mockMvc.perform(post(VALORES_A_RECEBER + "/" + valorId + "/receber-integral").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaId":"%s","categoriaId":"%s","dataRessarcimento":"2026-08-20"}
						""".formatted(conta.getId(), categoriaReceita.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(get(LANCAMENTOS).session(session).param("tipo", "RECEITA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].origem").value("RESSARCIMENTO_COMPRA_TERCEIRO"));
	}

	@Test
	void compraParaTerceiroImpactaLimiteDoCartaoNormalmente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("49999999000518");
		Usuario admin = criarUsuario("terc.limite@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "1000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","parteFinanceiraId":"%s",
						 "descricao":"Presente caro","dataCompra":"2026-08-01","valorTotal":900.00,
						 "quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId(), parte.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.limiteDisponivel").value(100.00));

		// segunda compra para terceiro que estouraria o limite (100 disponivel, pedindo 200) e rejeitada,
		// exatamente como uma compra normal de cartao.
		mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","parteFinanceiraId":"%s",
						 "descricao":"Outro presente","dataCompra":"2026-08-02","valorTotal":200.00,
						 "quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId(), parte.getId())))
				.andExpect(status().isBadRequest());
	}

	/* ==================== SEGURANCA / MULTIEMPRESA ==================== */

	@Test
	void compraParaTerceiroDeOutroTenantERejeitada() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("51111111000519");
		Empresa empresaB = criarEmpresaComFinanceiro("52222222000520");
		Usuario adminA = criarUsuario("terc.tenant.buscar.a@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("terc.tenant.buscar.b@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoaA = criarPessoa(empresaA, adminA, "Morador A");
		CategoriaFinanceira categoriaA = criarCategoria(empresaA, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parteA = criarParte(empresaA, adminA, "Amigo A");
		String cartaoA = criarCartao(empresaA, adminA, "5000.00");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessionB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());
		String compraId = criarCompraTerceiro(sessionA, cartaoA, pessoaA, categoriaA, parteA, "100.00", "2026-08-01", 1);

		// CompraTerceiroService.buscar reaproveita CompraCartaoService.buscar tal
		// qual (mesma busca ja usada por compras normais de cartao), incluindo sua
		// convencao de erro ja estabelecida: DadosInvalidosException (400), nao um
		// NaoEncontrado dedicado — o importante aqui e que o tenant B nunca acessa
		// o registro do tenant A, nao o codigo HTTP exato.
		mockMvc.perform(get(COMPRAS_TERCEIROS + "/" + compraId).session(sessionB)).andExpect(status().isBadRequest());
	}

	@Test
	void endpointsExigemAutenticacao() throws Exception {
		mockMvc.perform(get(COMPRAS_TERCEIROS)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(VALORES_A_RECEBER)).andExpect(status().isUnauthorized());
	}

	@Test
	void escritaSemTokenCsrfERejeitada() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("53333333000521");
		Usuario admin = criarUsuario("terc.csrf@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","parteFinanceiraId":"%s",
						 "descricao":"Presente","dataCompra":"2026-08-01","valorTotal":100.00,"quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId(), parte.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumNaoPodeRegistrarCompraParaTerceiro() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("54444444000522");
		Usuario admin = criarUsuario("terc.perfil.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario comum = criarUsuario("terc.perfil.usuario@criati.test");
		criarVinculo(comum, empresa, PerfilUsuario.USUARIO);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Morador");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Presentes", TipoFinanceiro.DESPESA);
		ParteFinanceira parte = criarParte(empresa, admin, "Amigo");
		String cartaoId = criarCartao(empresa, admin, "5000.00");
		MockHttpSession session = autenticarNaEmpresa(comum.getEmail(), empresa.getId());

		mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","parteFinanceiraId":"%s",
						 "descricao":"Presente","dataCompra":"2026-08-01","valorTotal":100.00,"quantidadeParcelas":1}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId(), parte.getId())))
				.andExpect(status().isForbidden());
	}

	/* ==================== HELPERS ==================== */

	private String criarCompraTerceiro(MockHttpSession session, String cartaoId, PessoaFinanceira pessoa,
			CategoriaFinanceira categoria, ParteFinanceira parte, String valor, String dataCompra, int quantidadeParcelas)
			throws Exception {
		MvcResult resultado = mockMvc.perform(post(COMPRAS_TERCEIROS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","pessoaResponsavelId":"%s","categoriaId":"%s","parteFinanceiraId":"%s",
						 "descricao":"Compra para terceiro","dataCompra":"%s","valorTotal":%s,
						 "quantidadeParcelas":%d}
						""".formatted(cartaoId, pessoa.getId(), categoria.getId(), parte.getId(), dataCompra, valor,
						quantidadeParcelas)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.compra.id");
	}

	private String buscarPrimeiroValorAReceber(MockHttpSession session, String compraId) throws Exception {
		MvcResult resultado = mockMvc.perform(get(COMPRAS_TERCEIROS + "/" + compraId).session(session))
				.andExpect(status().isOk()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.valoresAReceber[0].id");
	}

	private void assertThatLancamentoNaoENulo(String lancamentoId) {
		org.assertj.core.api.Assertions.assertThat(lancamentoId).isNotNull();
	}

	private String criarCartao(Empresa empresa, Usuario autor, String limiteTotal) {
		return criarCartao(empresa, autor, limiteTotal, 5, 12);
	}

	/**
	 * A competencia (vencimento) de uma parcela de cartao nao e "dataCompra + 1
	 * mes": segue o ciclo de fechamento/vencimento do cartao
	 * (ParcelamentoCartaoService.primeiraCompetencia). Este overload permite
	 * escolher diaFechamento/diaVencimento explicitamente para os testes que
	 * precisam de um vencimento previsivel (vencidas/proximas do vencimento).
	 */
	private String criarCartao(Empresa empresa, Usuario autor, String limiteTotal, int diaFechamento, int diaVencimento) {
		PessoaFinanceira titular = pessoaFinanceiraRepository
				.saveAndFlush(new PessoaFinanceira(empresa, "Titular Cartao", null, null, autor));
		InstituicaoFinanceira instituicao = instituicaoFinanceiraRepository
				.saveAndFlush(new InstituicaoFinanceira(empresa, "Banco " + empresa.getCnpj(), "000", autor));
		var cartao = new br.app.criati.financeiro.model.CartaoCredito(empresa, titular, instituicao, "Cartao Teste",
				TipoCartao.FISICO, null, Bandeira.VISA, "1234", new java.math.BigDecimal(limiteTotal), null,
				diaFechamento, diaVencimento, null, autor);
		return cartaoCreditoRepository.saveAndFlush(cartao).getId().toString();
	}

	/**
	 * Dada uma data alvo de vencimento, calcula a dataCompra necessaria para que
	 * a parcela vença exatamente nela, usando um cartao com diaFechamento=1: a
	 * compra feita no dia 2 do mes anterior ao alvo sempre "fecha" para o mes do
	 * alvo (ParcelamentoCartaoService.primeiraCompetencia rola para o proximo
	 * ciclo sempre que a compra e posterior ao fechamento).
	 */
	private LocalDate dataCompraParaVencimento(LocalDate alvo) {
		return java.time.YearMonth.from(alvo).minusMonths(1).atDay(2);
	}

	private ParteFinanceira criarParte(Empresa empresa, Usuario autor, String nome) {
		return parteFinanceiraRepository.saveAndFlush(
				new ParteFinanceira(empresa, nome, TipoParteFinanceira.PESSOA, null, null, null, autor));
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
		return contaFinanceiraRepository.saveAndFlush(new ContaFinanceira(empresa, "Conta", TipoContaFinanceira.CAIXA,
				java.math.BigDecimal.ZERO, StatusCadastro.ATIVO));
	}

	private CategoriaFinanceira criarCategoria(Empresa empresa, String nome, TipoFinanceiro tipo) {
		return categoriaFinanceiraRepository
				.saveAndFlush(new CategoriaFinanceira(empresa, nome, tipo, StatusCadastro.ATIVO));
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
