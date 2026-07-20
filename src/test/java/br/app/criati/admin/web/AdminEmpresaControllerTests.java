package br.app.criati.admin.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
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
class AdminEmpresaControllerTests {

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
	void deveCriarEmpresaComAdministradorInicialERetornar201() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.criar@criati.test");

		mockMvc.perform(post("/api/admin/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": {
						    "nome": "Empresa Inicial Ltda",
						    "nomeFantasia": "Empresa Inicial",
						    "cnpj": "11111111000191"
						  },
						  "administrador": {
						    "nome": "Administrador Inicial",
						    "email": "administrador.inicial@criati.test",
						    "senha": "senha-do-administrador"
						  }
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.empresa.cnpj").value("11111111000191"))
				.andExpect(jsonPath("$.empresa.status").value("ATIVO"))
				.andExpect(jsonPath("$.administrador.email").value("administrador.inicial@criati.test"))
				.andExpect(jsonPath("$.administrador.senha").doesNotExist())
				.andExpect(jsonPath("$.vinculo.perfil").value("ADMINISTRADOR"))
				.andExpect(jsonPath("$.vinculo.status").value("ATIVO"));
	}

	@Test
	void naoDeveConfiarNoPerfilEnviadoPeloClienteSeExistisse() throws Exception {
		// O DTO nao possui campo de perfil: o vinculo criado deve ser sempre
		// ADMINISTRADOR, fixado pelo backend (ja comprovado no teste anterior
		// via jsonPath "$.vinculo.perfil"). Este teste apenas documenta a
		// invariante de forma explicita, reforcando a regra de negocio.
		MockHttpSession session = loginSuperAdministrador("superadmin.perfil@criati.test");

		MvcResult resultado = mockMvc.perform(post("/api/admin/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": {
						    "nome": "Empresa Perfil Ltda",
						    "cnpj": "22222222000192"
						  },
						  "administrador": {
						    "nome": "Administrador Perfil",
						    "email": "administrador.perfil@criati.test",
						    "senha": "senha-do-administrador"
						  }
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();

		assertThat(resultado.getResponse().getContentAsString()).contains("\"perfil\":\"ADMINISTRADOR\"");
	}

	// Propagation.NOT_SUPPORTED nesta unica classe: a classe usa @Transactional
	// para rollback automatico entre testes, mas isso faria a chamada HTTP
	// participar da MESMA transacao ja aberta pelo teste. Como o servico
	// tambem e @Transactional (padrao, sem propagation especial - igual aos
	// demais servicos do projeto), ele so consegue fazer rollback FISICO
	// imediato se for o dono da transacao. Sem essa anotacao, a escrita da
	// empresa ficaria visivel para a consulta de verificacao abaixo (mesma
	// transacao ainda aberta, "le sua propria escrita"), mesmo apos a
	// operacao falhar. Usar REQUIRES_NEW no servico foi tentado e descartado:
	// no H2 de teste isso abre uma segunda conexao/transacao fisica que
	// disputa lock de escrita com a transacao do teste (ainda aberta e com
	// escritas pendentes na tabela usuario), causando falha deterministica de
	// "Concurrent update" / timeout de lock - nao e flakiness, e um
	// deadlock estrutural entre as duas transacoes. Rodar este teste sem a
	// transacao do teste (cada chamada de repositorio/HTTP comita por conta
	// propria) reproduz fielmente o comportamento real de producao, onde nao
	// ha transacao ambiente nenhuma envolvendo o servico.
	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void deveFazerRollbackQuandoEmailDoAdministradorJaEstaEmUso() throws Exception {
		Usuario existente = new Usuario(
				"Usuario Existente", "ja.existe@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		usuarioRepository.saveAndFlush(existente);
		MockHttpSession session = loginSuperAdministrador("superadmin.rollback@criati.test");

		mockMvc.perform(post("/api/admin/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": {
						    "nome": "Empresa Sera Revertida Ltda",
						    "cnpj": "33333333000193"
						  },
						  "administrador": {
						    "nome": "Administrador Duplicado",
						    "email": "ja.existe@criati.test",
						    "senha": "senha-do-administrador"
						  }
						}
						"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("E-mail ja cadastrado"));

		assertThat(empresaRepository.existsByCnpj("33333333000193")).isFalse();
	}

	@Test
	void deveRetornar409ParaCnpjDuplicadoNaCriacaoInicial() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.cnpj@criati.test");
		String corpo = """
				{
				  "empresa": {
				    "nome": "Empresa Cnpj Duplicado Ltda",
				    "cnpj": "44444444000194"
				  },
				  "administrador": {
				    "nome": "Administrador Um",
				    "email": "administrador.um@criati.test",
				    "senha": "senha-do-administrador"
				  }
				}
				""";
		mockMvc.perform(post("/api/admin/empresas").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated());

		String corpoRepetido = """
				{
				  "empresa": {
				    "nome": "Empresa Cnpj Duplicado Ltda",
				    "cnpj": "44444444000194"
				  },
				  "administrador": {
				    "nome": "Administrador Dois",
				    "email": "administrador.dois@criati.test",
				    "senha": "senha-do-administrador"
				  }
				}
				""";
		mockMvc.perform(post("/api/admin/empresas").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(corpoRepetido))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("CNPJ ja cadastrado"));

		assertThat(usuarioRepository.existsByEmailIgnoreCase("administrador.dois@criati.test")).isFalse();
	}

	@Test
	void deveRetornar400ParaEntradaInvalidaNaCriacaoInicial() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.400.admin@criati.test");

		mockMvc.perform(post("/api/admin/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": {
						    "nome": "",
						    "cnpj": ""
						  },
						  "administrador": {
						    "nome": "",
						    "email": "email-invalido",
						    "senha": ""
						  }
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors['empresa.nome']").value("Nome e obrigatorio"))
				.andExpect(jsonPath("$.fieldErrors['empresa.cnpj']").value("CNPJ e obrigatorio"))
				.andExpect(jsonPath("$.fieldErrors['administrador.email']").value("E-mail deve ser valido"));

		assertThat(empresaRepository.findAll()).isEmpty();
	}

	@Test
	void deveRetornar403ParaAdministradorEmpresarialComVinculoEContextoAtivos() throws Exception {
		// ADMINISTRADOR empresarial (com vinculo ATIVO e contexto de empresa
		// selecionado) nao e Superadministrador: ROLE_SUPERADMIN nunca deriva de
		// perfil empresarial (UsuarioEmpresa.perfil nao alimenta GrantedAuthority),
		// entao mesmo com contexto valido a rota global deve permanecer negada.
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa do Administrador Ltda", "Empresa do Administrador", "55555555000195",
						StatusCadastro.ATIVO));
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Administrador Empresarial", "administrador.empresarial@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));

		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "administrador.empresarial@criati.test",
						  "senha": "%s"
						}
						""".formatted(SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresa.getId())))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/admin/empresas").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
	}

	@Test
	void deveListarEmpresasQuandoSuperAdministrador() throws Exception {
		MockHttpSession session = loginSuperAdministrador("superadmin.listar@criati.test");

		mockMvc.perform(get("/api/admin/empresas").session(session))
				.andExpect(status().isOk());
	}

	@Test
	void deveRetornar401ParaRequisicaoAnonima() throws Exception {
		mockMvc.perform(get("/api/admin/empresas"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveRetornar403ParaUsuarioComumAutenticado() throws Exception {
		MockHttpSession session = login("usuario.comum.admin@criati.test", false);

		mockMvc.perform(get("/api/admin/empresas").session(session))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Acesso negado"));
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
