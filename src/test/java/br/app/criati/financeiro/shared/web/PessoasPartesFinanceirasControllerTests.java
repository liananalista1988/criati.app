package br.app.criati.financeiro.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PessoasPartesFinanceirasControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String PESSOAS = "/api/contexto/financeiro/pessoas";
	private static final String CONTATOS = "/api/contexto/financeiro/contatos";

	@Autowired private MockMvc mockMvc;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private PessoaFinanceiraRepository pessoaRepository;
	@Autowired private ParteFinanceiraRepository parteRepository;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;

	@Test
	void criaPessoaSemUsuarioNaEmpresaAtiva() throws Exception {
		Cenario c = cenario("11111111000301");
		mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"  Pessoa Um  \", \"apelido\": \"Titular\" }"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Pessoa Um"))
				.andExpect(jsonPath("$.usuarioId").doesNotExist()).andExpect(jsonPath("$.status").value("ATIVO"));
		assertThat(pessoaRepository.findAllByEmpresaIdOrderByNomeAsc(c.empresa().getId())).hasSize(1);
	}

	@Test
	void nomeDaPessoaEObrigatorio() throws Exception {
		Cenario c = cenario("11111111000302");
		mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"   \" }"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.nome").value("Nome e obrigatorio"));
	}

	@Test
	void vinculaPessoaAUsuarioAtivoDaMesmaEmpresa() throws Exception {
		Cenario c = cenario("11111111000303"); Usuario integrante = criarUsuario("integrante@criati.test");
		criarVinculo(integrante, c.empresa(), PerfilUsuario.USUARIO);
		mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Integrante\", \"usuarioId\": \"%s\" }".formatted(integrante.getId())))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(integrante.getId().toString()))
				.andExpect(jsonPath("$.usuarioEmail").value("integrante@criati.test"));
	}

	@Test
	void rejeitaVinculoComUsuarioDeOutraEmpresa() throws Exception {
		Cenario c = cenario("11111111000304"); Empresa outra = criarEmpresa("22222222000304");
		Usuario alheio = criarUsuario("alheio@criati.test"); criarVinculo(alheio, outra, PerfilUsuario.USUARIO);
		mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Tentativa\", \"usuarioId\": \"%s\" }".formatted(alheio.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejeitaSegundoVinculoAtivoDoMesmoUsuario() throws Exception {
		Cenario c = cenario("11111111000305"); Usuario integrante = criarUsuario("duplicado@criati.test");
		criarVinculo(integrante, c.empresa(), PerfilUsuario.USUARIO);
		pessoaRepository.saveAndFlush(new PessoaFinanceira(c.empresa(), "Primeira", integrante, null, c.admin()));
		mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Segunda\", \"usuarioId\": \"%s\" }".formatted(integrante.getId())))
				.andExpect(status().isConflict());
	}

	@Test
	void atualizaPessoaERemoveVinculo() throws Exception {
		Cenario c = cenario("11111111000306"); PessoaFinanceira pessoa = pessoaRepository.saveAndFlush(new PessoaFinanceira(c.empresa(), "Antes", c.admin(), null, c.admin()));
		mockMvc.perform(put(PESSOAS + "/" + pessoa.getId()).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Depois\", \"apelido\": \"Casa\", \"usuarioId\": null }"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Depois"))
				.andExpect(jsonPath("$.usuarioId").doesNotExist());
	}

	@Test
	void desativaEReativaPessoaPreservandoRegistro() throws Exception {
		Cenario c = cenario("11111111000307"); PessoaFinanceira pessoa = pessoaRepository.saveAndFlush(new PessoaFinanceira(c.empresa(), "Pessoa", null, null, c.admin()));
		mockMvc.perform(post(PESSOAS + "/" + pessoa.getId() + "/inativar").session(c.session()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
		mockMvc.perform(post(PESSOAS + "/" + pessoa.getId() + "/reativar").session(c.session()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
		assertThat(pessoaRepository.findByIdAndEmpresaId(pessoa.getId(), c.empresa().getId())).isPresent();
	}

	@Test
	void pessoaDeOutraEmpresaNaoPodeSerConsultada() throws Exception {
		Cenario c = cenario("11111111000308"); Empresa outra = criarEmpresa("22222222000308");
		PessoaFinanceira alheia = pessoaRepository.saveAndFlush(new PessoaFinanceira(outra, "Alheia", null, null, c.admin()));
		mockMvc.perform(get(PESSOAS + "/" + alheia.getId()).session(c.session())).andExpect(status().isNotFound());
	}

	@Test
	void listagemDePessoasNaoVazaOutroTenant() throws Exception {
		Cenario c = cenario("11111111000309"); Empresa outra = criarEmpresa("22222222000309");
		pessoaRepository.saveAndFlush(new PessoaFinanceira(c.empresa(), "Visivel", null, null, c.admin()));
		pessoaRepository.saveAndFlush(new PessoaFinanceira(outra, "Oculta", null, null, c.admin()));
		mockMvc.perform(get(PESSOAS).session(c.session())).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nome").value("Visivel")).andExpect(jsonPath("$[1]").doesNotExist());
	}

	@Test
	void gestorNaoPodeCriarPessoa() throws Exception {
		Cenario c = cenario("11111111000310", PerfilUsuario.GESTOR);
        mockMvc.perform(post(PESSOAS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": \"Bloqueada\" }"))
				.andExpect(status().isForbidden());
	}

	@Test
	void criaContatoComDocumentoNormalizado() throws Exception {
		Cenario c = cenario("11111111000311");
		mockMvc.perform(post(CONTATOS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \" Mercado \", \"tipo\": \"ESTABELECIMENTO\", \"documento\": \"12.345.678/0001-90\" }"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.nome").value("Mercado"))
				.andExpect(jsonPath("$.documento").value("12345678000190"));
	}

	@Test
	void contatoAceitaDocumentoOpcionalENomesDuplicados() throws Exception {
		Cenario c = cenario("11111111000312");
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(post(CONTATOS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Mesmo nome\", \"tipo\": \"PESSOA\" }")).andExpect(status().isCreated());
		}
		assertThat(parteRepository.findAllByEmpresaIdOrderByNomeAsc(c.empresa().getId())).hasSize(2);
	}

	@Test
	void nomeETipoDoContatoSaoObrigatorios() throws Exception {
		Cenario c = cenario("11111111000313");
        mockMvc.perform(post(CONTATOS).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{ \"nome\": \" \" }"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.nome").exists()).andExpect(jsonPath("$.fieldErrors.tipo").exists());
	}

	@Test
	void atualizaContato() throws Exception {
		Cenario c = cenario("11111111000314"); ParteFinanceira parte = parteRepository.saveAndFlush(new ParteFinanceira(c.empresa(), "Antes", TipoParteFinanceira.PESSOA, null, null, null, c.admin()));
		mockMvc.perform(put(CONTATOS + "/" + parte.getId()).session(c.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{ \"nome\": \"Depois\", \"tipo\": \"ORGANIZACAO\", \"apelido\": \"Fornecedor\" }"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Depois")).andExpect(jsonPath("$.tipo").value("ORGANIZACAO"));
	}

	@Test
	void desativaEReativaContato() throws Exception {
		Cenario c = cenario("11111111000315"); ParteFinanceira parte = parteRepository.saveAndFlush(new ParteFinanceira(c.empresa(), "Contato", TipoParteFinanceira.OUTRA, null, null, null, c.admin()));
		mockMvc.perform(post(CONTATOS + "/" + parte.getId() + "/inativar").session(c.session()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"));
		mockMvc.perform(post(CONTATOS + "/" + parte.getId() + "/reativar").session(c.session()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
	}

	@Test
	void pesquisaContatoPorNomeETipo() throws Exception {
		Cenario c = cenario("11111111000316");
		parteRepository.saveAndFlush(new ParteFinanceira(c.empresa(), "Mercado Central", TipoParteFinanceira.ESTABELECIMENTO, null, null, null, c.admin()));
		parteRepository.saveAndFlush(new ParteFinanceira(c.empresa(), "Mercado Amigo", TipoParteFinanceira.PESSOA, null, null, null, c.admin()));
		mockMvc.perform(get(CONTATOS).param("busca", "mercado").param("tipo", "ESTABELECIMENTO").session(c.session()))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Mercado Central")).andExpect(jsonPath("$[1]").doesNotExist());
	}

	@Test
	void contatoDeOutroTenantNaoPodeSerConsultadoNemListado() throws Exception {
		Cenario c = cenario("11111111000317"); Empresa outra = criarEmpresa("22222222000317");
		ParteFinanceira alheia = parteRepository.saveAndFlush(new ParteFinanceira(outra, "Oculta", TipoParteFinanceira.OUTRA, null, null, null, c.admin()));
		mockMvc.perform(get(CONTATOS + "/" + alheia.getId()).session(c.session())).andExpect(status().isNotFound());
		mockMvc.perform(get(CONTATOS).session(c.session())).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void anonimoNaoAcessaApis() throws Exception {
		mockMvc.perform(get(PESSOAS)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(CONTATOS)).andExpect(status().isUnauthorized());
	}

	private Cenario cenario(String cnpj) throws Exception { return cenario(cnpj, PerfilUsuario.ADMINISTRADOR); }
	private Cenario cenario(String cnpj, PerfilUsuario perfil) throws Exception {
		Empresa empresa = criarEmpresa(cnpj); aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario admin = criarUsuario("usuario." + cnpj + "@criati.test"); criarVinculo(admin, empresa, perfil);
		return new Cenario(empresa, admin, autenticarNaEmpresa(admin.getEmail(), empresa.getId()));
	}
	private Empresa criarEmpresa(String cnpj) { return empresaRepository.saveAndFlush(new Empresa("Empresa Teste", "Empresa Teste", cnpj, StatusCadastro.ATIVO)); }
	private Usuario criarUsuario(String email) { return usuarioRepository.saveAndFlush(new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO)); }
	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) { return usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO)); }
	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ \"email\": \"%s\", \"senha\": \"%s\" }".formatted(email, SENHA))).andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mockMvc.perform(post("/api/contexto/empresa-ativa").session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{ \"empresaId\": \"%s\" }".formatted(empresaId))).andExpect(status().isOk());
		return session;
	}
	private record Cenario(Empresa empresa, Usuario admin, MockHttpSession session) { }
}
