package br.app.criati.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ErrorHandlingSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void requisicaoAnonimaAEndpointProtegidoRetorna401Json() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/api/usuarios"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(resultado.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo).doesNotContain("Exception", "at br.app.criati", "\tat ", "stackTrace");
		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void requisicaoAnonimaARotaInexistenteRetornaComportamentoConsistenteSemStackTrace() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/api/rota-que-nao-existe"))
				.andReturn();

		int status = resultado.getResponse().getStatus();
		assertThat(status).isIn(401, 404);
		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo).doesNotContain("Exception", "at br.app.criati", "\tat ", "stackTrace");
	}

	@Test
	void errorNaoDeveCriarSessaoParaRequisicaoAnonimaARotaInexistente() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/api/rota-que-nao-existe"))
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void requisicaoAutenticadaARotaInexistenteNaoRetorna401Indevido() throws Exception {
		criarUsuarioAtivo("erro.autenticado@criati.test", "senha-correta");

		MvcResult loginResult = mockMvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "erro.autenticado@criati.test",
								  "senha": "senha-correta"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
		assertThat(session).isNotNull();

		MvcResult resultado = mockMvc.perform(get("/api/rota-que-nao-existe").session(session))
				.andReturn();

		assertThat(resultado.getResponse().getStatus()).isNotEqualTo(401);
		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo).doesNotContain("Exception", "at br.app.criati", "\tat ", "stackTrace");
	}

	@Test
	void requisicaoAutenticadaAEndpointExistenteContinuaProtegidaPorAutorizacao() throws Exception {
		criarUsuarioAtivo("erro.negocio@criati.test", "senha-correta");

		MvcResult loginResult = mockMvc.perform(
				org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "erro.negocio@criati.test",
								  "senha": "senha-correta"
								}
								"""))
				.andExpect(status().isOk())
				.andReturn();

		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk());
	}

	private Usuario criarUsuarioAtivo(String email, String senhaBruta) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(senhaBruta), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}
}
