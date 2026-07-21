package br.app.criati.admin.web;

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

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
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
class AdminUsuarioControllerTests {

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
	void deveListarUsuariosGlobaisComContadores() throws Exception {
		Usuario usuario = usuarioRepository.saveAndFlush(
				new Usuario("Usuario Global Um", "usuario.global.um@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Global Um", null, "40111111000101", StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO));

		MockHttpSession session = loginSuperAdministrador("superadmin.usuarios.listar@criati.test");

		mockMvc.perform(get("/api/admin/usuarios").session(session).param("busca", "Usuario Global Um"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("usuario.global.um@criati.test"))
				.andExpect(jsonPath("$[0].quantidadeEmpresas").value(1))
				.andExpect(jsonPath("$[0].vinculosAtivos").value(1))
				.andExpect(jsonPath("$[0].senha").doesNotExist());
	}

	@Test
	void deveDetalharUsuarioComVinculosEConvitesSemDadosSensiveis() throws Exception {
		Usuario usuario = usuarioRepository.saveAndFlush(
				new Usuario("Usuario Detalhe Global", "usuario.detalhe.global@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Detalhe Global", null, "40222222000102", StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO));

		MockHttpSession session = loginSuperAdministrador("superadmin.usuarios.detalhe@criati.test");

		MvcResult resultado = mockMvc.perform(get("/api/admin/usuarios/" + usuario.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("usuario.detalhe.global@criati.test"))
				.andExpect(jsonPath("$.vinculos[0].empresaNome").value("Empresa Detalhe Global"))
				.andExpect(jsonPath("$.vinculos[0].perfil").value("GESTOR"))
				.andReturn();

		String corpo = resultado.getResponse().getContentAsString();
		assertThat(corpo).doesNotContain("senha");
		assertThat(corpo).doesNotContain("tokenHash");
		assertThat(corpo).doesNotContain("token");
	}

	@Test
	void anonimoRecebe401ParaUsuariosGlobais() throws Exception {
		mockMvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403ParaUsuariosGlobais() throws Exception {
		MockHttpSession session = login("usuario.comum.usuarios.globais@criati.test", false);
		mockMvc.perform(get("/api/admin/usuarios").session(session)).andExpect(status().isForbidden());
	}

	private MockHttpSession loginSuperAdministrador(String email) throws Exception {
		return login(email, true);
	}

	private MockHttpSession login(String email, boolean superAdministrador) throws Exception {
		Usuario usuario = superAdministrador
				? Usuario.criarSuperAdministrador("Usuario Teste", email, passwordEncoder.encode(SENHA))
				: new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(usuario);

		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "%s",
						  "senha": "%s"
						}
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}
}
