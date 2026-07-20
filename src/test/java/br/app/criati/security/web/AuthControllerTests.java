package br.app.criati.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveFazerLoginComCredenciaisValidasECriarSessao() throws Exception {
		criarUsuarioAtivo("login.valido@criati.test", "senha-correta");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "login.valido@criati.test",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("login.valido@criati.test"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void deveRetornar401GenericoParaEmailInexistente() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "nao.existe@criati.test",
						  "senha": "qualquer-coisa"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("E-mail ou senha invalidos"));
	}

	@Test
	void deveRetornarMesmaRespostaGenericaParaSenhaErrada() throws Exception {
		criarUsuarioAtivo("senha.errada@criati.test", "senha-correta");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "senha.errada@criati.test",
						  "senha": "senha-incorreta"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("E-mail ou senha invalidos"));
	}

	@Test
	void naoDeveAutenticarUsuarioInativo() throws Exception {
		criarUsuario("inativo@criati.test", "senha-correta", StatusCadastro.INATIVO);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "inativo@criati.test",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("E-mail ou senha invalidos"));
	}

	@Test
	void deveRetornar401ParaMeSemAutenticacao() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveAcessarMeComSessaoValidaEInvalidarNoLogout() throws Exception {
		criarUsuarioAtivo("sessao.valida@criati.test", "senha-correta");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "sessao.valida@criati.test",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(session).isNotNull();

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("sessao.valida@criati.test"));

		mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveExigirCsrfParaLogoutMasNaoParaLogin() throws Exception {
		criarUsuarioAtivo("csrf@criati.test", "senha-correta");

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "csrf@criati.test",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveTrocarIdDaSessaoAposLoginParaEvitarSessionFixation() throws Exception {
		criarUsuarioAtivo("fixacao@criati.test", "senha-correta");

		MockHttpSession sessaoAnonima = new MockHttpSession();
		String idAnterior = sessaoAnonima.getId();

		mockMvc.perform(post("/api/auth/login")
				.session(sessaoAnonima)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "fixacao@criati.test",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk());

		assertThat(sessaoAnonima.getId()).isNotEqualTo(idAnterior);

		mockMvc.perform(get("/api/auth/me").session(sessaoAnonima))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("fixacao@criati.test"));
	}

	@Test
	void naoDeveCriarSessaoQuandoLoginFalha() throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "nao.existe@criati.test",
						  "senha": "qualquer-coisa"
						}
						"""))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void deveFazerLoginComEmailContendoEspacos() throws Exception {
		criarUsuarioAtivo("espacos@criati.test", "senha-correta");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "  espacos@criati.test  ",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("espacos@criati.test"));
	}

	@Test
	void deveFazerLoginComEmailEmCaixaDiferente() throws Exception {
		criarUsuarioAtivo("caixa@criati.test", "senha-correta");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "CAIXA@Criati.TEST",
						  "senha": "senha-correta"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("caixa@criati.test"));
	}

	private Usuario criarUsuarioAtivo(String email, String senhaBruta) {
		return criarUsuario(email, senhaBruta, StatusCadastro.ATIVO);
	}

	private Usuario criarUsuario(String email, String senhaBruta, StatusCadastro status) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(senhaBruta), status);
		return usuarioRepository.saveAndFlush(usuario);
	}
}
