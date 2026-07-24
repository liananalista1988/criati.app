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
class PaginaEmprestimosSegurancaTests {

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
	void paginasDeEmprestimosExigemAutenticacao() throws Exception {
		String detalhe = "/app/financeiro/emprestimos/" + UUID.randomUUID();
		for (String rota : new String[] {
				"/app/financeiro/emprestimos", "/app/financeiro/emprestimos/novo", detalhe
		}) {
			mockMvc.perform(get(rota))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
		}
	}

	@Test
	void empresaSemFinanceiroNaoAcessaPaginasDeEmprestimos() throws Exception {
		Empresa empresa = criarEmpresa("76000000000101");
		Usuario usuario = criarUsuario("pagina.emp.sem.financeiro@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.GESTOR);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro/emprestimos").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/emprestimos/novo").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/emprestimos/" + UUID.randomUUID()).session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void listagemRenderizaResumoEstadosVaziosEVencimentos() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("76000000000102",
				"pagina.emp.lista@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(get("/app/financeiro/emprestimos").session(session))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("Empréstimos concedidos")))
				.andExpect(content().string(containsString("Nenhum empréstimo concedido encontrado")))
				.andExpect(content().string(containsString("Total principal")))
				.andExpect(content().string(containsString("Total recebido")))
				.andExpect(content().string(containsString("Saldo a receber")))
				.andExpect(content().string(containsString("Parcelas pendentes")))
				.andExpect(content().string(containsString("Parcelas vencidas")))
				.andExpect(content().string(containsString("Próximas do vencimento")))
				.andExpect(content().string(containsString("financeiro-emprestimos.js")))
				.andExpect(content().string(containsString("Navegação do Financeiro LeS")))
				.andExpect(content().string(not(containsString("href=\"/app/financeiro/emprestimos/novo\""))));
	}

	@Test
	void formularioExigePerfilDeEscritaERenderizaCamposDoBackend() throws Exception {
		MockHttpSession usuario = sessaoComFinanceiro("76000000000103",
				"pagina.emp.usuario@criati.test", PerfilUsuario.USUARIO);
		mockMvc.perform(get("/app/financeiro/emprestimos/novo").session(usuario))
				.andExpect(status().isForbidden());

		MockHttpSession gestor = sessaoComFinanceiro("76000000000104",
				"pagina.emp.gestor@criati.test", PerfilUsuario.GESTOR);
		mockMvc.perform(get("/app/financeiro/emprestimos/novo").session(gestor))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo empréstimo concedido")))
				.andExpect(content().string(containsString("Devedor")))
				.andExpect(content().string(containsString("Categoria de receita")))
				.andExpect(content().string(containsString("Quantidade de parcelas")))
				.andExpect(content().string(containsString("Configuração de cobrança")))
				.andExpect(content().string(containsString("Percentual de juros")))
				.andExpect(content().string(containsString("Percentual de multa")));
	}

	@Test
	void detalheRenderizaParcelasDataPrometidaRecebimentosEErros() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("76000000000105",
				"pagina.emp.detalhe@criati.test", PerfilUsuario.GESTOR);

		mockMvc.perform(get("/app/financeiro/emprestimos/" + UUID.randomUUID()).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Detalhes do empréstimo")))
				.andExpect(content().string(containsString("Empréstimo não encontrado")))
				.andExpect(content().string(containsString("Parcelas")))
				.andExpect(content().string(containsString("Data prometida")))
				.andExpect(content().string(containsString("Registrar recebimento")))
				.andExpect(content().string(containsString("Integral")))
				.andExpect(content().string(containsString("Parcial")))
				.andExpect(content().string(containsString("Histórico de recebimentos")))
				.andExpect(content().string(containsString("Lançamento")));
	}

	@Test
	void detalheNaoRenderizaUuidTecnicoDaRotaEDelegaIsolamentoParaApi() throws Exception {
		MockHttpSession session = sessaoComFinanceiro("76000000000106",
				"pagina.emp.tenant@criati.test", PerfilUsuario.USUARIO);
		UUID idDeOutroTenantOuInexistente = UUID.randomUUID();

		mockMvc.perform(get("/app/financeiro/emprestimos/" + idDeOutroTenantOuInexistente).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString(idDeOutroTenantOuInexistente.toString()))))
				.andExpect(content().string(not(containsString("ID do empréstimo"))))
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
