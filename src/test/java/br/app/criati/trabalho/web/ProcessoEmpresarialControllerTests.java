package br.app.criati.trabalho.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class ProcessoEmpresarialControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/trabalho/processos";
	private static final String URL_TAREFAS = "/api/contexto/trabalho/tarefas";

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
	void administradorCriaProcesso() throws Exception {
		Empresa empresa = criarEmpresa("31111111000101");
		Usuario admin = criarUsuario("processo.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Implantacao do cliente X", "descricao": "Etapas iniciais" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.situacao").value("ABERTO"))
				.andExpect(jsonPath("$.prioridade").value("MEDIA"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.quantidadeTarefas").value(0));
	}

	@Test
	void usuarioComumNaoPodeCriarProcessoMasPodeListarEConsultar() throws Exception {
		Empresa empresa = criarEmpresa("31111111000102");
		Usuario usuario = criarUsuario("processo.usuario@criati.test");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Processo restrito" }
						"""))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(URL_BASE).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens").isArray());
	}

	@Test
	void apiExigeAutenticacaoECsrf() throws Exception {
		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());

		Empresa empresa = criarEmpresa("31111111000103");
		Usuario admin = criarUsuario("processo.seguranca@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Sem csrf" }
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void naoPermiteResponsavelDeOutraEmpresa() throws Exception {
		Empresa empresaA = criarEmpresa("31111111000104");
		Empresa empresaB = criarEmpresa("31111111000105");
		Usuario admin = criarUsuario("processo.respA@criati.test");
		criarVinculo(admin, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario usuarioB = criarUsuario("processo.respB@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(usuarioB, empresaB, PerfilUsuario.USUARIO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresaA.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Processo com responsavel invalido", "responsavelId": "%s" }
						""".formatted(vinculoB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void atribuiResponsavelDaMesmaEmpresa() throws Exception {
		Empresa empresa = criarEmpresa("31111111000106");
		Usuario admin = criarUsuario("processo.resp.ok@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		Usuario colaborador = criarUsuario("processo.colaborador@criati.test");
		UsuarioEmpresa vinculoColaborador = criarVinculo(colaborador, empresa, PerfilUsuario.USUARIO);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Processo com responsavel", null);

		mockMvc.perform(post(URL_BASE + "/" + id + "/responsavel").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "responsavelId": "%s" }
						""".formatted(vinculoColaborador.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.responsavelId").value(vinculoColaborador.getId().toString()))
				.andExpect(jsonPath("$.responsavelNome").value("Usuario Teste"));
	}

	@Test
	void administradorEditaProcesso() throws Exception {
		Empresa empresa = criarEmpresa("31111111000120");
		Usuario admin = criarUsuario("processo.editar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Titulo original", null);

		mockMvc.perform(put(URL_BASE + "/" + id).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "titulo": "Titulo revisado", "prioridade": "ALTA" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Titulo revisado"))
				.andExpect(jsonPath("$.prioridade").value("ALTA"));
	}

	@Test
	void empresaANaoConsultaProcessoDaEmpresaB() throws Exception {
		Empresa empresaA = criarEmpresa("31111111000107");
		Empresa empresaB = criarEmpresa("31111111000108");
		Usuario adminA = criarUsuario("processo.isolA@criati.test");
		criarVinculo(adminA, empresaA, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("processo.isolB@criati.test");
		criarVinculo(adminB, empresaB, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessaoA = autenticarNaEmpresa(adminA.getEmail(), empresaA.getId());
		MockHttpSession sessaoB = autenticarNaEmpresa(adminB.getEmail(), empresaB.getId());
		String idB = criarProcessoViaApi(sessaoB, "Processo da empresa B", null);

		mockMvc.perform(get(URL_BASE + "/" + idB).session(sessaoA)).andExpect(status().isNotFound());
	}

	@Test
	void processoInexistenteRetorna404() throws Exception {
		Empresa empresa = criarEmpresa("31111111000109");
		Usuario admin = criarUsuario("processo.404@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(get(URL_BASE + "/" + UUID.randomUUID()).session(session)).andExpect(status().isNotFound());
	}

	@Test
	void fluxoDeSituacaoIniciarConcluirReabrirCancelar() throws Exception {
		Empresa empresa = criarEmpresa("31111111000110");
		Usuario admin = criarUsuario("processo.fluxo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Processo com fluxo completo", null);

		mockMvc.perform(post(URL_BASE + "/" + id + "/iniciar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/concluir").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("CONCLUIDO"))
				.andExpect(jsonPath("$.dataConclusao").isNotEmpty());

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("EM_ANDAMENTO"))
				.andExpect(jsonPath("$.dataConclusao").doesNotExist());

		mockMvc.perform(post(URL_BASE + "/" + id + "/cancelar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("CANCELADO"));
	}

	@Test
	void transicaoInvalidaEhRejeitada() throws Exception {
		Empresa empresa = criarEmpresa("31111111000111");
		Usuario admin = criarUsuario("processo.transicao.invalida@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Processo aberto", null);

		mockMvc.perform(post(URL_BASE + "/" + id + "/reabrir").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void naoConcluiProcessoComTarefaPendenteMasConcluiAposFinalizarTarefas() throws Exception {
		Empresa empresa = criarEmpresa("31111111000112");
		Usuario admin = criarUsuario("processo.tarefas.pendentes@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String idProcesso = criarProcessoViaApi(session, "Processo com tarefas", null);

		MvcResult resultadoTarefa = mockMvc.perform(post(URL_TAREFAS).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "processoId": "%s", "titulo": "Tarefa pendente do processo" }
						""".formatted(idProcesso)))
				.andExpect(status().isCreated()).andReturn();
		String idTarefa = com.jayway.jsonpath.JsonPath.read(resultadoTarefa.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(URL_BASE + "/" + idProcesso + "/concluir").session(session).with(csrf()))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(URL_TAREFAS + "/" + idTarefa + "/concluir").session(session).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post(URL_BASE + "/" + idProcesso + "/concluir").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.situacao").value("CONCLUIDO"));
	}

	@Test
	void inativarBloqueiaNovasAlteracoes() throws Exception {
		Empresa empresa = criarEmpresa("31111111000113");
		Usuario admin = criarUsuario("processo.inativar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Processo a inativar", null);

		mockMvc.perform(post(URL_BASE + "/" + id + "/inativar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/iniciar").session(session).with(csrf()))
				.andExpect(status().isConflict());

		mockMvc.perform(post(URL_BASE + "/" + id + "/inativar").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void historicoRegistraCriacaoInicioEConclusao() throws Exception {
		Empresa empresa = criarEmpresa("31111111000114");
		Usuario admin = criarUsuario("processo.historico@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		String id = criarProcessoViaApi(session, "Processo com historico", null);
		mockMvc.perform(post(URL_BASE + "/" + id + "/iniciar").session(session).with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post(URL_BASE + "/" + id + "/concluir").session(session).with(csrf())).andExpect(status().isOk());

		// As tres acoes ocorrem na mesma transacao deste teste (MockMvc +
		// @Transactional), entao CURRENT_TIMESTAMP e identico para as tres
		// linhas (mesmo comportamento do Postgres real dentro de uma unica
		// transacao) - por isso valida presenca dos tipos, nao a ordem exata.
		mockMvc.perform(get(URL_BASE + "/" + id + "/historico").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[?(@.tipoEvento=='CRIACAO')]").exists())
				.andExpect(jsonPath("$[?(@.tipoEvento=='ALTERACAO_SITUACAO')]").exists())
				.andExpect(jsonPath("$[?(@.tipoEvento=='CONCLUSAO')]").exists());
	}

	@Test
	void filtraPorSituacaoPrioridadeEBusca() throws Exception {
		Empresa empresa = criarEmpresa("31111111000115");
		Usuario admin = criarUsuario("processo.filtros@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarProcessoViaApiCompleto(session, "Implantacao Alfa", "URGENTE", null);
		String idBeta = criarProcessoViaApiCompleto(session, "Migracao Beta", "BAIXA", null);
		mockMvc.perform(post(URL_BASE + "/" + idBeta + "/iniciar").session(session).with(csrf())).andExpect(status().isOk());

		mockMvc.perform(get(URL_BASE).session(session).param("busca", "implanta"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));

		mockMvc.perform(get(URL_BASE).session(session).param("prioridade", "URGENTE"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1));

		mockMvc.perform(get(URL_BASE).session(session).param("situacao", "EM_ANDAMENTO"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.itens[0].titulo").value("Migracao Beta"));
	}

	@Test
	void filtraProcessosAtrasados() throws Exception {
		Empresa empresa = criarEmpresa("31111111000116");
		Usuario admin = criarUsuario("processo.atrasado@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate ontem = LocalDate.now().minusDays(1);
		LocalDate proximaSemana = LocalDate.now().plusDays(7);
		criarProcessoComPrazoViaApi(session, "Processo atrasado", ontem);
		criarProcessoComPrazoViaApi(session, "Processo em dia", proximaSemana);

		mockMvc.perform(get(URL_BASE).session(session).param("atrasado", "true"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
				.andExpect(jsonPath("$.itens[0].titulo").value("Processo atrasado"));
	}

	@Test
	void paginaEOrdenaResultados() throws Exception {
		Empresa empresa = criarEmpresa("31111111000117");
		Usuario admin = criarUsuario("processo.paginacao@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		criarProcessoViaApi(session, "Alfa", null);
		criarProcessoViaApi(session, "Beta", null);
		criarProcessoViaApi(session, "Gama", null);

		mockMvc.perform(get(URL_BASE).session(session).param("pagina", "0").param("tamanho", "2")
						.param("ordenarPor", "titulo").param("direcao", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens.length()").value(2))
				.andExpect(jsonPath("$.itens[0].titulo").value("Alfa"))
				.andExpect(jsonPath("$.totalElementos").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2));
	}

	private String criarProcessoViaApi(MockHttpSession session, String titulo, String responsavelId) throws Exception {
		String responsavel = responsavelId == null ? "null" : "\"" + responsavelId + "\"";
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\",\"responsavelId\":" + responsavel + "}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarProcessoViaApiCompleto(MockHttpSession session, String titulo, String prioridade,
			String responsavelId) throws Exception {
		String responsavel = responsavelId == null ? "null" : "\"" + responsavelId + "\"";
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"titulo\":\"" + titulo + "\",\"prioridade\":\"" + prioridade + "\",\"responsavelId\":"
						+ responsavel + "}"))
				.andExpect(status().isCreated()).andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private String criarProcessoComPrazoViaApi(MockHttpSession session, String titulo, LocalDate prazo) throws Exception {
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
