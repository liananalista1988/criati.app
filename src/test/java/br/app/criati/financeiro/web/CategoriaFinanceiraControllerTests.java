package br.app.criati.financeiro.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoriaFinanceiraControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/financeiro/categorias";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private CategoriaFinanceiraRepository categoriaFinanceiraRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void administradorCriaCategoriaDeReceitaEDespesa() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000181");
		Usuario admin = criarUsuario("categoria.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Vendas", "tipo": "RECEITA" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("RECEITA"));

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Aluguel", "tipo": "DESPESA" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("DESPESA"));
	}

	@Test
	void naoPermiteDuplicidadeDeNomeMesmoTipoNaMesmaEmpresa() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000182");
		Usuario admin = criarUsuario("categoria.duplicada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Vendas", "tipo": "RECEITA" }
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Vendas", "tipo": "RECEITA" }
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void mesmoNomeDeCategoriaEmEmpresasDiferentesEPermitido() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("33333333000183");
		Empresa empresaB = criarEmpresaComFinanceiro("44444444000184");
		Usuario adminA = criarUsuario("categoria.empresaA@criati.test");
		Usuario adminB = criarUsuario("categoria.empresaB@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());

		mockMvc.perform(post(URL_BASE).session(sessaoA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Vendas", "tipo": "RECEITA" }
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(URL_BASE).session(sessaoB).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Vendas", "tipo": "RECEITA" }
						"""))
				.andExpect(status().isCreated());
	}

	@Test
	void gestorNaoPodeInativarCategoria() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000185");
		Usuario gestor = criarUsuario("categoria.gestor@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + categoria.getId() + "/inativar").session(session).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveInativarEReativarCategoria() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000186");
		Usuario admin = criarUsuario("categoria.toggle@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(empresa, "Servicos", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + categoria.getId() + "/inativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INATIVO"));

		mockMvc.perform(post(URL_BASE + "/" + categoria.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void empresaANaoConsultaCategoriaDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("77777777000187");
		Empresa empresaB = criarEmpresaComFinanceiro("88888888000188");
		CategoriaFinanceira categoriaB = criarCategoria(empresaB, "Categoria B", TipoFinanceiro.DESPESA);

		Usuario usuarioA = criarUsuario("categoria.empresaA.consulta@criati.test");
		criarVinculo(usuarioA, empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(usuarioA.getEmail(), empresaA.getId());

		mockMvc.perform(get(URL_BASE + "/" + categoriaB.getId()).session(sessaoA))
				.andExpect(status().isNotFound());
	}

	@Test
	void usuarioComumPodeListarCategorias() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000191");
		Usuario usuario = criarUsuario("categoria.usuario.lista@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO);
		criarCategoria(empresa, "Vendas", TipoFinanceiro.RECEITA);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_BASE).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void criaSubcategoriaComHierarquiaOrdemEOrcamento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("12121212000192");
		Usuario admin = criarUsuario("categoria.hierarquia@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		String paiId = criarViaApi(session, "Alimentação", null, "DESPESA");
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "nome": "Supermercado", "descricao": "Compras do mês", "categoriaPaiId": "%s",
						  "tipo": "DESPESA", "ordem": 3, "permiteOrcamento": true }
						""".formatted(paiId)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nivel").value(2))
				.andExpect(jsonPath("$.categoriaPaiNome").value("Alimentação"))
				.andExpect(jsonPath("$.ordem").value(3)).andExpect(jsonPath("$.permiteOrcamento").value(true));
	}

	@Test
	void rejeitaPaiDeOutraEmpresaENaturezaIncompativel() throws Exception {
		Empresa empresaA = criarEmpresaComFinanceiro("13131313000193");
		Empresa empresaB = criarEmpresaComFinanceiro("14141414000194");
		Usuario adminA = criarUsuario("categoria.pai.a@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("categoria.pai.b@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());
		String paiOutraEmpresa = criarViaApi(sessaoB, "Moradia", null, "DESPESA");

		mockMvc.perform(post(URL_BASE).session(sessaoA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Filha", paiOutraEmpresa, "DESPESA"))).andExpect(status().isNotFound());
		String paiReceita = criarViaApi(sessaoA, "Receitas", null, "RECEITA");
		mockMvc.perform(post(URL_BASE).session(sessaoA).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Filha", paiReceita, "DESPESA"))).andExpect(status().isBadRequest());
	}

	@Test
	void duplicidadeNormalizadaNoMesmoPaiERejeitadaMasPaisDiferentesPermitem() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("15151515000195");
		Usuario admin = criarUsuario("categoria.normalizada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String paiA = criarViaApi(session, "Casa", null, "DESPESA");
		String paiB = criarViaApi(session, "Pessoal", null, "DESPESA");
		criarViaApi(session, "Super Mercado", paiA, "DESPESA");
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("  super   mercado ", paiA, "DESPESA"))).andExpect(status().isConflict());
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Super Mercado", paiB, "DESPESA"))).andExpect(status().isCreated());
	}

	@Test
	void rejeitaAutorreferenciaTerceiroNivelECiclo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("16161616000196");
		Usuario admin = criarUsuario("categoria.ciclo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String pai = criarViaApi(session, "Moradia", null, "DESPESA");
		String filha = criarViaApi(session, "Energia", pai, "DESPESA");
		mockMvc.perform(put(URL_BASE + "/" + pai).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Moradia", pai, "DESPESA"))).andExpect(status().isBadRequest());
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Taxa", filha, "DESPESA"))).andExpect(status().isBadRequest());
		mockMvc.perform(put(URL_BASE + "/" + pai).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Moradia", filha, "DESPESA"))).andExpect(status().isBadRequest());
	}

	@Test
	void filtraPesquisaPrincipaisPaiNaturezaEOrcamento() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("17171717000197");
		Usuario admin = criarUsuario("categoria.filtros@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String pai = criarViaApi(session, "Alimentação", null, "DESPESA");
		criarViaApi(session, "Restaurante", pai, "DESPESA");
		criarViaApi(session, "Salário", null, "RECEITA");

		mockMvc.perform(get(URL_BASE).session(session).param("paiId", pai).param("busca", "rest"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL_BASE).session(session).param("principais", "true").param("tipo", "DESPESA"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL_BASE).session(session).param("permiteOrcamento", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
		mockMvc.perform(get(URL_BASE + "/resumo").session(session)).andExpect(status().isOk())
				.andExpect(jsonPath("$.ativas").value(3)).andExpect(jsonPath("$.subcategorias").value(1));
	}

	@Test
	void inativasFicamForaDaListaPadraoEReativacaoValidaDuplicidade() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("18181818000198");
		Usuario admin = criarUsuario("categoria.lista.ativa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarViaApi(session, "Tarifas", null, "DESPESA");
		mockMvc.perform(post(URL_BASE + "/" + id + "/inativar").session(session).with(csrf())).andExpect(status().isOk());
		mockMvc.perform(get(URL_BASE).session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		criarViaApi(session, "tarifas", null, "DESPESA");
		mockMvc.perform(post(URL_BASE + "/" + id + "/reativar").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void naoInativaPaiComFilhosAtivosSemDecisaoExplicita() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("19191919000199");
		Usuario admin = criarUsuario("categoria.inativa.pai@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String pai = criarViaApi(session, "Saúde", null, "DESPESA");
		criarViaApi(session, "Farmácia", pai, "DESPESA");
		mockMvc.perform(post(URL_BASE + "/" + pai + "/inativar").session(session).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void naoMoveNemAlteraNaturezaDeCategoriaQuePossuiFilhas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("21212121000191");
		Usuario admin = criarUsuario("categoria.pai.estavel@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String pai = criarViaApi(session, "Moradia", null, "DESPESA");
		criarViaApi(session, "Água", pai, "DESPESA");
		String outroPai = criarViaApi(session, "Casa", null, "DESPESA");

		mockMvc.perform(put(URL_BASE + "/" + pai).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Moradia", outroPai, "DESPESA"))).andExpect(status().isBadRequest());
		mockMvc.perform(put(URL_BASE + "/" + pai).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Moradia", null, "RECEITA"))).andExpect(status().isBadRequest());
	}

	@Test
	void apiExigeAutenticacaoCsrfEAdministrador() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
		Empresa empresa = criarEmpresaComFinanceiro("20202020000190");
		Usuario gestor = criarUsuario("categoria.seguranca@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR);
		MockHttpSession session = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());
		mockMvc.perform(post(URL_BASE).session(session).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Teste", null, "DESPESA"))).andExpect(status().isForbidden());
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(payload("Teste", null, "DESPESA"))).andExpect(status().isForbidden());
	}

	private String criarViaApi(MockHttpSession session, String nome, String paiId, String tipo) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(payload(nome, paiId, tipo)))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String payload(String nome, String paiId, String tipo) {
		String pai = paiId == null ? "null" : "\"" + paiId + "\"";
		return "{\"nome\":\"" + nome + "\",\"categoriaPaiId\":" + pai + ",\"tipo\":\"" + tipo
				+ "\",\"ordem\":0,\"permiteOrcamento\":" + ("DESPESA".equals(tipo)) + "}";
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
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

	private MockHttpSession autenticarNaEmpresa(String email, java.util.UUID empresaId) throws Exception {
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
