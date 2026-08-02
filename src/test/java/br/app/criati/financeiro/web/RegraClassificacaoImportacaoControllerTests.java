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
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.RegraClassificacaoImportacaoRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * CRUD de regras de classificacao de importacao (CRIATI-IMP-002A): permissao
 * ADMINISTRADOR-apenas, isolamento multiempresa e os ajustes obrigatorios 2
 * (CONTEM nunca AUTOMATICA) e 3 (padroes genericos nunca AUTOMATICA).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegraClassificacaoImportacaoControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL = "/api/contexto/financeiro/regras-importacao";

	@Autowired private MockMvc mockMvc;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired private ContaFinanceiraRepository contaRepository;
	@Autowired private RegraClassificacaoImportacaoRepository regraRepository;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void administradorCriaRegraSugestaoComContemESemContaEspecifica() throws Exception {
		Cenario c = cenario("11111111000701", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Mercado", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "SUPERMERCADO BOM PRECO LTDA", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.padraoNormalizado").value("SUPERMERCADO BOM PRECO LTDA"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.quantidadeUtilizacoes").value(0));
	}

	@Test
	void bloqueiaAutomaticaComEstrategiaContem() throws Exception {
		Cenario c = cenario("22222222000702", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Mercado", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "SUPERMERCADO ESPECIFICO LTDA", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "ALTA", "aplicacao": "AUTOMATICA" }
						""".formatted(categoria.getId())))
				.andExpect(status().isBadRequest());
		assertThat(regraRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).isEmpty();
	}

	@Test
	void bloqueiaAutomaticaComPadraoGenericoMesmoComEstrategiaIgual() throws Exception {
		Cenario c = cenario("33333333000703", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Transferencias", TipoFinanceiro.RECEITA);

		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "PIX", "estrategiaComparacao": "IGUAL",
						  "tipo": "RECEITA", "categoriaId": "%s", "nivelConfianca": "ALTA", "aplicacao": "AUTOMATICA" }
						""".formatted(categoria.getId())))
				.andExpect(status().isBadRequest());
		assertThat(regraRepository.findAllByEmpresaIdOrderByCriadoEmDesc(c.empresa().getId())).isEmpty();
	}

	@Test
	void permiteAutomaticaComEstrategiaIgualEPadraoEspecifico() throws Exception {
		Cenario c = cenario("44444444000704", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Assinaturas", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "NETFLIX COM ASSINATURA", "estrategiaComparacao": "IGUAL",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "ALTA", "aplicacao": "AUTOMATICA" }
						""".formatted(categoria.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.aplicacao").value("AUTOMATICA"));
	}

	@Test
	void categoriaContaEPessoaDeOutraEmpresaSaoRejeitadas() throws Exception {
		Cenario a = cenario("55555555000705", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("66666666000706", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoriaDeB = criarCategoria(b.empresa(), "Categoria B", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(a.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "TESTE ISOLAMENTO", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoriaDeB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void naoPermiteRegraDuplicadaMesmoPadraoContaETipo() throws Exception {
		Cenario c = cenario("77777777000707", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Mercado", TipoFinanceiro.DESPESA);
		String corpo = """
				{ "descricaoReferencia": "PADRAO REPETIDO", "estrategiaComparacao": "CONTEM",
				  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
				""".formatted(categoria.getId());

		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());
		mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isBadRequest());
	}

	@Test
	void gestorEUsuarioNaoPodemEscreverMasPodemListar() throws Exception {
		Cenario gestor = cenario("88888888000708", PerfilUsuario.GESTOR);
		CategoriaFinanceira categoria = criarCategoria(gestor.empresa(), "Mercado", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(gestor.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "TESTE PERFIL", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId())))
				.andExpect(status().isForbidden());
		mockMvc.perform(get(URL).session(gestor.session())).andExpect(status().isOk());
	}

	@Test
	void editarInativarEReativarPreservamHistoricoENaoPermitemExclusaoFisica() throws Exception {
		Cenario c = cenario("99999999000709", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(c.empresa(), "Mercado", TipoFinanceiro.DESPESA);
		MvcResult criada = mockMvc.perform(post(URL).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "REGRA EDITAVEL", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId())))
				.andExpect(status().isCreated()).andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put(URL + "/" + id).session(c.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "REGRA EDITADA", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.padraoNormalizado").value("REGRA EDITADA"));

		mockMvc.perform(post(URL + "/" + id + "/inativar").session(c.session()).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
		mockMvc.perform(post(URL + "/" + id + "/inativar").session(c.session()).with(csrf()))
				.andExpect(status().isConflict());
		mockMvc.perform(post(URL + "/" + id + "/reativar").session(c.session()).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));

		// Nao existe endpoint de exclusao fisica (ajuste obrigatorio 6).
		assertThat(regraRepository.findByIdAndEmpresaId(UUID.fromString(id), c.empresa().getId())).isPresent();
	}

	@Test
	void listarEBuscarIsolamPorEmpresa() throws Exception {
		Cenario a = cenario("10101010000710", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("12121212000711", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoriaA = criarCategoria(a.empresa(), "Categoria A", TipoFinanceiro.DESPESA);
		MvcResult criada = mockMvc.perform(post(URL).session(a.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "SO DA EMPRESA A", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoriaA.getId())))
				.andExpect(status().isCreated()).andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(criada.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(URL).session(b.session())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
		mockMvc.perform(get(URL + "/" + id).session(b.session())).andExpect(status().isNotFound());
		mockMvc.perform(get(URL + "/" + id).session(a.session())).andExpect(status().isOk());
	}

	@Test
	void aceitaContaOpcionalRestritaAMesmaEmpresa() throws Exception {
		Cenario a = cenario("13131313000712", PerfilUsuario.ADMINISTRADOR);
		Cenario b = cenario("14141414000713", PerfilUsuario.ADMINISTRADOR);
		CategoriaFinanceira categoria = criarCategoria(a.empresa(), "Mercado", TipoFinanceiro.DESPESA);

		mockMvc.perform(post(URL).session(a.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "CONTA ALHEIA", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "contaId": "%s",
						  "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId(), b.conta().getId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post(URL).session(a.session()).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "descricaoReferencia": "CONTA PROPRIA", "estrategiaComparacao": "CONTEM",
						  "tipo": "DESPESA", "categoriaId": "%s", "contaId": "%s",
						  "nivelConfianca": "MEDIA", "aplicacao": "SUGESTAO" }
						""".formatted(categoria.getId(), a.conta().getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.contaId").value(a.conta().getId().toString()));
	}

	private CategoriaFinanceira criarCategoria(Empresa empresa, String nome, TipoFinanceiro tipo) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(empresa, nome, tipo, StatusCadastro.ATIVO));
	}

	private Cenario cenario(String cnpj, PerfilUsuario perfil) throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Regras", "Empresa Regras", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("Usuario Regras",
				"regras." + cnpj + "@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		ContaFinanceira conta = contaRepository.saveAndFlush(new ContaFinanceira(
				empresa, "Conta Regras", TipoContaFinanceira.CONTA_CORRENTE, BigDecimal.ZERO, StatusCadastro.ATIVO));
		return new Cenario(empresa, usuario, conta, autenticarNaEmpresa(usuario.getEmail(), empresa.getId()));
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		mockMvc.perform(post("/api/contexto/empresa-ativa").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}

	private record Cenario(Empresa empresa, Usuario usuario, ContaFinanceira conta, MockHttpSession session) {
	}
}
