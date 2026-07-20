package br.app.criati.convite.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.convite.service.ConviteCriado;
import br.app.criati.convite.service.ConviteService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ConvitePublicoControllerTests {

	private static final String SENHA_VALIDA = "senha-bastante-segura-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConviteService conviteService;

	@Autowired
	private ConviteRepository conviteRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveRetornarValidoParaConviteValido() throws Exception {
		Empresa empresa = criarEmpresa("11111111000141", "Empresa Publica Ltda");
		ConviteCriado criado = criarConvite(empresa, "convidado.publico@criati.test", PerfilUsuario.GESTOR);

		mockMvc.perform(get("/api/convites/{token}", criado.tokenBruto()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valido").value(true))
				.andExpect(jsonPath("$.empresa").value("Empresa Publica Ltda"))
				.andExpect(jsonPath("$.emailMascarado").value("co***@criati.test"))
				.andExpect(jsonPath("$.perfil").value("GESTOR"))
				.andExpect(jsonPath("$.expiraEm").exists());
	}

	@Test
	void deveRetornarInvalidoParaTokenInexistente() throws Exception {
		mockMvc.perform(get("/api/convites/{token}", "token-que-nao-existe"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valido").value(false))
				.andExpect(jsonPath("$.empresa").doesNotExist())
				.andExpect(jsonPath("$.emailMascarado").doesNotExist());
	}

	@Test
	void deveRetornarInvalidoParaConviteExpirado() throws Exception {
		Empresa empresa = criarEmpresa("22222222000142", "Empresa Expirada Ltda");
		Usuario criador = criarUsuario("criador.expirado@criati.test");
		Convite convite = new Convite(empresa, "expirado@criati.test", PerfilUsuario.USUARIO, "hash-expirado-teste",
				OffsetDateTime.now().minusHours(1), criador);
		conviteRepository.saveAndFlush(convite);

		mockMvc.perform(get("/api/convites/{token}", "token-cujo-hash-nao-corresponde"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valido").value(false));
	}

	@Test
	void deveRetornarInvalidoParaConviteUtilizado() throws Exception {
		Empresa empresa = criarEmpresa("33333333000143", "Empresa Utilizada Ltda");
		ConviteCriado criado = criarConvite(empresa, "utilizado@criati.test", PerfilUsuario.USUARIO);
		aceitar(criado.tokenBruto(), "Convidado Utilizado");

		mockMvc.perform(get("/api/convites/{token}", criado.tokenBruto()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valido").value(false));
	}

	@Test
	void deveRetornarInvalidoParaConviteRevogado() throws Exception {
		Empresa empresa = criarEmpresa("44444444000144", "Empresa Revogada Ltda");
		ConviteCriado criado = criarConvite(empresa, "revogado@criati.test", PerfilUsuario.USUARIO);
		criado.convite().revogar();
		conviteRepository.saveAndFlush(criado.convite());

		mockMvc.perform(get("/api/convites/{token}", criado.tokenBruto()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valido").value(false));
	}

	@Test
	void deveAceitarConviteECriarUsuarioEVinculoAtivos() throws Exception {
		Empresa empresa = criarEmpresa("55555555000145", "Empresa Aceite Ltda");
		ConviteCriado criado = criarConvite(empresa, "novo.usuario@criati.test", PerfilUsuario.GESTOR);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Novo Usuario",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Novo Usuario"))
				.andExpect(jsonPath("$.email").value("novo.usuario@criati.test"))
				.andExpect(jsonPath("$.status").value("ATIVO"))
				.andExpect(jsonPath("$.senha").doesNotExist());

		Usuario usuarioCriado = usuarioRepository.findByEmailIgnoreCase("novo.usuario@criati.test").orElseThrow();
		assertThat(usuarioCriado.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(usuarioCriado.getSenha()).isNotEqualTo(SENHA_VALIDA);
		assertThat(passwordEncoder.matches(SENHA_VALIDA, usuarioCriado.getSenha())).isTrue();

		var vinculo = usuarioEmpresaRepository
				.findByUsuarioIdAndEmpresaId(usuarioCriado.getId(), empresa.getId())
				.orElseThrow();
		assertThat(vinculo.getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
		assertThat(vinculo.getStatus()).isEqualTo(StatusCadastro.ATIVO);

		Convite conviteAtualizado = conviteRepository.findById(criado.convite().getId()).orElseThrow();
		assertThat(conviteAtualizado.getStatus()).isEqualTo(StatusConvite.UTILIZADO);
		assertThat(conviteAtualizado.getUtilizadoEm()).isNotNull();
	}

	@Test
	void deveRetornar400QuandoSenhaEConfirmacaoDivergem() throws Exception {
		Empresa empresa = criarEmpresa("66666666000146", "Empresa Divergente Ltda");
		ConviteCriado criado = criarConvite(empresa, "divergente@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Fulano",
						  "senha": "%s",
						  "confirmacaoSenha": "senha-completamente-diferente-123"
						}
						""".formatted(SENHA_VALIDA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Confirmacao de senha nao corresponde a senha"));
	}

	@Test
	void deveRetornar400QuandoSenhaForFraca() throws Exception {
		Empresa empresa = criarEmpresa("77777777000147", "Empresa Senha Fraca Ltda");
		ConviteCriado criado = criarConvite(empresa, "senha.fraca@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Fulano",
						  "senha": "curta123",
						  "confirmacaoSenha": "curta123"
						}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveRetornar404AoReutilizarTokenJaAceito() throws Exception {
		Empresa empresa = criarEmpresa("88888888000148", "Empresa Reuso Ltda");
		ConviteCriado criado = criarConvite(empresa, "reuso@criati.test", PerfilUsuario.USUARIO);
		aceitar(criado.tokenBruto(), "Primeiro Aceite");

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Segunda Tentativa",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Convite invalido ou expirado"));
	}

	@Test
	void deveRetornar409QuandoEmailJaPossuiUsuario() throws Exception {
		Empresa empresa = criarEmpresa("99999999000149", "Empresa Existente Ltda");
		criarUsuario("ja.cadastrado@criati.test");
		ConviteCriado criado = criarConvite(empresa, "ja.cadastrado@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Tentativa Takeover",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("E-mail ja cadastrado"));

		Convite conviteInalterado = conviteRepository.findById(criado.convite().getId()).orElseThrow();
		assertThat(conviteInalterado.getStatus()).isEqualTo(StatusConvite.PENDENTE);
		// Apenas o vinculo do administrador que criou o convite (ja existente
		// antes da tentativa de aceite) deve permanecer - nenhum vinculo novo
		// foi criado para o e-mail em conflito.
		assertThat(usuarioEmpresaRepository.findAllByEmpresaId(empresa.getId())).hasSize(1);
	}

	@Test
	void deveRetornar404AoAceitarQuandoEmpresaFoiDesativada() throws Exception {
		Empresa empresa = criarEmpresa("12121212000150", "Empresa Sera Desativada Ltda");
		ConviteCriado criado = criarConvite(empresa, "empresa.inativa@criati.test", PerfilUsuario.USUARIO);
		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Fulano",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isNotFound());
	}

	@Test
	void naoDeveCriarSessaoAutenticadaAoAceitarConvite() throws Exception {
		Empresa empresa = criarEmpresa("13131313000151", "Empresa Sem Sessao Ltda");
		ConviteCriado criado = criarConvite(empresa, "sem.sessao@criati.test", PerfilUsuario.USUARIO);

		MvcResult resultado = mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Fulano",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	private ConviteCriado criarConvite(Empresa empresa, String email, PerfilUsuario perfil) {
		Usuario administrador = criarUsuario("administrador." + UUID.randomUUID() + "@criati.test");
		usuarioEmpresaRepository.saveAndFlush(
				new br.app.criati.acesso.model.UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR,
						StatusCadastro.ATIVO));
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				administrador.getId(), empresa.getId(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		return conviteService.criar(email, perfil, contexto);
	}

	private void aceitar(String tokenBruto, String nome) throws Exception {
		mockMvc.perform(post("/api/convites/{token}/aceitar", tokenBruto)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "%s",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(nome, SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isOk());
	}

	private Empresa criarEmpresa(String cnpj, String nome) {
		return empresaRepository.saveAndFlush(new Empresa(nome, nome, cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(
				new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA_VALIDA), StatusCadastro.ATIVO));
	}
}
