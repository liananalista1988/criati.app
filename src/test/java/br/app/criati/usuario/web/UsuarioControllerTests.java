package br.app.criati.usuario.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.app.criati.usuario.service.CadastrarUsuarioService;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CadastrarUsuarioService cadastrarUsuarioService;

	@Test
	void deveBloquearCadastroDeUsuarioParaQualquerUsuario() throws Exception {
		mockMvc.perform(post("/api/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Lian Nascimento",
						  "email": "lian@criati.app.br",
						  "senha": "hash-da-senha"
						}
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		verifyNoInteractions(cadastrarUsuarioService);
	}
}
