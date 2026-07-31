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
import java.util.HexFormat;
import java.util.UUID;

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

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
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
		mockMvc.perform(get(URL + "/" + loteId).session(b.session())).andExpect(status().isNotFound());
		mockMvc.perform(post(URL + "/" + loteId + "/descartar").session(b.session()).with(csrf()))
				.andExpect(status().isNotFound());
		mockMvc.perform(get(URL).session(b.session())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
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
