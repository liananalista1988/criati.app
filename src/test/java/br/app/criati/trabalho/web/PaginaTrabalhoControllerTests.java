package br.app.criati.trabalho.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
class PaginaTrabalhoControllerTests {

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
	void paginasDeTrabalhoExigemAutenticacao() throws Exception {
		String processoDetalhe = "/app/trabalho/processos/" + UUID.randomUUID();
		String tarefaDetalhe = "/app/trabalho/tarefas/" + UUID.randomUUID();
		for (String rota : new String[] {
				"/app/trabalho/processos", "/app/trabalho/processos/novo", processoDetalhe,
				"/app/trabalho/tarefas", "/app/trabalho/tarefas/nova", tarefaDetalhe
		}) {
			mockMvc.perform(get(rota))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string("Location", endsWith("/login")));
		}
	}

	@Test
	void usuarioComumAcessaListagensEDetalhesMasNaoFormularios() throws Exception {
		MockHttpSession session = sessaoTrabalho("41000000000101", "trabalho.usuario@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(get("/app/trabalho/processos").session(session)).andExpect(status().isOk());
		mockMvc.perform(get("/app/trabalho/processos/" + UUID.randomUUID()).session(session)).andExpect(status().isOk());
		mockMvc.perform(get("/app/trabalho/tarefas").session(session)).andExpect(status().isOk());
		mockMvc.perform(get("/app/trabalho/tarefas/" + UUID.randomUUID()).session(session)).andExpect(status().isOk());

		mockMvc.perform(get("/app/trabalho/processos/novo").session(session)).andExpect(status().isForbidden());
		mockMvc.perform(get("/app/trabalho/processos/" + UUID.randomUUID() + "/editar").session(session))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/app/trabalho/tarefas/nova").session(session)).andExpect(status().isForbidden());
		mockMvc.perform(get("/app/trabalho/tarefas/" + UUID.randomUUID() + "/editar").session(session))
				.andExpect(status().isForbidden());
	}

	@Test
	void administradorAcessaFormularios() throws Exception {
		MockHttpSession session = sessaoTrabalho("41000000000102", "trabalho.admin@criati.test", PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(get("/app/trabalho/processos/novo").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Novo processo")))
				.andExpect(content().string(containsString("trabalho-processo-form.js")));

		mockMvc.perform(get("/app/trabalho/tarefas/nova").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Nova tarefa")))
				.andExpect(content().string(containsString("trabalho-tarefa-form.js")));
	}

	@Test
	void listagemDeProcessosRenderizaEstruturaEBotaoNovoConformePermissao() throws Exception {
		MockHttpSession usuario = sessaoTrabalho("41000000000103", "trabalho.lista.usuario@criati.test", PerfilUsuario.USUARIO);
		mockMvc.perform(get("/app/trabalho/processos").session(usuario))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(containsString("Processos")))
				.andExpect(content().string(containsString("trabalho-indicadores")))
				.andExpect(content().string(containsString("trabalho-processos.js")))
				.andExpect(content().string(not(containsString("href=\"/app/trabalho/processos/novo\""))));

		MockHttpSession admin = sessaoTrabalho("41000000000104", "trabalho.lista.admin@criati.test", PerfilUsuario.ADMINISTRADOR);
		mockMvc.perform(get("/app/trabalho/processos").session(admin))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("href=\"/app/trabalho/processos/novo\"")));
	}

	@Test
	void listagemDeTarefasRenderizaFiltroMinhasTarefas() throws Exception {
		MockHttpSession session = sessaoTrabalho("41000000000105", "trabalho.tarefas.lista@criati.test", PerfilUsuario.USUARIO);
		mockMvc.perform(get("/app/trabalho/tarefas").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Minhas tarefas")))
				.andExpect(content().string(containsString("trabalho-tarefas.js")));
	}

	@Test
	void detalheDeProcessoEDeTarefaRenderizamShellSemVazarDadosDeOutraEmpresa() throws Exception {
		MockHttpSession session = sessaoTrabalho("41000000000106", "trabalho.detalhe.tenant@criati.test", PerfilUsuario.USUARIO);
		UUID idInexistenteOuDeOutraEmpresa = UUID.randomUUID();

		mockMvc.perform(get("/app/trabalho/processos/" + idInexistenteOuDeOutraEmpresa).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("trabalho-processo-detalhe.js")));

		mockMvc.perform(get("/app/trabalho/tarefas/" + idInexistenteOuDeOutraEmpresa).session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("trabalho-tarefa-detalhe.js")));
	}

	@Test
	void paginasNaoReferenciamModuloFinanceiro() throws Exception {
		MockHttpSession session = sessaoTrabalho("41000000000107", "trabalho.independente@criati.test", PerfilUsuario.USUARIO);
		mockMvc.perform(get("/app/trabalho/processos").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("financeiro-"))));
		mockMvc.perform(get("/app/trabalho/tarefas").session(session))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("financeiro-"))));
	}

	private MockHttpSession sessaoTrabalho(String cnpj, String email, PerfilUsuario perfil) throws Exception {
		Empresa empresa = criarEmpresa(cnpj);
		Usuario usuario = criarUsuario(email);
		criarVinculo(usuario, empresa, perfil);
		return autenticarNaEmpresa(email, empresa.getId());
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(
				new Empresa("Empresa Trabalho Ltda", "Empresa Trabalho", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(
				new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		return usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
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
						""".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}
}
