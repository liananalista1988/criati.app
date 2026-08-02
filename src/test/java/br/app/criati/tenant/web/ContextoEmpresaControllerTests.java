package br.app.criati.tenant.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.util.ReflectionTestUtils;
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
class ContextoEmpresaControllerTests {

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
	void deveListarSomenteVinculosAtivos() throws Exception {
		Usuario usuario = criarUsuario("lista@criati.test");
		Empresa empresaAtiva = criarEmpresa("11111111000101");
		Empresa empresaOndeVinculoEstaInativo = criarEmpresa("22222222000102");
		Empresa empresaInativa = criarEmpresa("33333333000103");
		criarVinculo(usuario, empresaAtiva, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);
		criarVinculo(usuario, empresaOndeVinculoEstaInativo, PerfilUsuario.USUARIO, StatusCadastro.INATIVO);
		criarVinculo(usuario, empresaInativa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		desativarEmpresa(empresaInativa);

		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/api/contexto/empresas").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].empresaId").value(empresaAtiva.getId().toString()))
				.andExpect(jsonPath("$[0].perfil").value("GESTOR"));
	}

	@Test
	void deveSelecionarEmpresaAtivaComVinculoValido() throws Exception {
		Usuario usuario = criarUsuario("selecao.valida@criati.test");
		Empresa empresa = criarEmpresa("11111111000201");
		criarVinculo(usuario, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());

		selecionarEmpresa(session, empresa.getId())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresa.getId().toString()))
				.andExpect(jsonPath("$.perfil").value("ADMINISTRADOR"));
	}

	@Test
	void deveConsultarContextoAtivoAposSelecao() throws Exception {
		Usuario usuario = criarUsuario("consulta@criati.test");
		Empresa empresa = criarEmpresa("11111111000301");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresa.getId().toString()));
	}

	@Test
	void deveRetornar204QuandoNaoHaContextoSelecionado() throws Exception {
		Usuario usuario = criarUsuario("sem.contexto@criati.test");
		MockHttpSession session = login(usuario.getEmail());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isNoContent());
	}

	@Test
	void deveLimparContextoERetornar204() throws Exception {
		Usuario usuario = criarUsuario("limpeza@criati.test");
		Empresa empresa = criarEmpresa("11111111000401");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/empresa-ativa").session(session).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isNoContent());
	}

	@Test
	void naoDeveSelecionarEmpresaSemVinculo() throws Exception {
		Usuario usuario = criarUsuario("sem.vinculo@criati.test");
		Empresa empresaSemVinculo = criarEmpresa("11111111000501");

		MockHttpSession session = login(usuario.getEmail());

		selecionarEmpresa(session, empresaSemVinculo.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void naoDeveSelecionarEmpresaComVinculoInativo() throws Exception {
		Usuario usuario = criarUsuario("vinculo.inativo@criati.test");
		Empresa empresa = criarEmpresa("11111111000601");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.INATIVO);

		MockHttpSession session = login(usuario.getEmail());

		selecionarEmpresa(session, empresa.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void naoDeveSelecionarEmpresaInativa() throws Exception {
		Usuario usuario = criarUsuario("empresa.inativa@criati.test");
		Empresa empresa = criarEmpresa("11111111000701");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		desativarEmpresa(empresa);

		MockHttpSession session = login(usuario.getEmail());

		selecionarEmpresa(session, empresa.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void usuarioDaEmpresaANaoSelecionaEmpresaB() throws Exception {
		Usuario usuario = criarUsuario("empresa.a@criati.test");
		Empresa empresaA = criarEmpresa("11111111000801");
		Empresa empresaB = criarEmpresa("22222222000802");
		criarVinculo(usuario, empresaA, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());

		selecionarEmpresa(session, empresaB.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void administradorDaEmpresaANaoCriaVinculoNaEmpresaB() throws Exception {
		Usuario administrador = criarUsuario("admin.a@criati.test");
		Empresa empresaA = criarEmpresa("11111111000901");
		Empresa empresaB = criarEmpresa("22222222000902");
		criarVinculo(administrador, empresaA, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.b@criati.test");

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaA.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/usuarios-empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "usuarioId": "%s",
						  "empresaId": "%s",
						  "perfil": "USUARIO"
						}
						""".formatted(alvo.getId(), empresaB.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		assertThat(usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(alvo.getId(), empresaB.getId())).isFalse();
	}

	@Test
	void empresaIdArbitrarioNoCorpoNaoSubstituiContextoDaSessao() throws Exception {
		Usuario administrador = criarUsuario("admin.contexto@criati.test");
		Empresa empresaContexto = criarEmpresa("11111111001001");
		Empresa outraEmpresa = criarEmpresa("22222222001002");
		criarVinculo(administrador, empresaContexto, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.contexto@criati.test");

		MockHttpSession session = login(administrador.getEmail());
		selecionarEmpresa(session, empresaContexto.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/usuarios-empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "usuarioId": "%s",
						  "empresaId": "%s",
						  "perfil": "USUARIO"
						}
						""".formatted(alvo.getId(), outraEmpresa.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		assertThat(usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(alvo.getId(), outraEmpresa.getId()))
				.isFalse();
	}

	@Test
	void limparContextoNaoEncerraAutenticacao() throws Exception {
		Usuario usuario = criarUsuario("preserva.auth@criati.test");
		Empresa empresa = criarEmpresa("11111111001101");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(delete("/api/contexto/empresa-ativa").session(session).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("preserva.auth@criati.test"));
	}

	@Test
	void logoutEncerraAutenticacaoEEliminaContexto() throws Exception {
		Usuario usuario = criarUsuario("logout.contexto@criati.test");
		Empresa empresa = criarEmpresa("11111111001201");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void duasSessoesDoMesmoUsuarioPodemTerEmpresasAtivasDiferentesSemInterferencia() throws Exception {
		Usuario usuario = criarUsuario("multi.sessao@criati.test");
		Empresa empresaX = criarEmpresa("11111111001301");
		Empresa empresaY = criarEmpresa("22222222001302");
		criarVinculo(usuario, empresaX, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		criarVinculo(usuario, empresaY, PerfilUsuario.GESTOR, StatusCadastro.ATIVO);

		MockHttpSession sessaoUm = login(usuario.getEmail());
		MockHttpSession sessaoDois = login(usuario.getEmail());

		selecionarEmpresa(sessaoUm, empresaX.getId()).andExpect(status().isOk());
		selecionarEmpresa(sessaoDois, empresaY.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessaoUm))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresaX.getId().toString()));
		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessaoDois))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresaY.getId().toString()));
	}

	@Test
	void doisUsuariosDistintosNaoCompartilhamContexto() throws Exception {
		Usuario usuarioUm = criarUsuario("usuario.um@criati.test");
		Usuario usuarioDois = criarUsuario("usuario.dois@criati.test");
		Empresa empresaUm = criarEmpresa("11111111001401");
		Empresa empresaDois = criarEmpresa("22222222001402");
		criarVinculo(usuarioUm, empresaUm, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		criarVinculo(usuarioDois, empresaDois, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession sessaoUm = login(usuarioUm.getEmail());
		MockHttpSession sessaoDois = login(usuarioDois.getEmail());

		selecionarEmpresa(sessaoUm, empresaUm.getId()).andExpect(status().isOk());
		selecionarEmpresa(sessaoDois, empresaDois.getId()).andExpect(status().isOk());

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessaoUm))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresaUm.getId().toString()));
		mockMvc.perform(get("/api/contexto/empresa-ativa").session(sessaoDois))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.empresaId").value(empresaDois.getId().toString()));
	}

	@Test
	void vinculoDesativadoAposSelecaoInvalidaContextoNoProximoUso() throws Exception {
		Usuario usuario = criarUsuario("vinculo.revalidado@criati.test");
		Empresa empresa = criarEmpresa("11111111001501");
		UsuarioEmpresa vinculo = criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		desativarVinculo(vinculo);

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isNoContent());
	}

	@Test
	void empresaDesativadaAposSelecaoInvalidaContextoNoProximoUso() throws Exception {
		Usuario usuario = criarUsuario("empresa.revalidada@criati.test");
		Empresa empresa = criarEmpresa("11111111001601");
		criarVinculo(usuario, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO);

		MockHttpSession session = login(usuario.getEmail());
		selecionarEmpresa(session, empresa.getId()).andExpect(status().isOk());

		desativarEmpresa(empresa);

		mockMvc.perform(get("/api/contexto/empresa-ativa").session(session))
				.andExpect(status().isNoContent());
	}

	@Test
	void usuarioSemEmpresaAtivaRecebeErroSeguroAoVincular() throws Exception {
		Usuario administrador = criarUsuario("sem.empresa.ativa@criati.test");
		Usuario alvo = criarUsuario("alvo.sem.contexto@criati.test");
		Empresa empresa = criarEmpresa("11111111001701");

		MockHttpSession session = login(administrador.getEmail());

		mockMvc.perform(post("/api/usuarios-empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "usuarioId": "%s",
						  "empresaId": "%s",
						  "perfil": "USUARIO"
						}
						""".formatted(alvo.getId(), empresa.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void respostas403NaoRevelamSeEmpresaAlvoExiste() throws Exception {
		Usuario usuario = criarUsuario("nao.revela@criati.test");
		MockHttpSession session = login(usuario.getEmail());
		Empresa empresaSemVinculo = criarEmpresa("11111111001801");

		// empresaId totalmente inexistente e empresaId de uma empresa real (mas
		// sem vinculo do usuario) devem produzir a mesma resposta generica, sem
		// distincao que permita inferir se a empresa existe.
		selecionarEmpresa(session, UUID.randomUUID())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		selecionarEmpresa(session, empresaSemVinculo.getId())
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
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

	private org.springframework.test.web.servlet.ResultActions selecionarEmpresa(
			MockHttpSession session, UUID empresaId) throws Exception {
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

	private void desativarEmpresa(Empresa empresa) {
		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);
	}

	private void desativarVinculo(UsuarioEmpresa vinculo) {
		ReflectionTestUtils.setField(vinculo, "status", StatusCadastro.INATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private UsuarioEmpresa criarVinculo(
			Usuario usuario, Empresa empresa, PerfilUsuario perfil, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, status);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}
}
