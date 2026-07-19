package br.app.criati.usuario.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.service.CadastrarUsuarioService;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTests {

	private static final String REQUEST_VALIDO = """
			{
			  "nome": "Lian Nascimento",
			  "email": "lian@criati.app.br",
			  "senhaHash": "hash-da-senha"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CadastrarUsuarioService cadastrarUsuarioService;

	@Test
	void deveCadastrarUsuarioERetornar201() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		when(cadastrarUsuarioService.executar(
				"Lian Nascimento", "lian@criati.app.br", "hash-da-senha"))
				.thenReturn(criarUsuario(usuarioId));

		mockMvc.perform(post("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(usuarioId.toString()))
				.andExpect(jsonPath("$.nome").value("Lian Nascimento"))
				.andExpect(jsonPath("$.email").value("lian@criati.app.br"))
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void naoDeveExporSenhaOuHashNaResposta() throws Exception {
		when(cadastrarUsuarioService.executar(
				"Lian Nascimento", "lian@criati.app.br", "hash-da-senha"))
				.thenReturn(criarUsuario(UUID.randomUUID()));

		mockMvc.perform(post("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void deveRetornar400ParaEmailInvalido() throws Exception {
		mockMvc.perform(post("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Lian Nascimento",
						  "email": "email-invalido",
						  "senhaHash": "hash-da-senha"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.email").value("E-mail deve ser valido"));
	}

	@Test
	void deveRetornar409ParaEmailDuplicado() throws Exception {
		when(cadastrarUsuarioService.executar(
				"Lian Nascimento", "lian@criati.app.br", "hash-da-senha"))
				.thenThrow(new EmailJaCadastradoException());

		mockMvc.perform(post("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("E-mail ja cadastrado"))
				.andExpect(jsonPath("$.path").value("/api/usuarios"));
	}

	private Usuario criarUsuario(UUID usuarioId) {
		Usuario usuario = new Usuario(
				"Lian Nascimento",
				"lian@criati.app.br",
				"hash-da-senha",
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(usuario, "id", usuarioId);
		return usuario;
	}
}
