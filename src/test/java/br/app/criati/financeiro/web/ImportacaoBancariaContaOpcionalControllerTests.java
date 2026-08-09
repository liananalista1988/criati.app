package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.financeiro.service.ImportacaoBancariaService;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * CRIATI-IMP-FEAT-004: conta financeira opcional na importacao bancaria e
 * sugestao automatica a partir de metadados OFX. Cobre apenas o que essa
 * tarefa acrescentou - o fluxo com conta informada explicitamente (inalterado)
 * ja e coberto por {@link ImportacaoBancariaControllerTests}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ImportacaoBancariaContaOpcionalControllerTests {

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
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ImportacaoBancariaService importacaoBancariaService;

	@Test
	void uploadOfxSemContaCriaLotePendenteDeResolucaoSemSugestao() throws Exception {
		Cenario c = cenario("11100000000101", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("sem-conta.ofx", umaTransacao("sc-1", "Sem conta"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").doesNotExist())
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist())
				.andExpect(jsonPath("$.transacoes[0].situacao").value("PENDENTE"));
		assertThat(loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).hasSize(1);
	}

	@Test
	void uploadCsvSemContaCriaLotePendenteDeResolucao() throws Exception {
		Cenario c = cenario("11100000000102", PerfilUsuario.ADMINISTRADOR);
		String csv = "data,descricao,valor\n2026-08-01,Sem conta CSV,-10.00\n";
		mockMvc.perform(multipart(URL + "/csv").file(arquivoCsv("sem-conta.csv", csv)).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").doesNotExist())
				.andExpect(jsonPath("$.lote.formato").value("CSV"));
	}

	@Test
	void uploadXlsxSemContaCriaLotePendenteDeResolucao() throws Exception {
		Cenario c = cenario("11100000000103", PerfilUsuario.ADMINISTRADOR);
		byte[] xlsx = xlsx("SC-1", "SC-2");
		mockMvc.perform(multipart(URL + "/xlsx").file(arquivoXlsx("sem-conta.xlsx", xlsx))
				.session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").doesNotExist())
				.andExpect(jsonPath("$.lote.formato").value("XLSX"));
	}

	@Test
	void ofxComIdentificacaoCompletaEUmaContaCompativelSugereConta() throws Exception {
		Cenario c = cenario("11100000000104", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Sugestao", "0341");
		ContaFinanceira conta = contaComIdentificacao(c, instituicao, "0001", "654321");

		MvcResult criado = mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("sugestao.ofx",
				umaTransacao("sug-1", "Com sugestao"), "0341", "0001", "654321")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").doesNotExist())
				.andExpect(jsonPath("$.lote.contaSugeridaId").value(conta.getId().toString()))
				.andExpect(jsonPath("$.lote.identificacaoBancoId").value("0341"))
				.andExpect(jsonPath("$.lote.identificacaoAgencia").value("0001"))
				.andExpect(jsonPath("$.lote.identificacaoNumeroConta").value("654321"))
				.andReturn();
		assertThat(criado.getResponse().getContentAsString()).contains(conta.getId().toString());
	}

	@Test
	void ofxSemContaCompativelNaoSugereNada() throws Exception {
		Cenario c = cenario("11100000000105", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Sem Match", "0342");
		contaComIdentificacao(c, instituicao, "0001", "111111");

		mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("sem-match.ofx",
				umaTransacao("sm-1", "Sem match"), "0342", "0001", "999999")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	@Test
	void ofxComMultiplasContasCompativeisNaoEscolheAutomaticamente() throws Exception {
		Cenario c = cenario("11100000000106", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Ambiguo", "0343");
		contaComIdentificacao(c, instituicao, "0002", "222222");
		contaComIdentificacao(c, instituicao, "0002", "222222");

		mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("ambiguo.ofx",
				umaTransacao("amb-1", "Ambiguo"), "0343", "0002", "222222")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	@Test
	void ofxComMetadadosIncompletosNaoSugereConta() throws Exception {
		Cenario c = cenario("11100000000107", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Incompleto", "0344");
		contaComIdentificacao(c, instituicao, "0003", "333333");
		String semAgenciaNemConta = "OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKACCTFROM><BANKID>0344</BANKACCTFROM>"
				+ "<BANKTRANLIST>" + transacaoOfx("inc-1", "Incompleto") + "</BANKTRANLIST></OFX>";

		mockMvc.perform(uploadOfxSemConta(c, arquivo("incompleto.ofx", semAgenciaNemConta)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist())
				.andExpect(jsonPath("$.lote.identificacaoBancoId").value("0344"))
				.andExpect(jsonPath("$.lote.identificacaoAgencia").doesNotExist());
	}

	@Test
	void isolamentoEntreEmpresasNaAutodetecaoDeConta() throws Exception {
		Cenario a = cenario("11100000000108", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("11100000000109", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicaoB = instituicao(b, "Banco De B", "0345");
		// Conta com os MESMOS dados bancarios existe apenas na empresa B.
		contaComIdentificacao(b, instituicaoB, "0004", "444444");

		mockMvc.perform(uploadOfxSemConta(a, arquivoOfx("isolamento.ofx",
				umaTransacao("iso-1", "Isolamento"), "0345", "0004", "444444")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	@Test
	void selecaoManualResolveContaMesmoDivergindoDaSugestao() throws Exception {
		Cenario c = cenario("11100000000110", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Divergente", "0346");
		ContaFinanceira sugerida = contaComIdentificacao(c, instituicao, "0005", "555555");
		ContaFinanceira escolhidaManualmente = contaSimples(c, "Conta escolhida manualmente");

		MvcResult criado = mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("divergente.ofx",
				umaTransacao("div-1", "Divergente"), "0346", "0005", "555555")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaSugeridaId").value(sugerida.getId().toString()))
				.andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(c, loteId, escolhidaManualmente.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.contaId").value(escolhidaManualmente.getId().toString()));
	}

	@Test
	void confirmacaoSemContaResolvidaEBloqueada() throws Exception {
		Cenario c = cenario("11100000000111", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira despesa = categoria(c, "Despesa sem conta", TipoFinanceiro.DESPESA);
		MvcResult criado = mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("bloqueada.ofx",
				umaTransacao("bloq-1", "Bloqueada")))).andExpect(status().isCreated()).andReturn();
		String json = criado.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String transacaoId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");

		mockMvc.perform(confirmar(c, loteId, transacaoId, despesa.getId(), "Nao pode confirmar"))
				.andExpect(status().isConflict());
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).isEmpty();
	}

	@Test
	void confirmacaoFuncionaAposResolverConta() throws Exception {
		Cenario c = cenario("11100000000112", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta resolvida depois");
		CategoriaFinanceira despesa = categoria(c, "Despesa resolvida", TipoFinanceiro.DESPESA);
		MvcResult criado = mockMvc.perform(uploadOfxSemConta(c, arquivoOfx("resolver.ofx",
				umaTransacao("resolver-1", "Resolver depois")))).andExpect(status().isCreated()).andReturn();
		String json = criado.getResponse().getContentAsString();
		String loteId = com.jayway.jsonpath.JsonPath.read(json, "$.lote.id");
		String transacaoId = com.jayway.jsonpath.JsonPath.read(json, "$.transacoes[0].id");

		mockMvc.perform(resolverConta(c, loteId, conta.getId())).andExpect(status().isOk());
		mockMvc.perform(confirmar(c, loteId, transacaoId, despesa.getId(), "Confirmado apos resolver"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.resumo.confirmadas").value(1));
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).hasSize(1)
				.allSatisfy(l -> assertThat(l.getConta().getId()).isEqualTo(conta.getId()));
	}

	@Test
	void deduplicacaoAntesEDepoisDaResolucaoDeConta() throws Exception {
		Cenario c = cenario("11100000000113", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta dedup");

		// 1a importacao sem conta: nao ha como comparar contra historico ainda.
		MvcResult primeiro = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("dedup-1.ofx", umaTransacao("dedup-1", "Duplicavel"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(0))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(false))
				.andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(primeiro.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(c, loteId, conta.getId())).andExpect(status().isOk());

		// 2a importacao (arquivo diferente, mesmo FITID = mesma chave de
		// deduplicacao) ja informando a conta: agora a transacao da 1a importacao
		// (que ja tem conta resolvida) e encontrada como possivel duplicata.
		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("dedup-2.ofx",
				umaTransacao("dedup-1", "Duplicavel novamente")))
				.param("contaId", conta.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(1))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(true));
	}

	// CRIATI-IMP-FIX-007, item 1: resolverConta precisa recalcular a
	// duplicidade historica de TODAS as transacoes do lote, nao so detectar
	// duplicidade em uploads futuros. Aqui o historico ja existe ANTES do
	// lote sem conta ser sequer criado.
	@Test
	void resolverContaDetectaDuplicidadeHistoricaJaExistente() throws Exception {
		Cenario c = cenario("11100000000114", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta historico previo");
		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("previo.ofx", umaTransacao("hist-previo", "Ja importada")))
				.param("contaId", conta.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated());

		MvcResult semConta = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("posterior.ofx", umaTransacao("hist-previo", "Mesma transacao, arquivo novo"))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(0))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(false))
				.andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(c, loteId, conta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(1))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(true));
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(c.empresa().getId(),
				UUID.fromString(loteId))).singleElement()
				.satisfies(t -> assertThat(t.isPossivelmenteJaImportada()).isTrue());
	}

	@Test
	void resolverContaSemDuplicidadeHistoricaMantemContadorZerado() throws Exception {
		Cenario c = cenario("11100000000115", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta sem historico");
		MvcResult semConta = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("inedito.ofx", umaTransacao("inedito-1", "Nunca importada"))))
				.andExpect(status().isCreated()).andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(c, loteId, conta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(0))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(false));
	}

	// CRIATI-IMP-FIX-007, item 1: isolamento multiempresa no recalculo -
	// historico de OUTRA empresa nunca pode marcar duplicidade aqui, mesmo
	// com o mesmo identificador bancario (FITID) e a mesma conta escolhida
	// (contas de empresas diferentes nunca compartilham UUID por construcao,
	// mas o teste confirma que a query correta e usada).
	@Test
	void resolverContaRecalculaDuplicidadeIsoladaPorEmpresa() throws Exception {
		Cenario a = cenario("11100000000116", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("11100000000117", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaA = contaSimples(a, "Conta A isolamento");
		ContaFinanceira contaB = contaSimples(b, "Conta B isolamento");
		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("empresa-a.ofx", umaTransacao("iso-dedup", "Empresa A")))
				.param("contaId", contaA.getId().toString()).session(a.session()).with(csrf()))
				.andExpect(status().isCreated());

		MvcResult semContaB = mockMvc.perform(uploadOfxSemConta(b,
				arquivoOfx("empresa-b.ofx", umaTransacao("iso-dedup", "Empresa B"))))
				.andExpect(status().isCreated()).andReturn();
		String loteIdB = com.jayway.jsonpath.JsonPath.read(semContaB.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(b, loteIdB, contaB.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lote.quantidadePossiveisDuplicadas").value(0))
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(false));
	}

	// CRIATI-IMP-FIX-007, item 1: nao permitir confirmacao usando estado de
	// duplicidade desatualizado - apos resolverConta recalcular, confirmar
	// sem aceitar a duplicidade sinalizada deve continuar exigindo o aceite
	// explicito (mesma regra ja aplicada a duplicidade detectada no upload).
	@Test
	void confirmacaoRespeitaDuplicidadeRecalculadaAposResolverConta() throws Exception {
		Cenario c = cenario("11100000000118", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta confirmacao dedup");
		CategoriaFinanceira despesa = categoria(c, "Despesa dedup recalculada", TipoFinanceiro.DESPESA);
		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("original.ofx", umaTransacao("conf-dedup", "Original")))
				.param("contaId", conta.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated());

		MvcResult semConta = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("repique.ofx", umaTransacao("conf-dedup", "Repique sem conta"))))
				.andExpect(status().isCreated()).andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.lote.id");
		String transacaoId = com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.transacoes[0].id");

		mockMvc.perform(resolverConta(c, loteId, conta.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transacoes[0].possivelmenteJaImportada").value(true));

		mockMvc.perform(confirmar(c, loteId, transacaoId, despesa.getId(), "Sem aceitar duplicidade"))
				.andExpect(status().isConflict());
		mockMvc.perform(confirmarComDuplicidade(c, loteId, transacaoId, despesa.getId(), "Aceitando duplicidade"))
				.andExpect(status().isOk());
		// So a transacao "repique" foi confirmada nesta tarefa - "original" segue
		// PENDENTE (nunca confirmada), logo exatamente 1 lancamento existe.
		assertThat(lancamentoRepository.findAllByEmpresaId(c.empresa().getId())).hasSize(1);
	}

	// CRIATI-IMP-FIX-007, item 6: segunda resolucao sequencial deve ser
	// rejeitada - o lote so pode ter a conta definida uma unica vez.
	@Test
	void segundaResolucaoDeContaNoMesmoLoteERejeitada() throws Exception {
		Cenario c = cenario("11100000000119", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira primeira = contaSimples(c, "Primeira conta resolvida");
		ContaFinanceira segunda = contaSimples(c, "Segunda tentativa de conta");
		MvcResult semConta = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("unica-resolucao.ofx", umaTransacao("unica-1", "Resolucao unica"))))
				.andExpect(status().isCreated()).andReturn();
		String loteId = com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.lote.id");

		mockMvc.perform(resolverConta(c, loteId, primeira.getId())).andExpect(status().isOk());
		mockMvc.perform(resolverConta(c, loteId, segunda.getId())).andExpect(status().isBadRequest());
		assertThat(loteRepository.findByIdAndEmpresaId(UUID.fromString(loteId), c.empresa().getId()))
				.get().satisfies(lote -> assertThat(lote.getConta().getId()).isEqualTo(primeira.getId()));
	}

	// CRIATI-IMP-FIX-007, item 6: duas resolucoes concorrentes com contas
	// diferentes nao podem gerar troca de conta nem inconsistencia entre
	// lote e transacoes - o lock pessimista em buscarLoteParaAtualizar
	// serializa as chamadas; exatamente uma tem sucesso.
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void resolucoesConcorrentesDeContaNaoGeramInconsistencia() throws Exception {
		Cenario c = cenario("11100000000120", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaX = contaSimples(c, "Conta concorrente X");
		ContaFinanceira contaY = contaSimples(c, "Conta concorrente Y");
		MvcResult semConta = mockMvc.perform(uploadOfxSemConta(c,
				arquivoOfx("concorrencia-conta.ofx", umaTransacao("concorrencia-conta-1", "Concorrente"))))
				.andExpect(status().isCreated()).andReturn();
		UUID loteId = UUID.fromString(
				com.jayway.jsonpath.JsonPath.read(semConta.getResponse().getContentAsString(), "$.lote.id"));
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(c.usuario().getId(), c.empresa().getId(), null,
				PerfilUsuario.ADMINISTRADOR);

		CountDownLatch inicio = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<Throwable> falhas = new java.util.concurrent.CopyOnWriteArrayList<>();
		try {
			Future<?> primeira = executor.submit(() -> {
				aguardar(inicio);
				try {
					importacaoBancariaService.resolverConta(loteId, contaX.getId(), contexto);
				} catch (RuntimeException excecao) {
					falhas.add(excecao);
				}
			});
			Future<?> segunda = executor.submit(() -> {
				aguardar(inicio);
				try {
					importacaoBancariaService.resolverConta(loteId, contaY.getId(), contexto);
				} catch (RuntimeException excecao) {
					falhas.add(excecao);
				}
			});
			inicio.countDown();
			primeira.get(20, TimeUnit.SECONDS);
			segunda.get(20, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		assertThat(falhas).hasSize(1);
		assertThat(falhas.get(0)).isInstanceOf(DadosInvalidosException.class);
		var loteFinal = loteRepository.findByIdAndEmpresaId(loteId, c.empresa().getId()).orElseThrow();
		assertThat(loteFinal.getConta().getId()).isIn(contaX.getId(), contaY.getId());
		List<TransacaoBancariaImportada> transacoesFinais = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(c.empresa().getId(), loteId);
		assertThat(transacoesFinais).allSatisfy(
				t -> assertThat(t.getConta().getId()).isEqualTo(loteFinal.getConta().getId()));
	}

	// CRIATI-IMP-FIX-007, item 5: metadados OFX (BANKID/BRANCHID/ACCTID/
	// ACCTTYPE) devem ser persistidos sempre que existirem no arquivo, com ou
	// sem contaId informado no upload. Autodetecao (contaSugerida) tambem
	// roda nos dois casos desde a CRIATI-IMP-FIX-009 (ver testes de
	// divergencia abaixo) - aqui simplesmente nao ha instituicao cadastrada
	// com o codigo do OFX, entao nao ha candidata alguma para sugerir.
	@Test
	void metadadosOfxSaoPersistidosMesmoComContaInformadaNoUpload() throws Exception {
		Cenario c = cenario("11100000000121", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = contaSimples(c, "Conta ja informada no upload");
		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("com-conta-e-metadados.ofx",
				umaTransacao("meta-1", "Com conta e metadados"), "0347", "0009", "888777"))
				.param("contaId", conta.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(conta.getId().toString()))
				.andExpect(jsonPath("$.lote.identificacaoBancoId").value("0347"))
				.andExpect(jsonPath("$.lote.identificacaoAgencia").value("0009"))
				.andExpect(jsonPath("$.lote.identificacaoNumeroConta").value("888777"))
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	// ---- CRIATI-IMP-FIX-009: autodetecao roda tambem quando contaId e
	// informado no upload, para permitir o aviso de divergencia na tela de
	// revisao - a conta escolhida pelo usuario nunca e substituida.

	@Test
	void ofxComContaInformadaESugestaoIgualNaoIndicaDivergencia() throws Exception {
		Cenario c = cenario("11100000000122", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Igual", "0350");
		ContaFinanceira conta = contaComIdentificacao(c, instituicao, "0010", "111222");

		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("igual.ofx",
				umaTransacao("igual-1", "Sugestao igual"), "0350", "0010", "111222"))
				.param("contaId", conta.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(conta.getId().toString()))
				.andExpect(jsonPath("$.lote.contaSugeridaId").value(conta.getId().toString()));
	}

	@Test
	void ofxComContaInformadaESugestaoDiferentePreservaEscolhaEExpoeAmbasParaDivergencia() throws Exception {
		Cenario c = cenario("11100000000123", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Divergente Upload", "0351");
		ContaFinanceira contaSugerida = contaComIdentificacao(c, instituicao, "0011", "333444");
		ContaFinanceira contaEscolhida = contaSimples(c, "Conta escolhida no upload");

		MvcResult criado = mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("divergente-upload.ofx",
				umaTransacao("div-upload-1", "Sugestao diferente"), "0351", "0011", "333444"))
				.param("contaId", contaEscolhida.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(contaEscolhida.getId().toString()))
				.andExpect(jsonPath("$.lote.contaSugeridaId").value(contaSugerida.getId().toString()))
				.andReturn();

		// A conta persistida no lote e exatamente a informada no upload, nunca a
		// sugerida - confirmado tanto na resposta quanto direto no repositorio.
		String loteId = com.jayway.jsonpath.JsonPath.read(criado.getResponse().getContentAsString(), "$.lote.id");
		assertThat(loteRepository.findByIdAndEmpresaId(UUID.fromString(loteId), c.empresa().getId()))
				.get().satisfies(lote -> {
					assertThat(lote.getConta().getId()).isEqualTo(contaEscolhida.getId());
					assertThat(lote.getContaSugerida().getId()).isEqualTo(contaSugerida.getId());
				});
		assertThat(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(
				c.empresa().getId(), UUID.fromString(loteId)))
				.allSatisfy(t -> assertThat(t.getConta().getId()).isEqualTo(contaEscolhida.getId()));
	}

	@Test
	void ofxComContaInformadaESemCandidataCompativelNaoSugereNada() throws Exception {
		Cenario c = cenario("11100000000124", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaEscolhida = contaSimples(c, "Conta sem candidata compativel");

		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("sem-candidata-upload.ofx",
				umaTransacao("sc-upload-1", "Sem candidata"), "0352", "0012", "555666"))
				.param("contaId", contaEscolhida.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(contaEscolhida.getId().toString()))
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	@Test
	void ofxComContaInformadaEMultiplasCandidatasNaoSugereNada() throws Exception {
		Cenario c = cenario("11100000000125", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicao = instituicao(c, "Banco Ambiguo Upload", "0353");
		contaComIdentificacao(c, instituicao, "0013", "777888");
		contaComIdentificacao(c, instituicao, "0013", "777888");
		ContaFinanceira contaEscolhida = contaSimples(c, "Conta escolhida com ambiguidade");

		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("ambiguo-upload.ofx",
				umaTransacao("amb-upload-1", "Ambiguo com conta"), "0353", "0013", "777888"))
				.param("contaId", contaEscolhida.getId().toString()).session(c.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(contaEscolhida.getId().toString()))
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	@Test
	void isolamentoMultiempresaNaAutodetecaoComContaInformadaNoUpload() throws Exception {
		Cenario a = cenario("11100000000126", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("11100000000127", PerfilUsuario.ADMINISTRADOR);
		InstituicaoFinanceira instituicaoB = instituicao(b, "Banco De B Upload", "0354");
		// Conta com os mesmos dados bancarios do OFX existe apenas na empresa B.
		contaComIdentificacao(b, instituicaoB, "0014", "999000");
		ContaFinanceira contaEscolhidaA = contaSimples(a, "Conta escolhida na empresa A");

		mockMvc.perform(multipart(URL + "/ofx").file(arquivoOfx("isolamento-upload.ofx",
				umaTransacao("iso-upload-1", "Isolamento com conta"), "0354", "0014", "999000"))
				.param("contaId", contaEscolhidaA.getId().toString()).session(a.session()).with(csrf()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lote.contaId").value(contaEscolhidaA.getId().toString()))
				.andExpect(jsonPath("$.lote.contaSugeridaId").doesNotExist());
	}

	private static void aguardar(CountDownLatch inicio) {
		try {
			inicio.await();
		} catch (InterruptedException excecao) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(excecao);
		}
	}

	// ---------------------------------------------------------------- helpers

	private org.springframework.test.web.servlet.RequestBuilder uploadOfxSemConta(
			Cenario cenario, MockMultipartFile arquivo) {
		return multipart(URL + "/ofx").file(arquivo).session(cenario.session()).with(csrf());
	}

	private org.springframework.test.web.servlet.RequestBuilder resolverConta(Cenario cenario, String loteId, UUID contaId) {
		String corpo = "{\"contaId\":\"%s\"}".formatted(contaId);
		return post(URL + "/" + loteId + "/conta").session(cenario.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	private org.springframework.test.web.servlet.RequestBuilder confirmar(
			Cenario cenario, String loteId, String transacaoId, UUID categoriaId, String descricao) {
		String corpo = """
				{"transacoes":[{"transacaoId":"%s","categoriaId":"%s","descricaoFinal":"%s"}]}
				""".formatted(transacaoId, categoriaId, descricao);
		return post(URL + "/" + loteId + "/confirmacoes").session(cenario.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	private org.springframework.test.web.servlet.RequestBuilder confirmarComDuplicidade(
			Cenario cenario, String loteId, String transacaoId, UUID categoriaId, String descricao) {
		String corpo = """
				{"transacoes":[{"transacaoId":"%s","categoriaId":"%s","descricaoFinal":"%s","confirmarDuplicidade":true}]}
				""".formatted(transacaoId, categoriaId, descricao);
		return post(URL + "/" + loteId + "/confirmacoes").session(cenario.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo);
	}

	private CategoriaFinanceira categoria(Cenario cenario, String nome, TipoFinanceiro tipo) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(cenario.empresa(), nome, null, null,
				tipo, 0, tipo == TipoFinanceiro.DESPESA, cenario.usuario()));
	}

	private InstituicaoFinanceira instituicao(Cenario cenario, String nome, String codigo) {
		return instituicaoRepository.saveAndFlush(
				new InstituicaoFinanceira(cenario.empresa(), nome, codigo, cenario.usuario()));
	}

	private ContaFinanceira contaSimples(Cenario cenario, String nome) {
		PessoaFinanceira titular = pessoaRepository.saveAndFlush(
				new PessoaFinanceira(cenario.empresa(), "Titular " + nome, null, null, cenario.usuario()));
		return contaRepository.saveAndFlush(new ContaFinanceira(cenario.empresa(), titular, null, nome,
				TipoContaFinanceira.CAIXA, BigDecimal.ZERO, LocalDate.of(2026, 1, 1), false, cenario.usuario()));
	}

	private ContaFinanceira contaComIdentificacao(
			Cenario cenario, InstituicaoFinanceira instituicao, String agencia, String numeroConta) {
		PessoaFinanceira titular = pessoaRepository.saveAndFlush(
				new PessoaFinanceira(cenario.empresa(), "Titular " + agencia + numeroConta, null, null, cenario.usuario()));
		ContaFinanceira conta = new ContaFinanceira(cenario.empresa(), titular, instituicao,
				"Conta " + agencia + "/" + numeroConta, TipoContaFinanceira.CONTA_CORRENTE, BigDecimal.ZERO,
				LocalDate.of(2026, 1, 1), true, cenario.usuario());
		conta.atualizarIdentificacaoBancaria(agencia, numeroConta, null);
		return contaRepository.saveAndFlush(conta);
	}

	private MockMultipartFile arquivo(String nome, String conteudo) {
		return new MockMultipartFile("arquivo", nome, "application/x-ofx", conteudo.getBytes(StandardCharsets.UTF_8));
	}

	private MockMultipartFile arquivoOfx(String nome, String transacoes) {
		return arquivo(nome, "OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKTRANLIST>" + transacoes
				+ "</BANKTRANLIST></OFX>");
	}

	private MockMultipartFile arquivoOfx(
			String nome, String transacoes, String bankId, String branchId, String acctId) {
		return arquivo(nome, "OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKACCTFROM><BANKID>" + bankId
				+ "<BRANCHID>" + branchId + "<ACCTID>" + acctId + "<ACCTTYPE>CHECKING</BANKACCTFROM>"
				+ "<BANKTRANLIST>" + transacoes + "</BANKTRANLIST></OFX>");
	}

	private MockMultipartFile arquivoCsv(String nome, String conteudo) {
		return new MockMultipartFile("arquivo", nome, "text/csv", conteudo.getBytes(StandardCharsets.UTF_8));
	}

	private MockMultipartFile arquivoXlsx(String nome, byte[] conteudo) {
		return new MockMultipartFile("arquivo", nome,
				"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", conteudo);
	}

	private byte[] xlsx(String primeiroId, String segundoId) throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Movimentos");
			Row cabecalho = sheet.createRow(0);
			cabecalho.createCell(0).setCellValue("Data");
			cabecalho.createCell(1).setCellValue("Descricao");
			cabecalho.createCell(2).setCellValue("Valor");
			cabecalho.createCell(3).setCellValue("Identificador");
			preencherXlsx(sheet.createRow(1), "2026-08-01", "Entrada", 10.00, primeiroId);
			preencherXlsx(sheet.createRow(2), "2026-08-02", "Saida", -5.00, segundoId);
			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			workbook.write(saida);
			return saida.toByteArray();
		}
	}

	private void preencherXlsx(Row row, String data, String descricao, double valor, String id) {
		row.createCell(0).setCellValue(data);
		row.createCell(1).setCellValue(descricao);
		row.createCell(2).setCellValue(valor);
		row.createCell(3).setCellValue(id);
	}

	private String umaTransacao(String fitid, String memo) {
		return transacaoOfx(fitid, memo);
	}

	private String transacaoOfx(String fitid, String memo) {
		return "<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260731<TRNAMT>-10.00<FITID>" + fitid + "<MEMO>" + memo
				+ "</STMTTRN>";
	}

	private Cenario cenario(String cnpj, PerfilUsuario perfil) throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Conta Opcional", "Empresa Conta Opcional", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("Usuario Conta Opcional",
				"conta-opcional." + cnpj + "@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		return new Cenario(empresa, usuario, autenticarNaEmpresa(usuario.getEmail(), empresa.getId()));
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

	private record Cenario(Empresa empresa, Usuario usuario, MockHttpSession session) {
	}
}
