package br.app.criati.aplicacao.web;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
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
class ContextoAplicacaoControllerTests {

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
	private AplicacaoRepository aplicacaoRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void listaApenasAplicacoesAtivasDaEmpresaAtivaSemVazarOutraEmpresa() throws Exception {
		Usuario usuario = criarUsuario("contexto.lista@criati.test");
		Empresa empresaA = criarEmpresa("11111111000141");
		Empresa empresaB = criarEmpresa("22222222000142");
		criarVinculo(usuario, empresaA, StatusCadastro.ATIVO);
		criarVinculo(usuario, empresaB, StatusCadastro.ATIVO);
		aplicacaoService.habilitar(empresaA.getId(), "FINANCEIRO");
		aplicacaoService.habilitar(empresaB.getId(), "CLINICA");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresaA.getId());

		mockMvc.perform(get("/api/contexto/aplicacoes").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].codigo").value("FINANCEIRO"))
				.andExpect(jsonPath("$[0].urlInicial").value("/app/financeiro"));
	}

	@Test
	void aplicacaoInativaNaoAparecMesmoComVinculoAtivo() throws Exception {
		Usuario usuario = criarUsuario("contexto.aplicacao.inativa@criati.test");
		Empresa empresa = criarEmpresa("33333333000143");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");

		Aplicacao clinica = aplicacaoRepository.findByCodigo("CLINICA").orElseThrow();
		ReflectionTestUtils.setField(clinica, "status", StatusCadastro.INATIVO);
		aplicacaoRepository.saveAndFlush(clinica);
		try {
			MockHttpSession session = login(usuario.getEmail());
			selecionarEmpresa(session, empresa.getId());

			mockMvc.perform(get("/api/contexto/aplicacoes").session(session))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(0));
		} finally {
			ReflectionTestUtils.setField(clinica, "status", StatusCadastro.ATIVO);
			aplicacaoRepository.saveAndFlush(clinica);
		}
	}

	@Test
	void vinculoInativoBloqueiaListagem() throws Exception {
		Usuario usuario = criarUsuario("contexto.vinculo.inativo@criati.test");
		Empresa empresa = criarEmpresa("44444444000144");
		UsuarioEmpresa vinculo = criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		ReflectionTestUtils.setField(vinculo, "status", StatusCadastro.INATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);

		mockMvc.perform(get("/api/contexto/aplicacoes").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void empresaInativaBloqueiaListagem() throws Exception {
		Usuario usuario = criarUsuario("contexto.empresa.inativa@criati.test");
		Empresa empresa = criarEmpresa("55555555000145");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId());

		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);

		mockMvc.perform(get("/api/contexto/aplicacoes").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void semContextoAtivoRetornaErroApropriado() throws Exception {
		Usuario usuario = criarUsuario("contexto.sem.contexto@criati.test");
		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/api/contexto/aplicacoes").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void anonimoRecebe401() throws Exception {
		mockMvc.perform(get("/api/contexto/aplicacoes"))
				.andExpect(status().isUnauthorized());
	}

	private void selecionarEmpresa(MockHttpSession session, java.util.UUID empresaId) throws Exception {
		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
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

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private Empresa criarEmpresa(String cnpj) {
		Empresa empresa = new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO);
		return empresaRepository.saveAndFlush(empresa);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, PerfilUsuario.USUARIO, status);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}
}
