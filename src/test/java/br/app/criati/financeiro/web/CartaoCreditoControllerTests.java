package br.app.criati.financeiro.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CartaoCreditoControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/financeiro/cartoes";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private EmpresaRepository empresaRepository;
	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired
	private PessoaFinanceiraRepository pessoaFinanceiraRepository;
	@Autowired
	private InstituicaoFinanceiraRepository instituicaoFinanceiraRepository;
	@Autowired
	private AplicacaoService aplicacaoService;
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarCartaoFisicoComLimiteFechamentoEVencimento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000511");
		Usuario admin = criarUsuario("cartao.fisico@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Cartao BB","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","ultimosQuatroDigitos":"1234","limiteTotal":5000.00,
						 "limiteSaudavel":2000.00,"diaFechamento":5,"diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("FISICO"))
				.andExpect(jsonPath("$.limiteTotal").value(5000.00))
				.andExpect(jsonPath("$.limiteDisponivel").value(5000.00))
				.andExpect(jsonPath("$.diaFechamento").value(5))
				.andExpect(jsonPath("$.diaVencimento").value(12))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.bloqueado").value(false));
	}

	@Test
	void deveCriarCartaoVirtualHerdandoDadosDoPrincipalQuandoNaoInformados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000512");
		Usuario admin = criarUsuario("cartao.virtual.heranca@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "3000.00");

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Cartao Virtual Compras","tipo":"VIRTUAL","cartaoPrincipalId":"%s",
						 "ultimosQuatroDigitos":"9999"}
						""".formatted(principalId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("VIRTUAL"))
				.andExpect(jsonPath("$.titularId").value(pessoa.getId().toString()))
				.andExpect(jsonPath("$.instituicaoId").value(instituicao.getId().toString()))
				.andExpect(jsonPath("$.bandeira").value("VISA"))
				.andExpect(jsonPath("$.limiteTotal").value(3000.00))
				.andExpect(jsonPath("$.limiteDisponivel").value(3000.00));
	}

	@Test
	void resumoConsolidadoNaoDuplicaLimiteDoVirtual() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000513");
		Usuario admin = criarUsuario("cartao.resumo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "4000.00");
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual 1","tipo":"VIRTUAL","cartaoPrincipalId":"%s"}
						""".formatted(principalId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get(URL_BASE + "/resumo").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidadeFisicos").value(1))
				.andExpect(jsonPath("$.quantidadeVirtuais").value(1))
				.andExpect(jsonPath("$.limiteTotalConsolidado").value(4000.00))
				.andExpect(jsonPath("$.limiteDisponivelConsolidado").value(4000.00));
	}

	@Test
	void cartaoFisicoNaoPodePossuirCartaoPrincipal() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000514");
		Usuario admin = criarUsuario("cartao.fisico.com.principal@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "1000.00");

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Fisico invalido","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "cartaoPrincipalId":"%s","bandeira":"VISA","limiteTotal":100.00,"diaFechamento":5,
						 "diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId(), principalId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cartaoVirtualExigePrincipal() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000515");
		Usuario admin = criarUsuario("cartao.virtual.sem.principal@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual sem principal","titularId":"%s","instituicaoId":"%s","tipo":"VIRTUAL",
						 "bandeira":"VISA"}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cartaoPrincipalDeveSerFisicoNuncaOutroVirtual() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000516");
		Usuario admin = criarUsuario("cartao.principal.deve.ser.fisico@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "1000.00");
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual 1","tipo":"VIRTUAL","cartaoPrincipalId":"%s"}
						""".formatted(principalId)))
				.andExpect(status().isCreated()).andReturn();
		String virtualId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual de virtual","tipo":"VIRTUAL","cartaoPrincipalId":"%s"}
						""".formatted(virtualId)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void limiteSaudavelAcimaDoTotalERejeitado() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("77777777000517");
		Usuario admin = criarUsuario("cartao.limite.saudavel.invalido@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Limite invalido","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","limiteTotal":1000.00,"limiteSaudavel":2000.00,"diaFechamento":5,
						 "diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void ultimosQuatroDigitosInvalidosSaoRejeitados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("88888888000518");
		Usuario admin = criarUsuario("cartao.digitos.invalidos@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Digitos invalidos","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","ultimosQuatroDigitos":"12a4","limiteTotal":1000.00,"diaFechamento":5,
						 "diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void fechamentoEVencimentoIguaisSaoRejeitados() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("99999999000519");
		Usuario admin = criarUsuario("cartao.fechamento.vencimento.iguais@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Datas iguais","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","limiteTotal":1000.00,"diaFechamento":10,"diaVencimento":10}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveBloquearEDesbloquearCartao() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("10101010000520");
		Usuario admin = criarUsuario("cartao.bloqueio@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarCartaoFisico(session, pessoa, instituicao, "Cartao a Bloquear", "1000.00");

		mockMvc.perform(post(URL_BASE + "/" + id + "/bloquear").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"motivo":"Suspeita de fraude"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bloqueado").value(true))
				.andExpect(jsonPath("$.motivoBloqueio").value("Suspeita de fraude"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/desbloquear").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.bloqueado").value(false));
	}

	@Test
	void bloqueioDoPrincipalRefleteNoEfetivoDoVirtualSemAlterarSeuProprioCampo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("12121212000521");
		Usuario admin = criarUsuario("cartao.bloqueio.efetivo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "1000.00");
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual","tipo":"VIRTUAL","cartaoPrincipalId":"%s"}
						""".formatted(principalId)))
				.andExpect(status().isCreated()).andReturn();
		String virtualId = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE + "/" + principalId + "/bloquear").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isOk());

		mockMvc.perform(get(URL_BASE + "/" + virtualId).session(session))
				.andExpect(jsonPath("$.bloqueado").value(false))
				.andExpect(jsonPath("$.bloqueadoEfetivo").value(true));
	}

	@Test
	void deveInativarEReativarCartao() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("13131313000522");
		Usuario admin = criarUsuario("cartao.inativacao@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarCartaoFisico(session, pessoa, instituicao, "Cartao a Inativar", "1000.00");

		mockMvc.perform(post(URL_BASE + "/" + id + "/inativar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
		mockMvc.perform(post(URL_BASE + "/" + id + "/reativar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void listaFiltraPorTitularTipoStatusEBloqueadoSemVazarTenant() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("14141414000523");
		Empresa b = criarEmpresaComFinanceiro("15151515000524");
		Usuario adminA = criarUsuario("cartao.tenant.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("cartao.tenant.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		PessoaFinanceira pessoaB = criarPessoa(b, adminB, "Pessoa B");
		InstituicaoFinanceira instituicaoA = criarInstituicao(a, adminA, "Banco A");
		InstituicaoFinanceira instituicaoB = criarInstituicao(b, adminB, "Banco B");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		MockHttpSession sessionB = autenticarNaEmpresa(adminB.getEmail(), b.getId());
		String idA = criarCartaoFisico(sessionA, pessoaA, instituicaoA, "Cartao A", "1000.00");
		criarCartaoFisico(sessionB, pessoaB, instituicaoB, "Cartao B", "2000.00");

		mockMvc.perform(get(URL_BASE).session(sessionA).param("titularId", pessoaA.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(idA));

		mockMvc.perform(get(URL_BASE + "/" + idA).session(sessionB)).andExpect(status().isNotFound());

		// titular/instituicao de outro tenant sao rejeitados na criacao
		mockMvc.perform(post(URL_BASE).session(sessionA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Cross tenant","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","limiteTotal":100.00,"diaFechamento":5,"diaVencimento":12}
						""".formatted(pessoaB.getId(), instituicaoA.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaCartoesVirtuaisDeUmPrincipal() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("16161616000525");
		Usuario admin = criarUsuario("cartao.listar.virtuais@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String principalId = criarCartaoFisico(session, pessoa, instituicao, "Cartao Principal", "1000.00");
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Virtual 1","tipo":"VIRTUAL","cartaoPrincipalId":"%s"}
						""".formatted(principalId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get(URL_BASE + "/" + principalId + "/virtuais").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].tipo").value("VIRTUAL"));
	}

	@Test
	void endpointsExigemAutenticacao() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
	}

	@Test
	void escritaSemTokenCsrfERejeitada() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("17171717000526");
		Usuario admin = criarUsuario("cartao.csrf@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Sem csrf","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","limiteTotal":100.00,"diaFechamento":5,"diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void perfilGestorOuUsuarioNaoPodeEscrever() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("18181818000527");
		Usuario admin = criarUsuario("cartao.perfil.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario gestor = criarUsuario("cartao.perfil.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		InstituicaoFinanceira instituicao = criarInstituicao(empresa, admin, "Banco Teste");
		MockHttpSession sessionGestor = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(sessionGestor).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"Bloqueado","titularId":"%s","instituicaoId":"%s","tipo":"FISICO",
						 "bandeira":"VISA","limiteTotal":100.00,"diaFechamento":5,"diaVencimento":12}
						""".formatted(pessoa.getId(), instituicao.getId())))
				.andExpect(status().isForbidden());
	}

	@Test
	void paginaVaziaExibeMensagemQuandoNaoHaCartoes() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("19191919000528");
		Usuario admin = criarUsuario("cartao.vazio@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_BASE).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	/* ==================== HELPERS ==================== */

	private String criarCartaoFisico(MockHttpSession session, PessoaFinanceira pessoa, InstituicaoFinanceira instituicao,
			String nome, String limiteTotal) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"nome":"%s","titularId":"%s","instituicaoId":"%s","tipo":"FISICO","bandeira":"VISA",
						 "limiteTotal":%s,"diaFechamento":5,"diaVencimento":12}
						""".formatted(nome, pessoa.getId(), instituicao.getId(), limiteTotal)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaFinanceiraRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private InstituicaoFinanceira criarInstituicao(Empresa empresa, Usuario autor, String nome) {
		return instituicaoFinanceiraRepository.saveAndFlush(new InstituicaoFinanceira(empresa, nome, "999", autor));
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
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
