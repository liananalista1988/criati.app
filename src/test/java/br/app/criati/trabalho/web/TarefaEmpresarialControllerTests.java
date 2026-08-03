package br.app.criati.trabalho.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import br.app.criati.aplicacao.service.AplicacaoService;
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
class TarefaEmpresarialControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/trabalho/tarefas";
	private static final String URL_PROCESSOS = "/api/contexto/trabalho/processos";

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

	@Autowired
	private AplicacaoService aplicacaoService;

	@Test
	void administradorCriaTarefaSemProcesso() throws Exception {
		Empresa empresa = criarEmpresa("32111111000101");
		Usuario admin = criarUsuario("tarefa.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Tarefa avulsa" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.situacao").value("PENDENTE"))
				.andExpect(jsonPath("$.processoId").doesNotExist());
	}

	@Test
	void criaTarefaVinculadaAProcesso() throws Exception {
		Empresa empresa = criarEmpresa("32111111000102");
		Usuario admin = criarUsuario("tarefa.vinculada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String processoId = criarProcessoViaApi(session, "Processo pai");

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "processoId": "%s", "titulo": "Tarefa vinculada" }
						""".formatted(processoId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.processoId").value(processoId))
				.andExpect(jsonPath("$.processoTitulo").value("Processo pai"));
	}

	@Test
	void rejeitaProcessoDeOutraEmpresa() throws Exception {
		Empresa empresaA = criarEmpresa("32111111000103");
		Empresa empresaB = criarEmpresa("32111111000104");
		Usuario adminA = criarUsuario("tarefa.procA@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("tarefa.procB@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());
		String processoB = criarProcessoViaApi(sessaoB, "Processo da empresa B");

		mockMvc.perform(post(URL_BASE).session(sessaoA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "processoId": "%s", "titulo": "Tarefa cruzada" }
						""".formatted(processoB)))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejeitaResponsavelDeOutraEmpresa() throws Exception {
		Empresa empresaA = criarEmpresa("32111111000105");
		Empresa empresaB = criarEmpresa("32111111000106");
		Usuario adminA = criarUsuario("tarefa.respA@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario usuarioB = criarUsuario("tarefa.respB@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(usuarioB, empresaB, PerfilUsuario.USUARIO);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());

		mockMvc.perform(post(URL_BASE).session(sessaoA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Tarefa com responsavel invalido", "responsavelId": "%s" }
						""".formatted(vinculoB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void responsavelPodeAtualizarAndamentoDaPropriaTarefa() throws Exception {
		Empresa empresa = criarEmpresa("32111111000107");
		Usuario admin = criarUsuario("tarefa.andamento.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario colaborador = criarUsuario("tarefa.andamento.colaborador@criati.test");
		UsuarioEmpresa vinculoColaborador = criarVinculo(colaborador, empresa, PerfilUsuario.USUARIO);
		MockHttpSession sessaoAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String idTarefa = criarTarefaComResponsavelViaApi(sessaoAdmin, "Tarefa atribuida", vinculoColaborador.getId());
		MockHttpSession sessaoColaborador = autenticarNaEmpresa(colaborador.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + idTarefa + "/iniciar").session(sessaoColaborador).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"));

		mockMvc.perform(post(URL_BASE + "/" + idTarefa + "/concluir").session(sessaoColaborador).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("CONCLUIDA"));
	}

	@Test
	void usuarioNaoPodeAlterarAndamentoDeTarefaDeOutroResponsavel() throws Exception {
		Empresa empresa = criarEmpresa("32111111000108");
		Usuario admin = criarUsuario("tarefa.outro.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario responsavel = criarUsuario("tarefa.outro.responsavel@criati.test");
		UsuarioEmpresa vinculoResponsavel = criarVinculo(responsavel, empresa, PerfilUsuario.USUARIO);
		Usuario terceiro = criarUsuario("tarefa.outro.terceiro@criati.test");
		criarVinculo(terceiro, empresa, PerfilUsuario.USUARIO);
		MockHttpSession sessaoAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String idTarefa = criarTarefaComResponsavelViaApi(sessaoAdmin, "Tarefa de outro", vinculoResponsavel.getId());
		MockHttpSession sessaoTerceiro = autenticarNaEmpresa(terceiro.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + idTarefa + "/iniciar").session(sessaoTerceiro).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumNaoPodeEditarNemInativarTarefa() throws Exception {
		Empresa empresa = criarEmpresa("32111111000109");
		Usuario admin = criarUsuario("tarefa.editar.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario usuario = criarUsuario("tarefa.editar.usuario@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO);
		MockHttpSession sessaoAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarTarefaViaApi(sessaoAdmin, "Tarefa protegida");
		MockHttpSession sessaoUsuario = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE + "/" + id + "/inativar").session(sessaoUsuario).with(csrf()))
				.andExpect(status().isForbidden());
	}

	@Test
	void empresaANaoConsultaTarefaDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresa("32111111000110");
		Empresa empresaB = criarEmpresa("32111111000111");
		Usuario adminA = criarUsuario("tarefa.isolA@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("tarefa.isolB@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());
		String idB = criarTarefaViaApi(sessaoB, "Tarefa da empresa B");

		mockMvc.perform(get(URL_BASE + "/" + idB).session(sessaoA)).andExpect(status().isNotFound());
	}

	@Test
	void reabrirLimpaDataDeConclusao() throws Exception {
		Empresa empresa = criarEmpresa("32111111000112");
		Usuario admin = criarUsuario("tarefa.reabrir@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarTarefaViaApi(session, "Tarefa a reabrir");
		mockMvc.perform(post(URL_BASE + "/" + id + "/concluir").session(session).with(csrf())).andExpect(status().isOk());

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"))
				.andExpect(jsonPath("$.dataConclusao").doesNotExist());
	}

	@Test
	void filtraMinhasTarefasEAtrasadas() throws Exception {
		Empresa empresa = criarEmpresa("32111111000113");
		Usuario admin = criarUsuario("tarefa.minhas.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario colaborador = criarUsuario("tarefa.minhas.colaborador@criati.test");
		UsuarioEmpresa vinculoColaborador = criarVinculo(colaborador, empresa, PerfilUsuario.USUARIO);
		MockHttpSession sessaoAdmin = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarTarefaComResponsavelViaApi(sessaoAdmin, "Tarefa do colaborador", vinculoColaborador.getId());
		criarTarefaViaApi(sessaoAdmin, "Tarefa sem responsavel");
		criarTarefaComPrazoViaApi(sessaoAdmin, "Tarefa atrasada", LocalDate.now().minusDays(2));

		MockHttpSession sessaoColaborador = autenticarNaEmpresa(colaborador.getEmail(), empresa.getId());
		mockMvc.perform(get(URL_BASE).session(sessaoColaborador).param("minhasTarefas", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.itens[0].titulo").value("Tarefa do colaborador"));

		mockMvc.perform(get(URL_BASE).session(sessaoAdmin).param("atrasada", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.itens[0].titulo").value("Tarefa atrasada"));
	}

	@Test
	void tarefaInexistenteRetorna404() throws Exception {
		Empresa empresa = criarEmpresa("32111111000114");
		Usuario admin = criarUsuario("tarefa.404@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_BASE + "/" + UUID.randomUUID()).session(session)).andExpect(status().isNotFound());
	}

	@Test
	void apiExigeAutenticacao() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
	}

	private String criarProcessoViaApi(MockHttpSession session, String titulo) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_PROCESSOS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarTarefaViaApi(MockHttpSession session, String titulo) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarTarefaComResponsavelViaApi(MockHttpSession session, String titulo, UUID responsavelId)
			throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\",\"responsavelId\":\"" + responsavelId + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarTarefaComPrazoViaApi(MockHttpSession session, String titulo, LocalDate prazo) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\",\"prazo\":\"" + prazo + "\"}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Trabalho Ltda", "Empresa Trabalho", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		aplicacaoService.habilitar(empresaId, "TAREFAS_PROCESSOS");
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
