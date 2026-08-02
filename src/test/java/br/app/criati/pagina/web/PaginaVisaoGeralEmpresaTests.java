package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Cobre CRIATI-UX-002 (regra "Visao geral" por quantidade de empresas):
 * usuario com exatamente uma empresa vinculada pula o dashboard e vai direto
 * para /app/aplicacoes; zero (ex.: Superadministrador) ou duas ou mais
 * continuam vendo o dashboard/seletor de empresa. A regra e validada no
 * backend (PaginaController), nao apenas ocultada no menu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaVisaoGeralEmpresaTests {

	private static final String SENHA = "senha-correta-123456";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Test
	void usuarioComUmaUnicaEmpresaERedirecionadoDeDashboardParaAplicacoes() throws Exception {
		String email = "visaogeral.uma@criati.test";
		criarUsuarioComEmpresas(email, 1);
		MockHttpSession sessao = autenticar(email);

		mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void usuarioComUmaUnicaEmpresaNaoVeLinkDeDashboardNaSidebar() throws Exception {
		String email = "visaogeral.uma.sidebar@criati.test";
		criarUsuarioComEmpresas(email, 1);
		MockHttpSession sessao = autenticar(email);

		String corpo = mockMvc.perform(get("/app/aplicacoes").session(sessao))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-sidebar")
				.doesNotContain("href=\"/app/dashboard\"");
	}

	@Test
	void usuarioComZeroEmpresasContinuaVendoODashboard() throws Exception {
		String email = "visaogeral.zero@criati.test";
		Usuario usuario = criarUsuarioAtivo(email);
		usuarioRepository.saveAndFlush(usuario);
		MockHttpSession sessao = autenticar(email);

		String corpo = mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-dashboard-conteudo")
				.contains("href=\"/app/dashboard\"");
	}

	@Test
	void usuarioComDuasEmpresasContinuaVendoODashboardESeletor() throws Exception {
		String email = "visaogeral.duas@criati.test";
		criarUsuarioComEmpresas(email, 2);
		MockHttpSession sessao = autenticar(email);

		String corpo = mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("criati-dashboard-conteudo")
				.contains("href=\"/app/dashboard\"");
	}

	@Test
	void acessarDashboardDiretamenteComUmaEmpresaTambemRedirecionaMesmoSemPassarPeloMenu() throws Exception {
		// Reforca que a regra e validada no backend (nao so ocultada no menu):
		// mesmo digitando a URL diretamente, o resultado e o mesmo redirecionamento.
		String email = "visaogeral.url-direta@criati.test";
		criarUsuarioComEmpresas(email, 1);
		MockHttpSession sessao = autenticar(email);

		mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void contagemDeEmpresasDeUmUsuarioNaoEInfluenciadaPorVinculosDeOutroUsuario() throws Exception {
		// Isolamento: usuario A com uma empresa e usuario B com duas empresas
		// (em empresas totalmente diferentes) nao podem interferir na contagem
		// um do outro.
		String emailA = "visaogeral.isolamento.a@criati.test";
		String emailB = "visaogeral.isolamento.b@criati.test";
		criarUsuarioComEmpresas(emailA, 1);
		criarUsuarioComEmpresas(emailB, 2);

		mockMvc.perform(get("/app/dashboard").session(autenticar(emailA)))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/dashboard").session(autenticar(emailB)))
				.andExpect(status().isOk());
	}

	private void criarUsuarioComEmpresas(String email, int quantidadeEmpresas) throws Exception {
		Usuario usuario = criarUsuarioAtivo(email);
		Empresa primeiraEmpresa = null;
		for (int i = 0; i < quantidadeEmpresas; i++) {
			Empresa empresa = empresaRepository.saveAndFlush(
					new Empresa("Empresa Visao Geral " + i + " Ltda", "Empresa VG " + i, cnpjUnico(), StatusCadastro.ATIVO));
			if (primeiraEmpresa == null) {
				primeiraEmpresa = empresa;
			}
			usuarioEmpresaRepository.saveAndFlush(
					new UsuarioEmpresa(usuario, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		}
		if (primeiraEmpresa != null) {
			MockHttpSession sessao = autenticar(email);
			mockMvc.perform(post("/api/contexto/empresa-ativa")
					.session(sessao)
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{ \"empresaId\": \"" + primeiraEmpresa.getId() + "\" }"))
					.andExpect(status().isOk());
		}
	}

	private String cnpjUnico() {
		return String.valueOf(Math.abs(System.nanoTime())).substring(0, 14);
	}

	private MockHttpSession autenticar(String email) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "senha": "%s"
						}
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) loginResult.getRequest().getSession(false);
	}

	private Usuario criarUsuarioAtivo(String email) {
		Usuario usuario = new Usuario("Usuario Visao Geral", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}
}
