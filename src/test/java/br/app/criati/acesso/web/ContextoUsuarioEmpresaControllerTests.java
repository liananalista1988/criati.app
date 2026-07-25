package br.app.criati.acesso.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.RedefinicaoSenhaAuditoria;
import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.RedefinicaoSenhaAuditoriaRepository;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.AcaoAuditoriaSeguranca;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContextoUsuarioEmpresaControllerTests {

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

	@Autowired
	private RedefinicaoSenhaAuditoriaRepository redefinicaoSenhaAuditoriaRepository;

	// --- listagem ----------------------------------------------------------

	@Test
	void administradorListaIntegrantesDaEmpresaAtiva() throws Exception {
		Usuario administrador = criarUsuario("admin.lista@criati.test");
		Empresa empresa = criarEmpresa("11111111100001");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membro = criarUsuario("membro.lista@criati.test");
		criarVinculo(membro, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[?(@.email=='membro.lista@criati.test')].perfil").value("GESTOR"));
	}

	@Test
	void listagemNaoRetornaIntegrantesDeOutraEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.isolamento@criati.test");
		Empresa empresaA = criarEmpresa("11111111100101");
		Empresa empresaB = criarEmpresa("22222222100102");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.b@criati.test");
		criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].email").value(administrador.getEmail()));
	}

	@Test
	void listagemNaoExpoeSenhaOuHash() throws Exception {
		Usuario administrador = criarUsuario("admin.seguro@criati.test");
		Empresa empresa = criarEmpresa("11111111100201");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].senha").doesNotExist())
				.andExpect(jsonPath("$[0].superAdministrador").doesNotExist());
	}

	@Test
	void gestorRecebe403AoListar() throws Exception {
		Usuario gestor = criarUsuario("gestor.lista@criati.test");
		Empresa empresa = criarEmpresa("11111111100301");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(gestor.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isForbidden());
	}

	@Test
	void usuarioComumRecebe403AoListar() throws Exception {
		Usuario comum = criarUsuario("usuario.lista@criati.test");
		Empresa empresa = criarEmpresa("11111111100401");
		criarVinculo(comum, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(comum.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonimoRecebe401AoListar() throws Exception {
		mockMvc.perform(get("/api/contexto/usuarios"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void semEmpresaAtivaRecebe403AoListar() throws Exception {
		Usuario administrador = criarUsuario("admin.semcontexto@criati.test");
		MockHttpSession session = login(administrador.getEmail());

		mockMvc.perform(get("/api/contexto/usuarios").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void listagemFiltraPorPerfil() throws Exception {
		Usuario administrador = criarUsuario("admin.filtro@criati.test");
		Empresa empresa = criarEmpresa("11111111100501");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario gestor = criarUsuario("gestor.filtro@criati.test");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios").session(session).param("perfil", "GESTOR"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].email").value("gestor.filtro@criati.test"));
	}

	// --- consulta individual -----------------------------------------------

	@Test
	void administradorConsultaVinculoDaPropriaEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.consulta@criati.test");
		Empresa empresa = criarEmpresa("11111111100601");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios/" + vinculoAdmin.getId()).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(administrador.getEmail()))
				.andExpect(jsonPath("$.proprioUsuario").value(true));
	}

	@Test
	void consultaDeVinculoDeOutraEmpresaRetornaRespostaGenerica() throws Exception {
		Usuario administrador = criarUsuario("admin.consulta2@criati.test");
		Empresa empresaA = criarEmpresa("11111111100701");
		Empresa empresaB = criarEmpresa("22222222100702");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.consulta2@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios/" + vinculoB.getId()).session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void consultaDeVinculoInexistenteRetornaRespostaGenerica() throws Exception {
		Usuario administrador = criarUsuario("admin.consulta3@criati.test");
		Empresa empresa = criarEmpresa("11111111100801");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/usuarios/" + UUID.randomUUID()).session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	// --- alteracao de perfil ------------------------------------------------

	@Test
	void administradorAlteraPerfilDeUsuarioParaGestor() throws Exception {
		Usuario administrador = criarUsuario("admin.perfil@criati.test");
		Empresa empresa = criarEmpresa("11111111100901");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.perfil@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(patch("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/perfil")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "GESTOR" }
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.perfil").value("GESTOR"));

		assertThat(usuarioEmpresaRepository.findById(vinculoAlvo.getId()).orElseThrow().getPerfil())
				.isEqualTo(PerfilUsuario.GESTOR);
	}

	@Test
	void alteracaoDePerfilComValorInvalidoRetorna400() throws Exception {
		Usuario administrador = criarUsuario("admin.perfilinvalido@criati.test");
		Empresa empresa = criarEmpresa("11111111101001");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.perfilinvalido@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(patch("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/perfil")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "SUPERADMIN" }
						"""))
				.andExpect(status().isBadRequest());
	}

	// Nao ha teste HTTP de "outro chamador rebaixa o ultimo Administrador":
	// para chamar este endpoint o proprio chamador precisa ser um
	// Administrador ATIVO da empresa: se so existe um Administrador ativo,
	// o unico chamador possivel e ele mesmo, cenario ja coberto (e barrado
	// antes) por administradorNaoPodeRebaixarAProprioPerfil. A protecao do
	// ultimo Administrador diante de um chamador diferente so e alcancavel
	// por uma corrida de concorrencia; ver GerenciarUsuarioEmpresaServiceTests
	// (chamada direta ao servico) e docs/DECISOES.md.

	@Test
	void administradorNaoPodeRebaixarAProprioPerfil() throws Exception {
		Usuario administrador = criarUsuario("admin.autorebaixa@criati.test");
		Empresa empresa = criarEmpresa("11111111101201");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario outroAdmin = criarUsuario("admin.outro2@criati.test");
		criarVinculo(outroAdmin, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(patch("/api/contexto/usuarios/" + vinculoAdmin.getId() + "/perfil")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "GESTOR" }
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Operacao nao pode ser realizada sobre o proprio vinculo"));
	}

	@Test
	void gestorRecebe403AoAlterarPerfil() throws Exception {
		Usuario gestor = criarUsuario("gestor.altera@criati.test");
		Empresa empresa = criarEmpresa("11111111101301");
		criarVinculo(gestor, empresa, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.altera@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(gestor.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(patch("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/perfil")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "GESTOR" }
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonimoRecebe401AoAlterarPerfil() throws Exception {
		mockMvc.perform(patch("/api/contexto/usuarios/" + UUID.randomUUID() + "/perfil")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "perfil": "GESTOR" }
						"""))
				.andExpect(status().isUnauthorized());
	}

	// --- suspensao -----------------------------------------------------------

	@Test
	void administradorSuspendeVinculoComum() throws Exception {
		Usuario administrador = criarUsuario("admin.suspende@criati.test");
		Empresa empresa = criarEmpresa("11111111101401");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.suspende@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender")
				.session(session)
				.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INATIVO"));

		Usuario alvoRecarregado = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoRecarregado.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void usuarioSuspensoNaoConsegueSelecionarEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.suspende2@criati.test");
		Empresa empresa = criarEmpresa("11111111101501");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.suspende2@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession sessionAdmin = login(administrador.getEmail());
		selecionarEmpresa(sessionAdmin, empresa.getId()).andExpect(status().isOk());
		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender")
				.session(sessionAdmin)
				.with(csrf()))
				.andExpect(status().isOk());

		MockHttpSession sessionAlvo = login(alvo.getEmail());
		selecionarEmpresa(sessionAlvo, empresa.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void contextoJaSelecionadoDeixaDeSerValidoAposSuspensao() throws Exception {
		Usuario administrador = criarUsuario("admin.suspende3@criati.test");
		Empresa empresa = criarEmpresa("11111111101601");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.suspende3@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession sessionAlvo = login(alvo.getEmail());
		selecionarEmpresa(sessionAlvo, empresa.getId()).andExpect(status().isOk());

		MockHttpSession sessionAdmin = login(administrador.getEmail());
		selecionarEmpresa(sessionAdmin, empresa.getId()).andExpect(status().isOk());
		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender")
				.session(sessionAdmin)
				.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessionAlvo))
				.andExpect(status().isNoContent());
	}

	// Mesmo raciocinio de administradorNaoPodeRebaixarAProprioPerfil: a
	// protecao do ultimo Administrador diante de um chamador diferente nao e
	// alcancavel via HTTP (o chamador precisa ser Administrador ativo, e se
	// so ha um, o chamador so pode ser ele mesmo). Coberta diretamente no
	// servico por deveRejeitarSuspensaoDoUltimoAdministradorAtivoDiretoPeloServico.

	@Test
	void administradorNaoPodeSuspenderAProprio() throws Exception {
		Usuario administrador = criarUsuario("admin.autosuspende@criati.test");
		Empresa empresa = criarEmpresa("11111111101801");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAdmin.getId() + "/suspender")
				.session(session)
				.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Operacao nao pode ser realizada sobre o proprio vinculo"));
	}

	@Test
	void naoPodeSuspenderVinculoDeOutraEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.outraempresa@criati.test");
		Empresa empresaA = criarEmpresa("11111111101901");
		Empresa empresaB = criarEmpresa("22222222101902");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.outraempresa@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoB.getId() + "/suspender")
				.session(session)
				.with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		assertThat(usuarioEmpresaRepository.findById(vinculoB.getId()).orElseThrow().getStatus())
				.isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void repetirSuspensaoRetornaConflito() throws Exception {
		Usuario administrador = criarUsuario("admin.repetesuspende@criati.test");
		Empresa empresa = criarEmpresa("11111111102001");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.repetesuspende@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender").session(session).with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/suspender").session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Vinculo ja esta inativo"));
	}

	// --- reativacao ----------------------------------------------------------

	@Test
	void administradorReativaVinculoInativoPreservandoPerfil() throws Exception {
		Usuario administrador = criarUsuario("admin.reativa@criati.test");
		Empresa empresa = criarEmpresa("11111111102101");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.reativa@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.GESTOR, StatusCadastro.INATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.perfil").value("GESTOR"));

		assertThat(usuarioRepository.findById(alvo.getId()).orElseThrow().getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void repetirReativacaoRetornaConflito() throws Exception {
		Usuario administrador = criarUsuario("admin.reativarepete@criati.test");
		Empresa empresa = criarEmpresa("11111111102201");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.reativarepete@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Vinculo ja esta ativo"));
	}

	@Test
	void naoPodeReativarVinculoDeOutraEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.reativaoutra@criati.test");
		Empresa empresaA = criarEmpresa("11111111102301");
		Empresa empresaB = criarEmpresa("22222222102302");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.reativaoutra@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.INATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoB.getId() + "/reativar").session(session).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	// --- remocao logica --------------------------------------------------

	@Test
	void administradorRemoveLogicamenteVinculoComum() throws Exception {
		Usuario administrador = criarUsuario("admin.remove@criati.test");
		Empresa empresa = criarEmpresa("11111111102401");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.remove@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoAlvo.getId()).session(session).with(csrf()))
				.andExpect(status().isNoContent());

		UsuarioEmpresa vinculoRecarregado = usuarioEmpresaRepository.findById(vinculoAlvo.getId()).orElseThrow();
		assertThat(vinculoRecarregado.getStatus()).isEqualTo(StatusCadastro.INATIVO);
		assertThat(usuarioRepository.findById(alvo.getId())).isPresent();
	}

	@Test
	void naoExecutaDeleteFisico() throws Exception {
		Usuario administrador = criarUsuario("admin.semdeletefisico@criati.test");
		Empresa empresa = criarEmpresa("11111111102501");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.semdeletefisico@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		long totalAntes = usuarioEmpresaRepository.count();

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoAlvo.getId()).session(session).with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(usuarioEmpresaRepository.count()).isEqualTo(totalAntes);
	}

	// Mesmo raciocinio das secoes de alteracao de perfil e suspensao: coberta
	// diretamente no servico por deveRejeitarRemocaoDoUltimoAdministradorAtivo.

	@Test
	void administradorNaoPodeRemoverAProprio() throws Exception {
		Usuario administrador = criarUsuario("admin.autoremove@criati.test");
		Empresa empresa = criarEmpresa("11111111102701");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoAdmin.getId()).session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Operacao nao pode ser realizada sobre o proprio vinculo"));
	}

	@Test
	void vinculoRemovidoNaoConsegueSelecionarEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.removeseleciona@criati.test");
		Empresa empresa = criarEmpresa("11111111102801");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.removeseleciona@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession sessionAdmin = login(administrador.getEmail());
		selecionarEmpresa(sessionAdmin, empresa.getId()).andExpect(status().isOk());
		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoAlvo.getId()).session(sessionAdmin).with(csrf()))
				.andExpect(status().isNoContent());

		MockHttpSession sessionAlvo = login(alvo.getEmail());
		selecionarEmpresa(sessionAlvo, empresa.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void repetirRemocaoRetornaConflito() throws Exception {
		Usuario administrador = criarUsuario("admin.removerepete@criati.test");
		Empresa empresa = criarEmpresa("11111111102901");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.removerepete@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.INATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoAlvo.getId()).session(session).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Vinculo ja esta inativo"));
	}

	@Test
	void naoPodeRemoverVinculoDeOutraEmpresa() throws Exception {
		Usuario administrador = criarUsuario("admin.removeoutra@criati.test");
		Empresa empresaA = criarEmpresa("11111111103001");
		Empresa empresaB = criarEmpresa("22222222103002");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.removeoutra@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/usuarios/" + vinculoB.getId()).session(session).with(csrf()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		assertThat(usuarioEmpresaRepository.findById(vinculoB.getId()).orElseThrow().getStatus())
				.isEqualTo(StatusCadastro.ATIVO);
	}

	// --- redefinicao administrativa de senha (CRIATI-SEG-001) --------------

	private static final String SENHA_NOVA = "senha-nova-com-quinze-mais";

	@Test
	void administradorRedefineSenhaDeUsuarioDaPropriaEmpresaENovaSenhaPermiteLogin() throws Exception {
		Usuario administrador = criarUsuario("admin.redefine@criati.test");
		Empresa empresa = criarEmpresa("31111111100001");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.redefine@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagem").exists())
				// resposta nunca contem a senha, o hash ou o registro de auditoria.
				.andExpect(jsonPath("$.novaSenha").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.hash").doesNotExist())
				.andExpect(jsonPath("$.auditoria").doesNotExist());

		// nova senha permite login.
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(alvo.getEmail(), SENHA_NOVA)))
				.andExpect(status().isOk());

		// senha antiga deixa de funcionar.
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(alvo.getEmail(), SENHA)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioComumRecebe403AoTentarRedefinirSenha() throws Exception {
		Usuario comum = criarUsuario("usuario.redefine@criati.test");
		Empresa empresa = criarEmpresa("31111111100101");
		criarVinculo(comum, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.redefine2@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(comum.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isForbidden());

		assertThat(redefinicaoSenhaAuditoriaRepository.findAll()).isEmpty();
		assertThat(passwordEncoder.matches(SENHA, usuarioRepository.findById(alvo.getId()).orElseThrow().getSenha()))
				.isTrue();
	}

	@Test
	void naoPodeRedefinirSenhaDeUsuarioDeOutraEmpresaENaoCriaAuditoria() throws Exception {
		Usuario administrador = criarUsuario("admin.redefineoutra@criati.test");
		Empresa empresaA = criarEmpresa("31111111100201");
		Empresa empresaB = criarEmpresa("32222222100202");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario membroB = criarUsuario("membro.redefineoutra@criati.test");
		UsuarioEmpresa vinculoB = criarVinculo(membroB, empresaB, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoB.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		assertThat(redefinicaoSenhaAuditoriaRepository.findAll()).isEmpty();
		assertThat(passwordEncoder.matches(SENHA, usuarioRepository.findById(membroB.getId()).orElseThrow().getSenha()))
				.isTrue();
	}

	@Test
	void confirmacaoDivergenteERejeitadaSemCriarAuditoria() throws Exception {
		Usuario administrador = criarUsuario("admin.divergente@criati.test");
		Empresa empresa = criarEmpresa("31111111100301");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.divergente@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "outra-senha-bem-diferente" }
						""".formatted(SENHA_NOVA)))
				.andExpect(status().isBadRequest());

		assertThat(redefinicaoSenhaAuditoriaRepository.findAll()).isEmpty();
	}

	@Test
	void senhaAbaixoDoTamanhoMinimoERejeitadaSemCriarAuditoria() throws Exception {
		Usuario administrador = criarUsuario("admin.curta@criati.test");
		Empresa empresa = criarEmpresa("31111111100401");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.curta@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "curta123", "confirmacaoSenha": "curta123" }
						"""))
				.andExpect(status().isBadRequest());

		assertThat(redefinicaoSenhaAuditoriaRepository.findAll()).isEmpty();
	}

	@Test
	void administradorNaoPodeRedefinirAPropriaSenha() throws Exception {
		Usuario administrador = criarUsuario("admin.autoredefine@criati.test");
		Empresa empresa = criarEmpresa("31111111100501");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAdmin.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Operacao nao pode ser realizada sobre o proprio vinculo"));

		assertThat(redefinicaoSenhaAuditoriaRepository.findAll()).isEmpty();
	}

	@Test
	void anonimoRecebe401AoRedefinirSenha() throws Exception {
		mockMvc.perform(post("/api/contexto/usuarios/" + UUID.randomUUID() + "/redefinir-senha")
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cadaRedefinicaoDeSenhaCriaUmEventoDeAuditoriaSeparadoComDadosCorretosSemSenha() throws Exception {
		Usuario administrador = criarUsuario("admin.auditoria@criati.test");
		Empresa empresa = criarEmpresa("31111111100601");
		criarVinculo(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.auditoria@criati.test");
		UsuarioEmpresa vinculoAlvo = criarVinculo(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA, SENHA_NOVA)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "outra-senha-com-quinze-mais", "confirmacaoSenha": "outra-senha-com-quinze-mais" }
						"""))
				.andExpect(status().isOk());

		// duas redefinicoes preservam dois registros distintos, nenhum sobrescrito.
		java.util.List<RedefinicaoSenhaAuditoria> eventos = redefinicaoSenhaAuditoriaRepository.findAll();
		assertThat(eventos).hasSize(2);
		eventos.forEach(evento -> {
			assertThat(evento.getEmpresa().getId()).isEqualTo(empresa.getId());
			assertThat(evento.getAdministrador().getId()).isEqualTo(administrador.getId());
			assertThat(evento.getUsuarioAfetado().getId()).isEqualTo(alvo.getId());
			assertThat(evento.getAcao()).isEqualTo(AcaoAuditoriaSeguranca.REDEFINICAO_ADMINISTRATIVA_SENHA);
			assertThat(evento.getCriadoEm()).isNotNull();
		});

		// o evento de auditoria nunca guarda senha nem hash - so as colunas
		// modeladas na migration V16 (id, empresa, administrador, usuario
		// afetado, acao, data). Nao ha campo de senha para checar em runtime;
		// a garantia aqui e estrutural (ver RedefinicaoSenhaAuditoria.java).
	}

	// --- auxiliares --------------------------------------------------------

	private MockHttpSession login(String email) throws Exception {
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

	private ResultActions selecionarEmpresa(MockHttpSession session, UUID empresaId) throws Exception {
		return mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private Empresa criarEmpresa(String cnpj) {
		Empresa empresa = new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO);
		return empresaRepository.saveAndFlush(empresa);
	}

	private UsuarioEmpresa criarVinculo(
			Usuario usuario, Empresa empresa, PerfilUsuario perfil, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, status);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

}
