package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
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
 * Verifica, via MockMvc (renderizacao real do Thymeleaf, sem executar
 * JavaScript no navegador), que as paginas de Usuarios e Convites contem os
 * elementos principais esperados: tabela/filtros/botoes/modais/scripts.
 * Tambem audita os arquivos JS estaticos correspondentes para confirmar
 * ausencia de armazenamento de token em localStorage/sessionStorage e
 * ausencia de token fixo/hardcoded.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaUsuariosConvitesConteudoTests {

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
	private PasswordEncoder passwordEncoder;

	@Test
	void paginaUsuariosContemElementosPrincipais() throws Exception {
		String corpo = carregarPaginaComoAdministrador("/app/usuarios", "conteudo.usuarios@criati.test", "11111111000411");

		assertThat(corpo)
				.contains("criati-usuarios-tbody")
				.contains("criati-usuarios-cards")
				.contains("criati-usuarios-busca")
				.contains("criati-usuarios-filtro-perfil")
				.contains("criati-usuarios-filtro-status")
				.contains("Convidar usuario")
				.contains("criati-perfil-modal")
				.contains("criati-perfil-select")
				.contains("criati-confirmar-modal")
				.contains("criati-usuario-detalhe-modal")
				.contains("aria-modal=\"true\"")
				.contains("aria-labelledby")
				.contains("/js/criati-usuarios.js")
				.contains("/css/criati-acessos.css");
	}

	@Test
	void paginaConvitesContemElementosPrincipais() throws Exception {
		String corpo = carregarPaginaComoAdministrador("/app/convites", "conteudo.convites@criati.test", "22222222000412");

		assertThat(corpo)
				.contains("criati-convites-tbody")
				.contains("criati-convites-cards")
				.contains("criati-convites-busca")
				.contains("criati-convites-filtro-perfil")
				.contains("criati-convites-filtro-status")
				.contains("Novo convite")
				.contains("criati-convite-modal")
				.contains("criati-convite-sucesso-modal")
				.contains("criati-convite-sucesso-copiar")
				.contains("criati-confirmar-modal")
				.contains("aria-modal=\"true\"")
				.contains("aria-labelledby")
				.contains("/js/criati-convites.js")
				.contains("/css/criati-acessos.css");
	}

	@Test
	void paginasNaoContemTokenFixoNemLinkExternoHardcoded() throws Exception {
		String corpoConvites = carregarPaginaComoAdministrador(
				"/app/convites", "conteudo.convites.token@criati.test", "33333333000413");

		assertThat(corpoConvites)
				.doesNotContain("http://exemplo")
				.doesNotContain("https://exemplo")
				.doesNotContainPattern("value=\"[A-Za-z0-9_-]{20,}\"");
	}

	@Test
	void jsDeUsuariosNaoEscreveEmLocalStorageOuSessionStorage() throws Exception {
		String conteudo = lerRecursoEstatico("static/js/criati-usuarios.js");
		assertThat(conteudo)
				.doesNotContain("localStorage.setItem")
				.doesNotContain("sessionStorage.setItem");
	}

	@Test
	void jsDeConvitesNaoEscreveEmLocalStorageOuSessionStorageNemLogaOToken() throws Exception {
		String conteudo = lerRecursoEstatico("static/js/criati-convites.js");
		assertThat(conteudo)
				.doesNotContain("localStorage.setItem")
				.doesNotContain("sessionStorage.setItem")
				.doesNotContain("console.log(tokenAtual")
				.doesNotContain("console.log(token");
	}

	private String lerRecursoEstatico(String caminhoClasspath) throws Exception {
		ClassPathResource recurso = new ClassPathResource(caminhoClasspath);
		return Files.readString(recurso.getFile().toPath(), StandardCharsets.UTF_8);
	}

	private String carregarPaginaComoAdministrador(String rota, String email, String cnpj) throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Conteudo Ltda", "Empresa Conteudo", cnpj, StatusCadastro.ATIVO));
		Usuario admin = new Usuario("Administrador Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(admin);
		UsuarioEmpresa vinculo = new UsuarioEmpresa(admin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);

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
						""".formatted(empresa.getId())))
				.andExpect(status().isOk());

		return mockMvc.perform(get(rota).session(session))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}
}
