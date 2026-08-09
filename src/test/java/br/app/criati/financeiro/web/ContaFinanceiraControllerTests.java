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
@Transactional
class ContaFinanceiraControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL = "/api/contexto/financeiro/contas";
	private static final LocalDate DATA = LocalDate.of(2025, 1, 15);

	@Autowired private MockMvc mockMvc;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private PessoaFinanceiraRepository pessoaRepository;
	@Autowired private InstituicaoFinanceiraRepository instituicaoRepository;
	@Autowired private ContaFinanceiraRepository contaRepository;
	@Autowired private LancamentoFinanceiroRepository lancamentoRepository;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void criaContaBancariaCompletaNaEmpresaAtiva() throws Exception {
		Cenario c = cenario("11111111000401", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), c.instituicao().getId(), "Banco da casa", "CONTA_CORRENTE", "500.25", DATA, true)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Banco da casa"))
				.andExpect(jsonPath("$.titularId").value(c.pessoa().getId().toString()))
				.andExpect(jsonPath("$.instituicaoNome").value("Banco Teste"))
				.andExpect(jsonPath("$.moeda").value("BRL"))
				.andExpect(jsonPath("$.saldoInicial").value(500.25))
				.andExpect(jsonPath("$.dataSaldoInicial").value(DATA.toString()))
				.andExpect(jsonPath("$.permiteConciliacao").value(true));
		assertThat(lancamentoRepository.count()).isZero();
	}

	@Test
	void aceitaSaldosPositivoZeroENegativoSemGerarLancamentos() throws Exception {
		Cenario c = cenario("11111111000402", PerfilUsuario.ADMINISTRADOR);
		for (String saldo : new String[] { "10.00", "0", "-50.75" }) {
			mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(json(c.pessoa().getId(), null, "Carteira " + saldo, "CARTEIRA", saldo, DATA, false)))
					.andExpect(status().isCreated());
		}
		assertThat(contaRepository.findAllByEmpresaId(c.empresa().getId())).hasSize(3);
		assertThat(lancamentoRepository.count()).isZero();
	}

	@Test
	void validaNomeTitularTipoMoedaEData() throws Exception {
		Cenario c = cenario("11111111000403", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\",\"moeda\":\"USD\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.nome").exists())
				.andExpect(jsonPath("$.fieldErrors.titularId").exists())
				.andExpect(jsonPath("$.fieldErrors.tipo").exists());
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), c.instituicao().getId(), "Futura", "CONTA_CORRENTE", "0", LocalDate.now().plusDays(1), true)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void exigeInstituicaoParaContaBancariaMasNaoParaDinheiro() throws Exception {
		Cenario c = cenario("11111111000404", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), null, "Corrente", "CONTA_CORRENTE", "0", DATA, true)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), null, "Dinheiro", "DINHEIRO", "0", DATA, false)))
				.andExpect(status().isCreated());
	}

	@Test
	void rejeitaConciliacaoParaDinheiroOuCarteira() throws Exception {
		Cenario c = cenario("11111111000405", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), null, "Carteira", "CARTEIRA", "0", DATA, true)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void rejeitaTitularEInstituicaoDeOutroTenant() throws Exception {
		Cenario a = cenario("11111111000406", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("22222222000406", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(post(URL).session(a.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(b.pessoa().getId(), a.instituicao().getId(), "Titular alheio", "CONTA_CORRENTE", "0", DATA, true)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(URL).session(a.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(a.pessoa().getId(), b.instituicao().getId(), "Banco alheio", "CONTA_CORRENTE", "0", DATA, true)))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejeitaTitularInativoEmNovaConta() throws Exception {
		Cenario c = cenario("11111111000407", PerfilUsuario.ADMINISTRADOR);
		c.pessoa().desativar(c.admin()); pessoaRepository.saveAndFlush(c.pessoa());
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), null, "Carteira", "CARTEIRA", "0", DATA, false)))
				.andExpect(status().isConflict());
	}

	@Test
	void nomeDuplicadoGeraAlertaMasNaoBloqueia() throws Exception {
		Cenario c = cenario("11111111000408", PerfilUsuario.ADMINISTRADOR);
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
					.content(json(c.pessoa().getId(), null, "Mesmo nome", "CARTEIRA", "0", DATA, false)))
					.andExpect(status().isCreated()).andExpect(jsonPath("$.possivelDuplicidade").value(i == 1));
		}
	}

	@Test
	void atualizaContaERegistraAuditoria() throws Exception {
		Cenario c = cenario("11111111000409", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = criarConta(c, "Antes", BigDecimal.ZERO);
		mockMvc.perform(put(URL + "/" + conta.getId()).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(c.pessoa().getId(), c.instituicao().getId(), "Depois", "CONTA_PAGAMENTO", "-10", DATA, true)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Depois"));
		ContaFinanceira atualizada = contaRepository.findByIdAndEmpresaId(conta.getId(), c.empresa().getId()).orElseThrow();
		assertThat(atualizada.getAtualizadoPor().getId()).isEqualTo(c.admin().getId());
	}

	@Test
	void desativaReativaEPreservaConta() throws Exception {
		Cenario c = cenario("11111111000410", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira conta = criarConta(c, "Conta", BigDecimal.ZERO);
		mockMvc.perform(post(URL + "/" + conta.getId() + "/inativar").session(c.session()).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
		mockMvc.perform(get(URL).session(c.session())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(post(URL + "/" + conta.getId() + "/reativar").session(c.session()).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
		assertThat(contaRepository.findById(conta.getId())).isPresent();
	}

	@Test
	void listaPesquisaFiltraEResumeSemVazarTenant() throws Exception {
		Cenario a = cenario("11111111000411", PerfilUsuario.USUARIO);
		Cenario b = cenario("22222222000411", PerfilUsuario.ADMINISTRADOR);
		criarConta(a, "Inter A", new BigDecimal("100.00")); criarConta(b, "Inter B", new BigDecimal("900.00"));
		mockMvc.perform(get(URL).param("busca", "inter").param("titularId", a.pessoa().getId().toString())
				.param("instituicaoId", a.instituicao().getId().toString()).session(a.session()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Inter A"))
				.andExpect(jsonPath("$[1]").doesNotExist());
		mockMvc.perform(get(URL + "/resumo").session(a.session())).andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeContasAtivas").value(1))
				.andExpect(jsonPath("$.saldoInicialConsolidado").value(100.00));
	}

	@Test
	void contaDeOutroTenantNaoPodeSerConsultadaAlteradaOuDesativada() throws Exception {
		Cenario a = cenario("11111111000412", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("22222222000412", PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaB = criarConta(b, "Conta B", BigDecimal.ZERO);
		mockMvc.perform(get(URL + "/" + contaB.getId()).session(a.session())).andExpect(status().isNotFound());
		mockMvc.perform(put(URL + "/" + contaB.getId()).session(a.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(json(a.pessoa().getId(), a.instituicao().getId(), "Alterada", "CONTA_CORRENTE", "0", DATA, true)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(URL + "/" + contaB.getId() + "/inativar").session(a.session()).with(csrf()))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaTitularesInstituicoesECadastraInstituicaoLocal() throws Exception {
		Cenario c = cenario("11111111000413", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(get(URL + "/titulares").session(c.session())).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(c.pessoa().getId().toString()));
		mockMvc.perform(get(URL + "/instituicoes").session(c.session())).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nome").value("Banco Teste"));
		mockMvc.perform(post(URL + "/instituicoes").session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\" Nova Instituicao \",\"codigo\":\"999\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Nova Instituicao"));
	}

	// CRIATI-IMP-FIX-007, item 7: agencia/numero/digito completos so vao para
	// ADMINISTRADOR (unico perfil que tambem pode editar a conta); GESTOR e
	// USUARIO, com acesso apenas de leitura, recebem os valores mascarados.
	@Test
	void mascaraIdentificacaoBancariaParaPerfisSomenteLeituraEExibeCompletaParaAdministrador() throws Exception {
		Cenario admin = cenario("11111111000415", PerfilUsuario.ADMINISTRADOR);
		String corpoComIdentificacao = ("{\"nome\":\"Conta com identificacao\",\"titularId\":\"%s\",\"instituicaoId\":\"%s\","
				+ "\"tipo\":\"CONTA_CORRENTE\",\"moeda\":\"BRL\",\"saldoInicial\":0,\"dataSaldoInicial\":\"%s\","
				+ "\"permiteConciliacao\":true,\"agenciaBancaria\":\"0001\",\"numeroContaBancaria\":\"654321\",\"digitoContaBancaria\":\"9\"}")
				.formatted(admin.pessoa().getId(), admin.instituicao().getId(), DATA);
		MvcResult criada = mockMvc.perform(post(URL).session(admin.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpoComIdentificacao))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.agenciaBancaria").value("0001"))
				.andExpect(jsonPath("$.numeroContaBancaria").value("654321"))
				.andExpect(jsonPath("$.digitoContaBancaria").value("9"))
				.andReturn();
		String contaId = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

		Usuario leitor = criarUsuario("leitor.mascaramento@criati.test");
		criarVinculo(leitor, admin.empresa(), PerfilUsuario.USUARIO);
		MockHttpSession sessaoLeitor = autenticarNaEmpresa(leitor.getEmail(), admin.empresa().getId());

		mockMvc.perform(get(URL + "/" + contaId).session(sessaoLeitor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.agenciaBancaria").value("••••"))
				.andExpect(jsonPath("$.numeroContaBancaria").value("••4321"));
		mockMvc.perform(get(URL).session(sessaoLeitor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].digitoContaBancaria").value("•"));
		mockMvc.perform(get(URL + "/" + contaId).session(admin.session()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.agenciaBancaria").value("0001"))
				.andExpect(jsonPath("$.numeroContaBancaria").value("654321"))
				.andExpect(jsonPath("$.digitoContaBancaria").value("9"));
	}

	@Test
	void bloqueiaEscritaSemPerfilOuCsrfEPreservaLeituraAutenticada() throws Exception {
		Cenario c = cenario("11111111000414", PerfilUsuario.GESTOR);
		String corpo = json(c.pessoa().getId(), null, "Carteira", "CARTEIRA", "0", DATA, false);
		mockMvc.perform(post(URL).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(URL).session(c.session()).contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(URL).session(c.session())).andExpect(status().isOk());
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

	private Cenario cenario(String cnpj, PerfilUsuario perfil) throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(new Empresa("Empresa Teste", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario admin = criarUsuario("usuario." + cnpj + "@criati.test"); criarVinculo(admin, empresa, perfil);
		PessoaFinanceira pessoa = pessoaRepository.saveAndFlush(new PessoaFinanceira(empresa, "Titular Teste", null, null, admin));
		InstituicaoFinanceira instituicao = instituicaoRepository.saveAndFlush(new InstituicaoFinanceira(empresa, "Banco Teste", "123", admin));
		return new Cenario(empresa, admin, pessoa, instituicao, autenticarNaEmpresa(admin.getEmail(), empresa.getId()));
	}

	private ContaFinanceira criarConta(Cenario c, String nome, BigDecimal saldo) {
		return contaRepository.saveAndFlush(new ContaFinanceira(c.empresa(), c.pessoa(), c.instituicao(), nome,
				TipoContaFinanceira.CONTA_CORRENTE, saldo, DATA, true, c.admin()));
	}

	private String json(UUID titularId, UUID instituicaoId, String nome, String tipo, String saldo, LocalDate data, boolean conciliacao) {
		return "{\"nome\":\"%s\",\"titularId\":\"%s\",\"instituicaoId\":%s,\"tipo\":\"%s\",\"moeda\":\"BRL\",\"saldoInicial\":%s,\"dataSaldoInicial\":\"%s\",\"permiteConciliacao\":%s}"
				.formatted(nome, titularId, instituicaoId == null ? "null" : "\"" + instituicaoId + "\"", tipo, saldo, data, conciliacao);
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		return usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
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

	private record Cenario(
			Empresa empresa, Usuario admin, PessoaFinanceira pessoa,
			InstituicaoFinanceira instituicao, MockHttpSession session) {
	}
}
