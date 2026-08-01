package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.financeiro.service.DashboardFinanceiro;
import br.app.criati.financeiro.service.DashboardFinanceiroService;
import br.app.criati.financeiro.service.ConfirmacaoImportacaoBancariaService;
import br.app.criati.financeiro.service.ConfirmacaoTransacaoImportada;
import br.app.criati.financeiro.service.SaldoFinanceiroService;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "criati.financeiro.importacao-ofx.tamanho-maximo-bytes=512")
@Transactional
class ImportacaoBancariaControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL = "/api/contexto/financeiro/importacoes-bancarias";

	@Autowired private MockMvc mockMvc;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private PessoaFinanceiraRepository pessoaRepository;
	@Autowired private InstituicaoFinanceiraRepository instituicaoRepository;
	@Autowired private ContaFinanceiraRepository contaRepository;
	@Autowired private LoteImportacaoBancariaRepository loteRepository;
	@Autowired private TransacaoBancariaImportadaRepository transacaoRepository;
	@Autowired private LancamentoFinanceiroRepository lancamentoRepository;
	@Autowired private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired private SaldoFinanceiroService saldoFinanceiroService;
	@Autowired private DashboardFinanceiroService dashboardFinanceiroService;
	@Autowired private ConfirmacaoImportacaoBancariaService confirmacaoService;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void ofxValidoCriaPreviaRastreavelSemLancamentoFinanceiro() throws Exception {
		Cenario c = cenario("61111111000601", PerfilUsuario.ADMINISTRADOR);
		String conteudo = duasTransacoes();

		mockMvc.perform(upload(c, arquivo("extrato.ofx", conteudo)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(c.conta().getId().toString()))
				.andExpect(jsonPath("$.lote.formato").value("OFX"))
				.andExpect(jsonPath("$.lote.status").value("PREVIA_DISPONIVEL"))
				.andExpect(jsonPath("$.lote.hashArquivo").value(sha256(conteudo)))
				.andExpect(jsonPath("$.lote.quantidadeTransacoes").value(2))
				.andExpect(jsonPath("$.transacoes[0].valor").value(-25.50))
				.andExpect(jsonPath("$.transacoes[1].valor").value(100.00))
				.andExpect(jsonPath("$.transacoes[1].identificadorBancario").doesNotExist());

		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).isEmpty();
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).hasSize(1);
		assertThat(transacaoRepository.countByEmpresaId(c.empresa().getId())).isEqualTo(2);
	}

	private String sha256(String conteudo) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(conteudo.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException excecao) {
			throw new IllegalStateException(excecao);
		}
	}

	@Test
	void rejeitaArquivoVazioExtensaoIncompativelNomeInseguroEAcimaDoLimite() throws Exception {
		Cenario c = cenario("62222222000602", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(upload(c, arquivo("vazio.ofx", ""))).andExpect(status().isBadRequest());
		mockMvc.perform(upload(c, arquivo("extrato.csv", umaTransacao("id-csv", "Memo"))))
				.andExpect(status().isBadRequest());
		mockMvc.perform(upload(c, arquivo("../extrato.ofx", umaTransacao("id-path", "Memo"))))
				.andExpect(status().isBadRequest());
		mockMvc.perform(upload(c, new MockMultipartFile("arquivo", "grande.ofx", "application/x-ofx", new byte[513])))
				.andExpect(status().isBadRequest());
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).isEmpty();
	}

	@Test
	void rejeitaOfxInvalidoMalformadoOuComDataAusenteEFazRollbackIntegral() throws Exception {
		Cenario c = cenario("63333333000603", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(upload(c, arquivo("invalido.ofx", "texto arbitrario"))).andExpect(status().isBadRequest());
		mockMvc.perform(upload(c, arquivo("malformado.ofx", "<OFX><BANKTRANLIST></BANKTRANLIST></OFX>")))
				.andExpect(status().isBadRequest());
		String primeiroValidoSegundoInvalido = cabecalho()
				+ transacao("ok-rollback", "20260731", "-1.00", "Valida")
				+ "<STMTTRN><TRNAMT>-2.00<FITID>sem-data</STMTTRN>" + rodape();
		mockMvc.perform(upload(c, arquivo("rollback.ofx", primeiroValidoSegundoInvalido)))
				.andExpect(status().isBadRequest());
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).isEmpty();
		assertThat(transacaoRepository.countByEmpresaId(c.empresa().getId())).isZero();
	}

	@Test
	void hashRepetidoNaMesmaEmpresaERejeitadoMasEmpresasDiferentesSaoPermitidas() throws Exception {
		Cenario a = cenario("64444444000604", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("65555555000605", PerfilUsuario.ADMINISTRADOR);
		MockMultipartFile mesmoArquivo = arquivo("mesmo.ofx", umaTransacao("hash-igual", "Mesmo"));

		mockMvc.perform(upload(a, mesmoArquivo)).andExpect(status().isCreated());
		mockMvc.perform(upload(a, arquivo("mesmo.ofx", umaTransacao("hash-igual", "Mesmo"))))
				.andExpect(status().isConflict());
		mockMvc.perform(upload(b, arquivo("mesmo.ofx", umaTransacao("hash-igual", "Mesmo"))))
				.andExpect(status().isCreated());
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(a.empresa().getId())).hasSize(1);
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(b.empresa().getId())).hasSize(1);
	}

	@Test
	void sinalizaDuplicidadeDentroDoArquivoEPossivelImportacaoAnterior() throws Exception {
		Cenario c = cenario("66666666000606", PerfilUsuario.ADMINISTRADOR);
		String duplicadas = cabecalho()
				+ transacao("fit-repetido", "20260731", "-10.00", "Primeira")
				+ transacao("fit-repetido", "20260731", "-10.00", "Repetida") + rodape();
		mockMvc.perform(upload(c, arquivo("duplicadas.ofx", duplicadas)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.quantidadeDuplicadasArquivo").value(1))
				.andExpect(jsonPath("$.transacoes[0].duplicadaNoArquivo").value(false))
				.andExpect(jsonPath("$.transacoes[1].duplicadaNoArquivo").value(true));

		mockMvc.perform(upload(c, arquivo("historico.ofx", umaTransacao("fit-repetido", "Outro texto"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(1))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(true));
	}

	@Test
	void contaEConsultaDeOutroTenantSaoRejeitadasSemVazarRegistro() throws Exception {
		Cenario a = cenario("67777777000607", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("68888888000608", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(multipart(URL + "/ofx").file(arquivo("alheia.ofx", umaTransacao("alheia", "Memo")))
				.param("contaId", b.conta().getId().toString()).session(a.session()).with(csrf()))
				.andExpect(status().isNotFound());

		MvcResult criado = mockMvc.perform(upload(a, arquivo("tenant.ofx", umaTransacao("tenant-a", "Memo"))))
				.andExpect(status().isCreated()).andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.lote.id");
		String transacaoId = com.jayway.jsonpath.JsonPath.read(
				criado.getResponse().getContentAsString(), "$.transacoes[0].id");
		mockMvc.perform(get(URL + "/" + loteId).session(b.session())).andExpect(status().isNotFound());
		mockMvc.perform(post(URL + "/" + loteId + "/descartar").session(b.session()).with(csrf()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(URL).session(b.session())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(confirmar(b, loteId, transacaoId, UUID.randomUUID(), "Alheia", false))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(URL + "/" + loteId + "/pendencias").session(b.session()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(URL + "/" + loteId + "/resumo").session(b.session()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(URL + "/" + loteId).session(a.session()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.lote.status").value("PREVIA_DISPONIVEL"));
	}

	@Test
	void descartePreservaHistoricoEImpedeNovoDescarte() throws Exception {
		Cenario c = cenario("69999999000609", PerfilUsuario.GESTOR);
		MvcResult criado = mockMvc.perform(upload(c, arquivo("descartar.ofx", umaTransacao("descartar", "Memo"))))
				.andExpect(status().isCreated()).andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(post(URL + "/" + loteId + "/descartar").session(c.session()).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.status").value("DESCARTADO"))
				.andExpect(jsonPath("$.lote.descartadoEm").isNotEmpty())
				.andExpect(jsonPath("$.transacoes.length()").value(1));
		mockMvc.perform(post(URL + "/" + loteId + "/descartar").session(c.session()).with(csrf()))
				.andExpect(status().isConflict());
		mockMvc.perform(upload(c, arquivo("novo-lote.ofx", umaTransacao("descartar", "Outro memo"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(true));
		assertThat(transacaoRepository.countByEmpresaId(c.empresa().getId())).isEqualTo(2);
	}

	@Test
	void confirmaEntradaESaidaParcialmenteComValorAbsolutoEVinculoRefletindoSaldoEDashboard() throws Exception {
		Cenario c = cenario("72222222000611", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira receita = categoria(c, "Receitas OFX", TipoFinanceiro.RECEITA);
		CategoriaFinanceira despesa = categoria(c, "Despesas OFX", TipoFinanceiro.DESPESA);
		MvcResult criada = mockMvc.perform(upload(c, arquivo("confirmar.ofx", duasTransacoes())))
				.andExpect(status().isCreated()).andReturn();
		String json = criada.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String saidaId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");
		String entradaId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[1].id");

		mockMvc.perform(confirmar(c, loteId, entradaId, receita.getId(), "Credito confirmado", false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumo.pendentes").value(1))
				.andExpect(jsonPath("$.resumo.confirmadas").value(1))
				.andExpect(jsonPath("$.transacoes[0].situacao").value("CONFIRMADA"))
				.andExpect(jsonPath("$.transacoes[0].lancamentoFinanceiroId").isNotEmpty());
		mockMvc.perform(get(URL + "/" + loteId + "/pendencias").session(c.session()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(confirmar(c, loteId, saidaId, despesa.getId(), "Debito confirmado", false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumo.pendentes").value(0))
				.andExpect(jsonPath("$.resumo.confirmadas").value(2));

		List<LancamentoFinanceiro> lancamentos = lancamentoRepository.findAllByEmpresaId(c.empresa().getId());
		assertThat(lancamentos).hasSize(2).allSatisfy(l -> {
			assertThat(l.getOrigem()).isEqualTo(OrigemLancamentoFinanceiro.IMPORTACAO);
			assertThat(l.getStatus()).isEqualTo(StatusLancamentoFinanceiro.LIQUIDADO);
			assertThat(l.getConta().getId()).isEqualTo(c.conta().getId());
		});
		assertThat(lancamentos).filteredOn(l -> l.getTipo() == TipoFinanceiro.RECEITA)
				.extracting(LancamentoFinanceiro::getValor).containsExactly(new BigDecimal("100.00"));
		assertThat(lancamentos).filteredOn(l -> l.getTipo() == TipoFinanceiro.DESPESA)
				.extracting(LancamentoFinanceiro::getValor).containsExactly(new BigDecimal("25.50"));
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(
				c.empresa().getId(), UUID.fromString(loteId)))
				.allSatisfy(t -> {
					assertThat(t.getSituacao()).isEqualTo(SituacaoTransacaoImportada.CONFIRMADA);
					assertThat(t.getLancamentoFinanceiro()).isNotNull();
					assertThat(t.getConfirmadaEm()).isNotNull();
					assertThat(t.getConfirmadaPor().getId()).isEqualTo(c.usuario().getId());
				});
		assertThat(saldoFinanceiroService.calcularSaldoAtual(c.conta())).isEqualByComparingTo("74.50");
		DashboardFinanceiro dashboard = dashboardFinanceiroService.gerar(
				new ContextoEmpresaAtual(c.usuario().getId(), c.empresa().getId(), null,
						PerfilUsuario.ADMINISTRADOR), YearMonth.of(2026, 7));
		assertThat(dashboard.receitasPagas()).isEqualByComparingTo("100.00");
		assertThat(dashboard.despesasPagas()).isEqualByComparingTo("25.50");
		assertThat(dashboard.saldoAtualConsolidado()).isEqualByComparingTo("74.50");
	}

	@Test
	void categoriaDeOutroTenantFalhaEFazRollbackIntegralDoLote() throws Exception {
		Cenario a = cenario("73333333000612", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("74444444000613", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira despesaA = categoria(a, "Despesa A", TipoFinanceiro.DESPESA);
		CategoriaFinanceira receitaB = categoria(b, "Receita B", TipoFinanceiro.RECEITA);
		MvcResult criada = mockMvc.perform(upload(a, arquivo("rollback-confirmacao.ofx", duasTransacoes())))
				.andExpect(status().isCreated()).andReturn();
		String json = criada.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String saidaId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");
		String entradaId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[1].id");
		String corpo = """
				{"transacoes":[
				 {"transacaoId":"%s","categoriaId":"%s","descricaoFinal":"Saida"},
				 {"transacaoId":"%s","categoriaId":"%s","descricaoFinal":"Entrada"}
				]}
				""".formatted(saidaId, despesaA.getId(), entradaId, receitaB.getId());

		mockMvc.perform(post(URL + "/" + loteId + "/confirmacoes").session(a.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isNotFound());
		assertThat(lancamentoRepository.findAllByEmpresaId(a.empresa().getId())).isEmpty();
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(
				a.empresa().getId(), UUID.fromString(loteId)))
				.allMatch(t -> t.getSituacao() == SituacaoTransacaoImportada.PENDENTE);
	}

	@Test
	void duplicidadeExigeAceiteConfirmacaoERequisicaoRepetidasSaoIdempotentesEIgnorarPreservaHistorico()
			throws Exception {
		Cenario c = cenario("75555555000614", PerfilUsuario.GESTOR);
		CategoriaFinanceira despesa = categoria(c, "Despesa duplicada", TipoFinanceiro.DESPESA);
		String conteudo = cabecalho() + transacao("duplicada-confirmacao", "20260731", "-10.00", "Primeira")
				+ transacao("duplicada-confirmacao", "20260731", "-10.00", "Segunda") + rodape();
		MvcResult criada = mockMvc.perform(upload(c, arquivo("duplicada-confirmacao.ofx", conteudo)))
				.andExpect(status().isCreated()).andReturn();
		String json = criada.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String primeiraId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");
		String duplicadaId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[1].id");

		mockMvc.perform(confirmar(c, loteId, duplicadaId, despesa.getId(), "Duplicada explicita", false))
				.andExpect(status().isConflict());
		mockMvc.perform(confirmar(c, loteId, duplicadaId, despesa.getId(), "Duplicada explicita", true))
				.andExpect(status().isOk()).andExpect(jsonPath("$.resumo.duplicadas").value(1));
		mockMvc.perform(confirmar(c, loteId, duplicadaId, despesa.getId(), "Duplicada explicita", true))
				.andExpect(status().isOk());
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).hasSize(1);

		mockMvc.perform(post(URL + "/" + loteId + "/transacoes/" + primeiraId + "/ignorar")
				.session(c.session()).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.resumo.ignoradas").value(1));
		mockMvc.perform(post(URL + "/" + loteId + "/transacoes/" + primeiraId + "/ignorar")
				.session(c.session()).with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(confirmar(c, loteId, primeiraId, despesa.getId(), "Nao pode", false))
				.andExpect(status().isConflict());
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(
				c.empresa().getId(), UUID.fromString(loteId)))
				.filteredOn(t -> t.getSituacao() == SituacaoTransacaoImportada.IGNORADA)
				.singleElement().satisfies(t -> {
					assertThat(t.getIgnoradaEm()).isNotNull();
					assertThat(t.getIgnoradaPor().getId()).isEqualTo(c.usuario().getId());
					assertThat(t.getLancamentoFinanceiro()).isNull();
				});
		mockMvc.perform(get(URL + "/" + loteId + "/resumo").session(c.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pendentes").value(0))
				.andExpect(jsonPath("$.confirmadas").value(1))
				.andExpect(jsonPath("$.ignoradas").value(1));
	}

	@Test
	void loteDescartadoNaoPodeSerConfirmado() throws Exception {
		Cenario c = cenario("76666666000615", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira despesa = categoria(c, "Despesa descartada", TipoFinanceiro.DESPESA);
		MvcResult criada = mockMvc.perform(upload(c, arquivo("descartado-confirmacao.ofx",
				umaTransacao("descartado-confirmacao", "Memo")))).andExpect(status().isCreated()).andReturn();
		String json = criada.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String transacaoId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");
		mockMvc.perform(post(URL + "/" + loteId + "/descartar").session(c.session()).with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(confirmar(c, loteId, transacaoId, despesa.getId(), "Nao confirmar", false))
				.andExpect(status().isConflict());
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).isEmpty();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void confirmacoesConcorrentesGeramNoMaximoUmLancamento() throws Exception {
		Cenario c = cenario("77777777000616", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira despesa = categoria(c, "Despesa concorrente", TipoFinanceiro.DESPESA);
		MvcResult criada = mockMvc.perform(upload(c, arquivo("concorrencia.ofx",
				umaTransacao("concorrencia-confirmacao", "Concorrente"))))
				.andExpect(status().isCreated()).andReturn();
		String json = criada.getResponse().getContentAsString();
		UUID loteId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.lote.id"));
		UUID transacaoId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id"));
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(c.usuario().getId(), c.empresa().getId(), null,
				PerfilUsuario.ADMINISTRADOR);
		ConfirmacaoTransacaoImportada comando = new ConfirmacaoTransacaoImportada(
				transacaoId, despesa.getId(), "Confirmacao concorrente", false);
		CountDownLatch inicio = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> primeira = executor.submit(() -> {
				aguardar(inicio);
				confirmacaoService.confirmar(loteId, List.of(comando), contexto);
			});
			Future<?> segunda = executor.submit(() -> {
				aguardar(inicio);
				confirmacaoService.confirmar(loteId, List.of(comando), contexto);
			});
			inicio.countDown();
			primeira.get(20, TimeUnit.SECONDS);
			segunda.get(20, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).hasSize(1);
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(c.empresa().getId(), loteId))
				.singleElement().satisfies(t -> assertThat(t.getLancamentoFinanceiro()).isNotNull());
	}

	@Test
	void endpointsExigemAutenticacaoCsrfEPerfilDeEscrita() throws Exception {
		Cenario usuario = cenario("71111111000610", PerfilUsuario.USUARIO);
		MockMultipartFile arquivo = arquivo("seguranca.ofx", umaTransacao("seguranca", "Memo"));
		mockMvc.perform(multipart(URL + "/ofx").file(arquivo).param("contaId", usuario.conta().getId().toString())
				.with(csrf()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(multipart(URL + "/ofx").file(arquivo("seguranca.ofx", umaTransacao("seguranca", "Memo")))
				.param("contaId", usuario.conta().getId().toString()).session(usuario.session()))
				.andExpect(status().isForbidden());
		mockMvc.perform(upload(usuario, arquivo("seguranca.ofx", umaTransacao("seguranca", "Memo"))))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(URL).session(usuario.session())).andExpect(status().isOk());
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
		String aleatorio = UUID.randomUUID().toString();
		String corpo = ("{\"transacoes\":[{\"transacaoId\":\"%s\",\"categoriaId\":\"%s\","
				+ "\"descricaoFinal\":\"Teste\"}]}").formatted(aleatorio, UUID.randomUUID());
		mockMvc.perform(post(URL + "/" + aleatorio + "/confirmacoes").session(usuario.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
		mockMvc.perform(post(URL + "/" + aleatorio + "/confirmacoes").session(usuario.session())
				.contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.RequestBuilder confirmar(Cenario cenario, String loteId,
			String transacaoId, UUID categoriaId, String descricao, boolean confirmarDuplicidade) {
		String corpo = """
				{"transacoes":[{"transacaoId":"%s","categoriaId":"%s",
				"descricaoFinal":"%s","confirmarDuplicidade":%s}]}
				""".formatted(transacaoId, categoriaId, descricao, confirmarDuplicidade);
		return post(URL + "/" + loteId + "/confirmacoes").session(cenario.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	private CategoriaFinanceira categoria(Cenario cenario, String nome, TipoFinanceiro tipo) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(cenario.empresa(), nome, null, null,
				tipo, 0, tipo == TipoFinanceiro.DESPESA, cenario.usuario()));
	}

	private static void aguardar(CountDownLatch inicio) {
		try {
			inicio.await();
		} catch (InterruptedException excecao) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(excecao);
		}
	}

	private org.springframework.test.web.servlet.RequestBuilder upload(
			Cenario cenario, MockMultipartFile arquivo) {
		return multipart(URL + "/ofx").file(arquivo).param("contaId", cenario.conta().getId().toString())
				.session(cenario.session()).with(csrf());
	}

	private MockMultipartFile arquivo(String nome, String conteudo) {
		return new MockMultipartFile("arquivo", nome, "application/x-ofx", conteudo.getBytes(StandardCharsets.UTF_8));
	}

	private String duasTransacoes() {
		return cabecalho() + transacao("fit-1", "20260730", "-25.50", "Café &amp; mercado")
				+ "<STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260731<TRNAMT>100.00<MEMO>Crédito</STMTTRN>"
				+ rodape();
	}

	private String umaTransacao(String fitid, String memo) {
		return cabecalho() + transacao(fitid, "20260731", "-10.00", memo) + rodape();
	}

	private String transacao(String fitid, String data, String valor, String memo) {
		return "<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>" + data + "<TRNAMT>" + valor
				+ "<FITID>" + fitid + "<MEMO>" + memo + "</STMTTRN>";
	}

	private String cabecalho() {
		return "OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKTRANLIST>";
	}

	private String rodape() {
		return "</BANKTRANLIST></OFX>";
	}

	private Cenario cenario(String cnpj, PerfilUsuario perfil) throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa OFX", "Empresa OFX", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("Usuario OFX", "ofx." + cnpj + "@criati.test",
				passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		PessoaFinanceira titular = pessoaRepository.saveAndFlush(
				new PessoaFinanceira(empresa, "Titular OFX", null, null, usuario));
		InstituicaoFinanceira instituicao = instituicaoRepository.saveAndFlush(
				new InstituicaoFinanceira(empresa, "Banco OFX", "777", usuario));
		ContaFinanceira conta = contaRepository.saveAndFlush(new ContaFinanceira(empresa, titular, instituicao,
				"Conta OFX", TipoContaFinanceira.CONTA_CORRENTE, BigDecimal.ZERO, LocalDate.of(2026, 1, 1), true, usuario));
		return new Cenario(empresa, usuario, conta, autenticarNaEmpresa(usuario.getEmail(), empresa.getId()));
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA)))
				.andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		mockMvc.perform(post("/api/contexto/empresa-ativa").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"empresaId\":\"%s\"}".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}

	private record Cenario(Empresa empresa, Usuario usuario, ContaFinanceira conta, MockHttpSession session) {
	}
}
