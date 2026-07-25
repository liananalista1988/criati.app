package br.app.criati.pagina.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaComprasTerceirosSegurancaTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void paginasDeComprasTerceirosExigemAutenticacao() throws Exception {
		String detalhe = "/app/financeiro/compras-terceiros/" + UUID.randomUUID();
		for (String rota : new String[] {
				"/app/financeiro/compras-terceiros", "/app/financeiro/compras-terceiros/nova", detalhe
		}) {
			mockMvc.perform(get(rota))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
		}
	}

	@Test
	void empresaSemFinanceiroNaoAcessaPaginasDeComprasTerceiros() throws Exception {
		Empresa empresa = criarEmpresa("77000000000101");
		Usuario usuario = criarUsuario("pagina.ct.sem.financeiro@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.GESTOR);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro/compras-terceiros").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/compras-terceiros/nova").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/compras-terceiros/" + UUID.randomUUID()).session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void listagemRenderizaResumoEEstadoVazio() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("77000000000102",
				"pagina.ct.lista@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(get("/app/financeiro/compras-terceiros").session(session))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("Compras para terceiros")))
				.andExpect(content().string(containsString("Nenhuma compra para terceiro encontrada")))
				.andExpect(content().string(containsString("Total principal")))
				.andExpect(content().string(containsString("Total ressarcido")))
				.andExpect(content().string(containsString("Saldo a receber")))
				.andExpect(content().string(containsString("financeiro-compras-terceiros.js")))
				.andExpect(content().string(containsString("Navegação do Financeiro LeS")))
				.andExpect(content().string(not(containsString("href=\"/app/financeiro/compras-terceiros/nova\""))));
	}

	@Test
	void formularioExigePerfilDeEscritaERenderizaCamposDoBackend() throws Exception {
		MockHttpSession usuario = sessaoComFinanceiro("77000000000103",
				"pagina.ct.usuario@criati.test", PerfilUsuario.USUARIO);
		mockMvc.perform(get("/app/financeiro/compras-terceiros/nova").session(usuario))
				.andExpect(status().isForbidden());

		MockHttpSession gestor = sessaoComFinanceiro("77000000000104",
				"pagina.ct.gestor@criati.test", PerfilUsuario.GESTOR);
		mockMvc.perform(get("/app/financeiro/compras-terceiros/nova").session(gestor))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nova compra para terceiro")))
				.andExpect(content().string(containsString("Terceiro a ressarcir")))
				.andExpect(content().string(containsString("Responsável pela compra")))
				.andExpect(content().string(containsString("Cartão")))
				.andExpect(content().string(containsString("Categoria")));
	}

	@Test
	void detalheRenderizaValoresAReceberDataPrometidaRessarcimentoEErros() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("77000000000105",
				"pagina.ct.detalhe@criati.test", PerfilUsuario.GESTOR);

		mockMvc.perform(get("/app/financeiro/compras-terceiros/" + UUID.randomUUID()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Detalhes da compra")))
				.andExpect(content().string(containsString("Compra não encontrada")))
				.andExpect(content().string(containsString("Valores a receber")))
				.andExpect(content().string(containsString("Data prometida")))
				.andExpect(content().string(containsString("Registrar ressarcimento")))
				.andExpect(content().string(containsString("Integral")))
				.andExpect(content().string(containsString("Parcial")))
				.andExpect(content().string(containsString("Histórico de ressarcimentos")))
				.andExpect(content().string(containsString(
						"não gera receita, despesa ou lançamento financeiro")));
	}

	@Test
	void detalheNaoRenderizaUuidTecnicoDaRotaEDelegaIsolamentoParaApi() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("77000000000106",
				"pagina.ct.tenant@criati.test", PerfilUsuario.USUARIO);
		UUID idDeOutroTenantOuInexistente = UUID.randomUUID();

		mockMvc.perform(get("/app/financeiro/compras-terceiros/" + idDeOutroTenantOuInexistente).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString(idDeOutroTenantOuInexistente.toString()))))
				.andExpect(content().string(containsString("indisponível para a empresa atual")));
	}

	private MockHttpSession sessaoComFinanceiro(String cnpj, String email, PerfilUsuario perfil) throws Exception {
		Empresa empresa = criarEmpresa(cnpj);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario(email);
		criarVinculo(usuario, empresa, perfil);
		return autenticarNaEmpresa(email, empresa.getId());
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(
				new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(
				new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		return usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
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
