package br.app.criati.convite.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

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
import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContextoConviteControllerTests {

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
	private ConviteRepository conviteRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarConviteQuandoAdministrador() throws Exception {
		Empresa empresa = criarEmpresa("11111111000121");
		MockHttpSession session = loginComPerfil("administrador.convite@criati.test", empresa, PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test", "perfil": "GESTOR" }
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.convite.email").value("convidado@criati.test"))
				.andExpect(jsonPath("$.convite.perfil").value("GESTOR"))
				.andExpect(jsonPath("$.convite.status").value("PENDENTE"))
				.andExpect(jsonPath("$.tokenBruto").exists());
	}

	@Test
	void deveRetornar403QuandoGestorTentaCriarConvite() throws Exception {
		Empresa empresa = criarEmpresa("22222222000122");
		MockHttpSession session = loginComPerfil("gestor.convite@criati.test", empresa, PerfilUsuario.GESTOR);

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void deveRetornar403QuandoUsuarioComumTentaCriarConvite() throws Exception {
		Empresa empresa = criarEmpresa("33333333000123");
		MockHttpSession session = loginComPerfil("usuario.convite@criati.test", empresa, PerfilUsuario.USUARIO);

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveRetornar401ParaRequisicaoAnonima() throws Exception {
		// CSRF valido incluido de proposito para isolar a checagem de
		// autenticacao (sem ele o CsrfFilter rejeitaria antes, com 403).
		mockMvc.perform(post("/api/contexto/convites")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveRetornar400ParaEmailInvalido() throws Exception {
		Empresa empresa = criarEmpresa("44444444000124");
		MockHttpSession session = loginComPerfil("administrador.400@criati.test", empresa, PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "email-invalido", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").value("E-mail deve ser valido"));
	}

	@Test
	void deveRetornar400ParaPerfilAusente() throws Exception {
		Empresa empresa = criarEmpresa("55555555000125");
		MockHttpSession session = loginComPerfil("administrador.perfil@criati.test", empresa, PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(post("/api/contexto/convites")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test" }
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.perfil").value("Perfil e obrigatorio"));
	}

	@Test
	void deveRevogarConvitePendenteExistenteAoConvidarMesmoEmailNovamente() throws Exception {
		Empresa empresa = criarEmpresa("66666666000126");
		MockHttpSession session = loginComPerfil("administrador.duplicado@criati.test", empresa, PerfilUsuario.ADMINISTRADOR);
		String corpo = """
				{ "email": "repetido@criati.test", "perfil": "USUARIO" }
				""";

		mockMvc.perform(post("/api/contexto/convites").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/contexto/convites").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());

		var convites = conviteRepository.findAllByEmpresaId(empresa.getId());
		assertThat(convites).hasSize(2);
		long pendentes = convites.stream().filter(c -> c.getStatus() == StatusConvite.PENDENTE).count();
		long revogados = convites.stream().filter(c -> c.getStatus() == StatusConvite.REVOGADO).count();
		assertThat(pendentes).isEqualTo(1);
		assertThat(revogados).isEqualTo(1);
	}

	@Test
	void deveListarApenasConvitesDaEmpresaAtiva() throws Exception {
		Empresa empresaA = criarEmpresa("77777777000127");
		Empresa empresaB = criarEmpresa("88888888000128");
		MockHttpSession sessionA = loginComPerfil("administrador.a@criati.test", empresaA, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessionB = loginComPerfil("administrador.b@criati.test", empresaB, PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(post("/api/contexto/convites").session(sessionA).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidadoa@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/contexto/convites").session(sessionB).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidadob@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/contexto/convites").session(sessionA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].email").value("convidadoa@criati.test"))
				.andExpect(jsonPath("$[0].tokenHash").doesNotExist());
	}

	@Test
	void deveRevogarConviteDaPropriaEmpresa() throws Exception {
		Empresa empresa = criarEmpresa("10101010000129");
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Administrador", "administrador.revoga@criati.test", passwordEncoder.encode(SENHA),
						StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		Convite convite = conviteRepository.saveAndFlush(new Convite(
				empresa, "revogavel@criati.test", PerfilUsuario.USUARIO, "hash-de-teste-revogar",
				OffsetDateTime.now().plusHours(10), administrador));
		MockHttpSession session = login(administrador.getEmail(), empresa);

		mockMvc.perform(delete("/api/contexto/convites/{id}", convite.getId()).session(session).with(csrf()))
				.andExpect(status().isNoContent());

		Convite atualizado = conviteRepository.findById(convite.getId()).orElseThrow();
		assertThat(atualizado.getStatus()).isEqualTo(StatusConvite.REVOGADO);
	}

	@Test
	void deveRetornar403AoRevogarConviteDeOutraEmpresa() throws Exception {
		Empresa empresaDoConvite = criarEmpresa("20202020000130");
		Empresa outraEmpresa = criarEmpresa("30303030000131");
		Usuario criador = usuarioRepository.saveAndFlush(
				new Usuario("Criador", "criador.convite@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Convite convite = conviteRepository.saveAndFlush(new Convite(
				empresaDoConvite, "de.outra.empresa@criati.test", PerfilUsuario.USUARIO, "hash-outra-empresa",
				OffsetDateTime.now().plusHours(10), criador));
		MockHttpSession session = loginComPerfil("administrador.outra@criati.test", outraEmpresa, PerfilUsuario.ADMINISTRADOR);

		mockMvc.perform(delete("/api/contexto/convites/{id}", convite.getId()).session(session).with(csrf()))
				.andExpect(status().isForbidden());

		Convite inalterado = conviteRepository.findById(convite.getId()).orElseThrow();
		assertThat(inalterado.getStatus()).isEqualTo(StatusConvite.PENDENTE);
	}

	@Test
	void deveRetornar400AoRevogarConviteJaUtilizado() throws Exception {
		Empresa empresa = criarEmpresa("40404040000132");
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Administrador", "administrador.utilizado@criati.test", passwordEncoder.encode(SENHA),
						StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		Convite convite = new Convite(
				empresa, "ja.utilizado@criati.test", PerfilUsuario.USUARIO, "hash-utilizado",
				OffsetDateTime.now().plusHours(10), administrador);
		convite.marcarUtilizado(OffsetDateTime.now());
		conviteRepository.saveAndFlush(convite);
		MockHttpSession session = login(administrador.getEmail(), empresa);

		mockMvc.perform(delete("/api/contexto/convites/{id}", convite.getId()).session(session).with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Convite nao pode ser revogado"));
	}

	@Test
	void deveRetornar403AoCriarConviteQuandoEmpresaFoiDesativadaAposLogin() throws Exception {
		Empresa empresa = criarEmpresa("50505050000133");
		MockHttpSession session = loginComPerfil("administrador.inativa@criati.test", empresa, PerfilUsuario.ADMINISTRADOR);

		Empresa gerenciada = empresaRepository.findById(empresa.getId()).orElseThrow();
		desativar(gerenciada);
		empresaRepository.saveAndFlush(gerenciada);

		mockMvc.perform(post("/api/contexto/convites").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "convidado@criati.test", "perfil": "USUARIO" }
						"""))
				.andExpect(status().isForbidden());
	}

	private void desativar(Empresa empresa) {
		org.springframework.test.util.ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Convite Ltda", "Empresa Convite", cnpj, StatusCadastro.ATIVO));
	}

	private MockHttpSession loginComPerfil(String email, Empresa empresa, PerfilUsuario perfil) throws Exception {
		Usuario usuario = usuarioRepository.saveAndFlush(
				new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		return selecionarContexto(login(email), empresa);
	}

	private MockHttpSession login(String email, Empresa empresa) throws Exception {
		return selecionarContexto(login(email), empresa);
	}

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

	private MockHttpSession selecionarContexto(MockHttpSession session, Empresa empresa) throws Exception {
		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresa.getId())))
				.andExpect(status().isOk());
		return session;
	}
}
