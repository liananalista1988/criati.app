package br.app.criati.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;
import br.app.criati.admin.repository.RedefinicaoSenhaGlobalAuditoriaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.AcaoAuditoriaSegurancaGlobal;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUsuarioControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String SENHA_NOVA_GLOBAL = "senha-nova-global-com-quinze-mais";

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
	private RedefinicaoSenhaGlobalAuditoriaRepository redefinicaoSenhaGlobalAuditoriaRepository;

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

	// --- redefinicao GLOBAL de senha (CRIATI-SEG-001) -----------------------

	@Test
	void superAdministradorRedefineSenhaDeUsuarioComumENovaSenhaPermiteLogin() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.global@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.global@criati.test", StatusCadastro.ATIVO);

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mensagem").exists())
				.andExpect(jsonPath("$.novaSenha").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist());

		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(alvo.getEmail(), SENHA_NOVA_GLOBAL)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(alvo.getEmail(), SENHA)))
				.andExpect(status().isUnauthorized());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		RedefinicaoSenhaGlobalAuditoria evento = eventos.get(0);
		assertThat(evento.getUsuarioAlvoId()).isEqualTo(alvo.getId());
		assertThat(evento.getAcao()).isEqualTo(AcaoAuditoriaSegurancaGlobal.REDEFINICAO_ADMINISTRATIVA_SENHA_GLOBAL);
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.SUCESSO);
		assertThat(evento.getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.REDEFINICAO_CONCLUIDA);
		assertThat(evento.getIpOrigem()).isNotBlank();
		assertThat(evento.getCriadoEm()).isNotNull();
	}

	@Test
	void usuarioComumRecebe403AoTentarRedefinirSenhaGlobalEAuditaSemPermissaoSemDuplicar() throws Exception {
		String emailComum = "usuario.comum.redefine.global@criati.test";
		MockHttpSession sessionComum = login(emailComum, false);
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.negado@criati.test", StatusCadastro.ATIVO);
		String hashOriginalAlvo = alvo.getSenha();
		commitarSetupParaAuditoriaEmTransacaoIndependente();
		Usuario chamadorComum = usuarioRepository.findByEmailIgnoreCase(emailComum).orElseThrow();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionComum).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isForbidden());

		// bloqueado pelo SecurityConfig (/api/admin/** exige ROLE_SUPERADMIN) antes
		// mesmo de chegar ao service/controller - a auditoria agora acontece no
		// AccessDeniedHandler (RedefinirSenhaGlobalAcessoNegadoAuditor), o unico
		// ponto que intercepta essa negacao real via HTTP. Exatamente um evento e
		// criado (sem duplicar com o guard redundante do proprio service, que
		// aqui nunca chega a executar - ver RedefinirSenhaGlobalServiceTests).
		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.NEGADO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.SEM_PERMISSAO);
		assertThat(eventos.get(0).getAdministrador().getId()).isEqualTo(chamadorComum.getId());

		// acesso realmente negado (nao apenas auditado): senha do alvo intacta
		Usuario alvoAtual = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoAtual.getSenha()).isEqualTo(hashOriginalAlvo);
	}

	@Test
	void anonimoRecebe401AoRedefinirSenhaGlobal() throws Exception {
		Usuario alvo = criarUsuarioGlobal("alvo.anonimo.global@criati.test", StatusCadastro.ATIVO);

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void superAdministradorNaoPodeRedefinirAPropriaSenhaGlobalEAuditaNegado() throws Exception {
		String email = "superadmin.auto.redefine.global@criati.test";
		Usuario superAdmin = Usuario.criarSuperAdministrador("Super Admin Proprio", email, passwordEncoder.encode(SENHA));
		usuarioRepository.saveAndFlush(superAdmin);
		MockHttpSession session = autenticar(email);
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + superAdmin.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isConflict());

		List<RedefinicaoSenhaGlobalAuditoria> eventos = redefinicaoSenhaGlobalAuditoriaRepository
				.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(superAdmin.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.NEGADO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.PROPRIO_USUARIO);
	}

	@Test
	void naoPodeRedefinirSenhaDeUsuarioGlobalInativoEAuditaNegado() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.inativo@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.inativo@criati.test", StatusCadastro.INATIVO);
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isConflict());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.NEGADO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.USUARIO_INATIVO);
	}

	@Test
	void naoPodeRedefinirSenhaDeUsuarioGlobalInexistente() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.inexistente@criati.test");
		UUID idInexistente = UUID.randomUUID();
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + idInexistente + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
						""".formatted(SENHA_NOVA_GLOBAL, SENHA_NOVA_GLOBAL)))
				.andExpect(status().isNotFound());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(idInexistente);
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.NEGADO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.USUARIO_NAO_ENCONTRADO);
	}

	@Test
	void senhaAbaixoDoTamanhoMinimoERejeitadaComAuditoriaDeFalhaValidacao() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.curta@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.curta@criati.test", StatusCadastro.ATIVO);
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "curta123", "confirmacaoSenha": "curta123" }
						"""))
				.andExpect(status().isBadRequest());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.FALHA_VALIDACAO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.SENHA_INVALIDA);
	}

	@Test
	void confirmacaoDivergenteERejeitadaComAuditoriaDeFalhaValidacao() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.divergente@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.divergente@criati.test", StatusCadastro.ATIVO);
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "outra-senha-bem-diferente" }
						""".formatted(SENHA_NOVA_GLOBAL)))
				.andExpect(status().isBadRequest());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.FALHA_VALIDACAO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.SENHA_INVALIDA);
	}

	// RedefinirSenhaGlobalRequest nao usa @NotBlank de proposito: um corpo com
	// novaSenha/confirmacaoSenha vazia (ou ausente) precisa chegar ate o
	// service para ser validado por SenhaValidador e auditado como
	// FALHA_VALIDACAO/SENHA_INVALIDA - se @NotBlank barrasse antes, o
	// MethodArgumentNotValidException do Bean Validation responderia 400 sem
	// nenhuma auditoria (gap corrigido aqui).
	@Test
	void senhaVaziaERejeitadaComAuditoriaDeFalhaValidacaoSemAlterarSenhaArmazenada() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.vazia@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.vazia@criati.test", StatusCadastro.ATIVO);
		String hashOriginal = alvo.getSenha();
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "", "confirmacaoSenha": "" }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.novaSenha").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.FALHA_VALIDACAO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.SENHA_INVALIDA);

		Usuario alvoAtual = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoAtual.getSenha()).isEqualTo(hashOriginal);
	}

	@Test
	void confirmacaoVaziaERejeitadaComAuditoriaDeFalhaValidacaoSemAlterarSenhaArmazenada() throws Exception {
		MockHttpSession sessionSuperAdmin = loginSuperAdministrador("superadmin.redefine.confvazia@criati.test");
		Usuario alvo = criarUsuarioGlobal("alvo.redefine.confvazia@criati.test", StatusCadastro.ATIVO);
		String hashOriginal = alvo.getSenha();
		commitarSetupParaAuditoriaEmTransacaoIndependente();

		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(sessionSuperAdmin).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "%s", "confirmacaoSenha": "" }
						""".formatted(SENHA_NOVA_GLOBAL)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.novaSenha").doesNotExist())
				.andExpect(jsonPath("$.senha").doesNotExist());

		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).hasSize(1);
		assertThat(eventos.get(0).getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.FALHA_VALIDACAO);
		assertThat(eventos.get(0).getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.SENHA_INVALIDA);

		Usuario alvoAtual = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoAtual.getSenha()).isEqualTo(hashOriginal);
	}

	// RedefinicaoSenhaGlobalAuditoriaService.registrarFalha roda em transacao
	// PROPRIA (REQUIRES_NEW, conexao/transacao fisica diferente da deste
	// teste @Transactional). O administrador/usuario-alvo criados acima ainda
	// nao foram commitados fisicamente (so flush na transacao deste teste) -
	// sem commitar antes, a FK de administrador_id falharia por nao enxergar
	// uma linha que so existe, sem commit, na transacao do teste. Chamado so
	// nos testes que exercitam um caminho de falha (que aciona REQUIRES_NEW);
	// o caminho de sucesso grava a auditoria na mesma transacao ambiente,
	// entao nao precisa disso.
	private void commitarSetupParaAuditoriaEmTransacaoIndependente() {
		TestTransaction.flagForCommit();
		TestTransaction.end();
	}

	private Usuario criarUsuarioGlobal(String email, StatusCadastro status) {
		Usuario usuario = new Usuario("Usuario Global Teste", email, passwordEncoder.encode(SENHA), status);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private MockHttpSession autenticar(String email) throws Exception {
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
