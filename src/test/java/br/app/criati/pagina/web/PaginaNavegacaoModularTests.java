package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
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
class PaginaNavegacaoModularTests {

	private static final String SENHA = "senha-correta-123456";
	private static final AtomicInteger SEQUENCIA_CNPJ = new AtomicInteger(70_000_000);

	@Autowired private MockMvc mockMvc;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void umaEmpresaEUmModuloSelecionaContextoEAbreModuloDiretamente() throws Exception {
		Usuario usuario = criarUsuario("modular.unico@criati.test");
		Empresa empresa = vincular(usuario, PerfilUsuario.USUARIO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		MockHttpSession sessao = login(usuario.getEmail());

		mockMvc.perform(get("/app/dashboard").session(sessao))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/financeiro"));
		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessao))
				.andExpect(status().isOk());
	}

	@Test
	void umaEmpresaEZeroOuVariosModulosUsaPanoramaSemLoop() throws Exception {
		Usuario semModulo = criarUsuario("modular.zero@criati.test");
		vincular(semModulo, PerfilUsuario.USUARIO);
		MockHttpSession sessaoSemModulo = login(semModulo.getEmail());
		mockMvc.perform(get("/app/dashboard").session(sessaoSemModulo))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		String vazio = mockMvc.perform(get("/app/aplicacoes").session(sessaoSemModulo))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(vazio).contains("criati-aplicacoes-vazio").contains("Nenhum módulo disponível");

		Usuario varios = criarUsuario("modular.varios@criati.test");
		Empresa empresa = vincular(varios, PerfilUsuario.USUARIO);
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		MockHttpSession sessaoVarios = login(varios.getEmail());
		mockMvc.perform(get("/app/dashboard").session(sessaoVarios))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/aplicacoes").session(sessaoVarios)).andExpect(status().isOk());
	}

	@Test
	void variasEmpresasMantemDashboardEAplicacoesSemContextoVoltaParaSelecao() throws Exception {
		Usuario usuario = criarUsuario("modular.empresas@criati.test");
		vincular(usuario, PerfilUsuario.ADMINISTRADOR);
		vincular(usuario, PerfilUsuario.USUARIO);
		MockHttpSession sessao = login(usuario.getEmail());

		mockMvc.perform(get("/app/dashboard").session(sessao)).andExpect(status().isOk());
		mockMvc.perform(get("/app/aplicacoes").session(sessao))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/dashboard"));
	}

	@Test
	void moduloInativoNaoViraDestinoInicial() throws Exception {
		Usuario usuario = criarUsuario("modular.inativo@criati.test");
		Empresa empresa = vincular(usuario, PerfilUsuario.USUARIO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		aplicacaoService.desabilitar(empresa.getId(), "FINANCEIRO");

		mockMvc.perform(get("/app/dashboard").session(login(usuario.getEmail())))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void empresaInativaNaoESelecionadaComoEmpresaUnica() throws Exception {
		Usuario usuario = criarUsuario("modular.empresa.inativa@criati.test");
		Empresa empresa = vincular(usuario, PerfilUsuario.USUARIO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);

		MockHttpSession sessao = login(usuario.getEmail());
		mockMvc.perform(get("/app/dashboard").session(sessao)).andExpect(status().isOk());
		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessao))
				.andExpect(status().isNoContent());
	}

	@Test
	void sidebarMostraSomenteModulosHabilitadosEAdministracaoPermitida() throws Exception {
		Usuario usuario = criarUsuario("modular.sidebar@criati.test");
		Empresa empresa = vincular(usuario, PerfilUsuario.USUARIO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");
		MockHttpSession sessao = login(usuario.getEmail());
		mockMvc.perform(get("/app/dashboard").session(sessao)).andExpect(status().is3xxRedirection());

		String corpo = mockMvc.perform(get("/app/aplicacoes").session(sessao))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(corpo)
				.contains("href=\"/app/financeiro\"")
				.contains("href=\"/app/clinica\"")
				.doesNotContain("tarefas-processos")
				.doesNotContain("href=\"/app/usuarios\"")
				.doesNotContain("href=\"/app/convites\"");
	}

	@Test
	void sidebarMostraAdministracaoSomenteAoAdministradorDaEmpresa() throws Exception {
		Usuario administrador = criarUsuario("modular.admin.sidebar@criati.test");
		vincular(administrador, PerfilUsuario.ADMINISTRADOR);
		MockHttpSession sessao = login(administrador.getEmail());
		mockMvc.perform(get("/app/dashboard").session(sessao)).andExpect(status().is3xxRedirection());

		String corpo = mockMvc.perform(get("/app/aplicacoes").session(sessao))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(corpo)
				.contains("href=\"/app/usuarios\"")
				.contains("href=\"/app/convites\"");
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(new Usuario(
				"Usuario Modular", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
	}

	private Empresa vincular(Usuario usuario, PerfilUsuario perfil) {
		String cnpj = String.format("%014d", SEQUENCIA_CNPJ.incrementAndGet());
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Modular Ltda", "Empresa Modular", cnpj, StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		return empresa;
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk()).andReturn();
		return (MockHttpSession) resultado.getRequest().getSession(false);
	}
}
