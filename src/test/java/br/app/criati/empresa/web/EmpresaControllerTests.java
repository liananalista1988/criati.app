package br.app.criati.empresa.web;

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

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.service.CadastrarEmpresaService;
import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;

@WebMvcTest(EmpresaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmpresaControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CadastrarEmpresaService cadastrarEmpresaService;

	@Test
	void deveCadastrarEmpresaERetornar201() throws Exception {
		UUID empresaId = UUID.randomUUID();
		Empresa empresa = new Empresa(
				"Criati Tecnologia Ltda",
				"Criati",
				"12345678000190",
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(empresa, "id", empresaId);
		when(cadastrarEmpresaService.executar(
				"Criati Tecnologia Ltda",
				"Criati",
				"12.345.678/0001-90"))
				.thenReturn(empresa);

		mockMvc.perform(post("/api/empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Criati Tecnologia Ltda",
						  "nomeFantasia": "Criati",
						  "cnpj": "12.345.678/0001-90"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(empresaId.toString()))
				.andExpect(jsonPath("$.nome").value("Criati Tecnologia Ltda"))
				.andExpect(jsonPath("$.nomeFantasia").value("Criati"))
				.andExpect(jsonPath("$.cnpj").value("12345678000190"))
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void deveRetornar400ParaEntradaInvalida() throws Exception {
		mockMvc.perform(post("/api/empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "",
						  "cnpj": ""
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.path").value("/api/empresas"))
				.andExpect(jsonPath("$.fieldErrors.nome").value("Nome e obrigatorio"))
				.andExpect(jsonPath("$.fieldErrors.cnpj").value("CNPJ e obrigatorio"));
	}

	@Test
	void deveRetornar409ParaCnpjDuplicado() throws Exception {
		when(cadastrarEmpresaService.executar("Empresa Duplicada", null, "12345678000190"))
				.thenThrow(new CnpjJaCadastradoException());

		mockMvc.perform(post("/api/empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Empresa Duplicada",
						  "cnpj": "12345678000190"
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("CNPJ ja cadastrado"))
				.andExpect(jsonPath("$.path").value("/api/empresas"));
	}
}
