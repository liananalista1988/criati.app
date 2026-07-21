package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.convite.service.ConviteCriado;
import br.app.criati.convite.service.ConviteService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Cobre a pagina publica de aceite de convite (/convites/{token}): renderiza
 * via MockMvc (sem executar JavaScript), confirma que a rota e realmente
 * publica, que nenhum dado sensivel aparece no HTML renderizado, e que a
 * tela de login reconhece ?motivo=convite-aceito sem refletir motivos
 * desconhecidos.
 *
 * Nao ha teste de "GET na pagina nao cria sessao": em execucao de suite
 * completa (nao isolada), qualquer pagina que renderiza {@code ${_csrf.token}}
 * - inclusive `/login`, ja existente antes desta tarefa - acaba com uma
 * MockHttpSession no request de teste, efeito colateral do uso extensivo de
 * `.with(csrf())` (SecurityMockMvcRequestPostProcessors) no restante da
 * suite, que sempre usa HttpSessionCsrfTokenRepository internamente
 * independente do repositorio configurado na aplicacao (CookieCsrfTokenRepository,
 * ver SecurityConfig). Confirmado isolando o teste (passa sozinho) e
 * reproduzindo o mesmo efeito em `/login` sob as mesmas condicoes - artefato
 * de infraestrutura de teste, nao uma sessao de autenticacao real. A
 * garantia que importa de fato - nenhuma autenticacao automatica apos aceitar
 * o convite - e verificada em {@link #sucessoNaoAutenticaUsuarioAutomaticamente()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaConviteAceitePublicaTests {

	private static final String SENHA_VALIDA = "senha-bastante-segura-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConviteService conviteService;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Test
	void retorna200ParaAnonimoSemExigirAutenticacao() throws Exception {
		mockMvc.perform(get("/convites/{token}", "qualquer-token-de-teste"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
	}

	@Test
	void paginaContemEstadosFormularioEScripts() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{token}", "token-conteudo"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.contains("Aceite seu convite")
				.contains("criati-convite-carregando")
				.contains("criati-convite-invalido")
				.contains("criati-convite-erro-rede")
				.contains("criati-convite-valido")
				.contains("criati-convite-sucesso")
				.contains("criati-convite-form")
				.contains("id=\"criati-convite-nome\"")
				.contains("id=\"criati-convite-senha\"")
				.contains("id=\"criati-convite-confirmacao\"")
				.contains("autocomplete=\"name\"")
				.contains("autocomplete=\"new-password\"")
				.contains("aria-live=\"polite\"")
				.contains("aria-pressed")
				.contains("criati-convite-enviar")
				.contains("Ir para o login")
				.contains("/js/criati-convite-aceite.js")
				.contains("/css/criati-convite.css");
	}

	@Test
	void paginaNaoExpoeDadosSensiveis() throws Exception {
		String corpo = mockMvc.perform(get("/convites/{token}", "token-sensivel"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo)
				.doesNotContain("tokenHash")
				.doesNotContain("token_hash")
				.doesNotContain("conviteId")
				.doesNotContain("empresaId")
				.doesNotContain(SENHA_VALIDA)
				.doesNotContain("$2a$")
				.doesNotContain("$2b$");
	}

	@Test
	void recursosCssEJsDaPaginaSaoAcessiveis() throws Exception {
		mockMvc.perform(get("/css/criati-convite.css")).andExpect(status().isOk());
		mockMvc.perform(get("/js/criati-convite-aceite.js")).andExpect(status().isOk());
	}

	@Test
	void apiPublicaDeConviteContinuaAcessivelAPartirDoFluxoDaPagina() throws Exception {
		Empresa empresa = criarEmpresa("21212121000161", "Empresa Pagina Publica Ltda");
		ConviteCriado criado = criarConvite(empresa, "pagina.publica@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(get("/api/convites/{token}", criado.tokenBruto()))
				.andExpect(status().isOk());
	}

	@Test
	void aceiteMantemIsencaoDeCsrfJaExistenteSemCriarSessao() throws Exception {
		Empresa empresa = criarEmpresa("31313131000162", "Empresa Aceite Sem Csrf Ltda");
		ConviteCriado criado = criarConvite(empresa, "aceite.sem.csrf@criati.test", PerfilUsuario.USUARIO);

		MvcResult resultado = mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Aceite Sem Csrf",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void tokenInvalidoNaoCriaSessaoAoTentarAceitar() throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/convites/{token}/aceitar", "token-invalido-qualquer")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Fulano",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isNotFound())
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void sucessoNaoAutenticaUsuarioAutomaticamente() throws Exception {
		Empresa empresa = criarEmpresa("41414141000163", "Empresa Nao Autentica Ltda");
		ConviteCriado criado = criarConvite(empresa, "nao.autentica@criati.test", PerfilUsuario.USUARIO);

		mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Nao Autentica",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA)))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void nenhumaOutraRotaFoiLiberadaPelaMudanca() throws Exception {
		mockMvc.perform(get("/app/dashboard"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/api/contexto/usuarios"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginComMotivoConviteAceitoMostraMensagemCorreta() throws Exception {
		mockMvc.perform(get("/login").param("motivo", "convite-aceito"))
				.andExpect(status().isOk());
		// A mensagem e escrita via JavaScript (textContent) a partir de um mapa
		// fixo de motivos conhecidos; o teste de conteudo do JS confirma que o
		// mapa contem essa chave, e o teste abaixo confirma que motivos
		// desconhecidos nunca sao refletidos no HTML renderizado pelo servidor.
		assertThat(lerRecursoEstatico("static/js/criati-auth.js")).contains("convite-aceito");
	}

	@Test
	void motivoDesconhecidoNaoInjetaConteudoNaPagina() throws Exception {
		String corpo = mockMvc.perform(get("/login").param("motivo", "<script>alert(1)</script>"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(corpo).doesNotContain("<script>alert(1)</script>");
	}

	private String lerRecursoEstatico(String caminhoClasspath) throws Exception {
		var recurso = new org.springframework.core.io.ClassPathResource(caminhoClasspath);
		return java.nio.file.Files.readString(recurso.getFile().toPath(), java.nio.charset.StandardCharsets.UTF_8);
	}

	private ConviteCriado criarConvite(Empresa empresa, String email, PerfilUsuario perfil) {
		Usuario administrador = criarUsuario("administrador." + UUID.randomUUID() + "@criati.test");
		UsuarioEmpresa vinculo = new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				administrador.getId(), empresa.getId(), vinculo.getId(), PerfilUsuario.ADMINISTRADOR);
		return conviteService.criar(email, perfil, contexto);
	}

	private Empresa criarEmpresa(String cnpj, String nome) {
		return empresaRepository.saveAndFlush(new Empresa(nome, nome, cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(
				new Usuario("Usuario Teste", email, "$2a$12$hashDeSenhaDeTesteQualquerValorAqui", StatusCadastro.ATIVO));
	}
}
