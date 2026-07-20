package br.app.criati.empresa.web;

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

import br.app.criati.empresa.service.CadastrarEmpresaService;

@WebMvcTest(EmpresaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmpresaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CadastrarEmpresaService cadastrarEmpresaService;

	@Test
	void deveBloquearCadastroDeEmpresaParaQualquerUsuario() throws Exception {
		mockMvc.perform(post("/api/empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Criati Tecnologia Ltda",
						  "nomeFantasia": "Criati",
						  "cnpj": "12.345.678/0001-90"
						}
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		verifyNoInteractions(cadastrarEmpresaService);
	}
}
