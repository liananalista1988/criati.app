package br.app.criati.acesso.web;

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

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.service.VincularUsuarioEmpresaService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;

@WebMvcTest(UsuarioEmpresaController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioEmpresaControllerTests {

	private static final UUID USUARIO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID EMPRESA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final String REQUEST_VALIDO = """
			{
			  "usuarioId": "11111111-1111-1111-1111-111111111111",
			  "empresaId": "22222222-2222-2222-2222-222222222222",
			  "perfil": "ADMINISTRADOR"
			}
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VincularUsuarioEmpresaService vincularUsuarioEmpresaService;

	@Test
	void deveCriarVinculoERetornar201() throws Exception {
		UUID vinculoId = UUID.randomUUID();
		when(vincularUsuarioEmpresaService.executar(
				USUARIO_ID, EMPRESA_ID, PerfilUsuario.ADMINISTRADOR))
				.thenReturn(criarVinculo(vinculoId));

		mockMvc.perform(post("/api/usuarios-empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(vinculoId.toString()))
				.andExpect(jsonPath("$.usuarioId").value(USUARIO_ID.toString()))
				.andExpect(jsonPath("$.empresaId").value(EMPRESA_ID.toString()))
				.andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"))
				.andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void deveRetornar400ParaPerfilNulo() throws Exception {
		mockMvc.perform(post("/api/usuarios-empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "usuarioId": "11111111-1111-1111-1111-111111111111",
						  "empresaId": "22222222-2222-2222-2222-222222222222",
						  "perfil": null
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.perfil").value("Perfil e obrigatorio"));
	}

	@Test
	void deveRetornar404ParaUsuarioInexistente() throws Exception {
		when(vincularUsuarioEmpresaService.executar(
				USUARIO_ID, EMPRESA_ID, PerfilUsuario.ADMINISTRADOR))
				.thenThrow(new UsuarioNaoEncontradoException());

		mockMvc.perform(post("/api/usuarios-empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Usuario nao encontrado"));
	}

	@Test
	void deveRetornar404ParaEmpresaInexistente() throws Exception {
		when(vincularUsuarioEmpresaService.executar(
				USUARIO_ID, EMPRESA_ID, PerfilUsuario.ADMINISTRADOR))
				.thenThrow(new EmpresaNaoEncontradaException());

		mockMvc.perform(post("/api/usuarios-empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Empresa nao encontrada"));
	}

	@Test
	void deveRetornar409ParaVinculoDuplicado() throws Exception {
		when(vincularUsuarioEmpresaService.executar(
				USUARIO_ID, EMPRESA_ID, PerfilUsuario.ADMINISTRADOR))
				.thenThrow(new UsuarioEmpresaJaVinculadoException());

		mockMvc.perform(post("/api/usuarios-empresas")
				.contentType(MediaType.APPLICATION_JSON)
				.content(REQUEST_VALIDO))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Usuario ja vinculado a empresa"));
	}

	private UsuarioEmpresa criarVinculo(UUID vinculoId) {
		Usuario usuario = new Usuario(
				"Usuario de Teste",
				"usuario@criati.test",
				"hash-da-senha",
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(usuario, "id", USUARIO_ID);

		Empresa empresa = new Empresa(
				"Empresa de Teste Ltda",
				"Empresa de Teste",
				"12345678000190",
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(empresa, "id", EMPRESA_ID);

		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				usuario,
				empresa,
				PerfilUsuario.ADMINISTRADOR,
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(vinculo, "id", vinculoId);
		return vinculo;
	}
}
