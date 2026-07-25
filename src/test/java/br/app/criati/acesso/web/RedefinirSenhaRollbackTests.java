package br.app.criati.acesso.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.RedefinicaoSenhaAuditoriaRepository;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// Contexto Spring dedicado: o @MockitoSpyBean sobre RedefinicaoSenhaAuditoriaRepository
// forca um ApplicationContext proprio. Nenhuma regra de producao e alterada - o spy
// delega ao real em tudo, exceto o save() da auditoria, forcado a falhar dentro da
// MESMA transacao real de GerenciarUsuarioEmpresaService.redefinirSenha. Sem
// @Transactional nesta classe, de proposito, para que a falha provoque rollback
// FISICO imediato e visivel de fora (mesmo raciocinio de AceitarConviteRollbackTests
// e AdminEmpresaRollbackVinculoTests) - diferente dos testes unitarios/Mockito de
// GerenciarUsuarioEmpresaServiceTests, que so provam que a excecao nao e engolida,
// mas nao conseguem observar rollback fisico porque o teste ali roda dentro da
// mesma transacao (nao ha commit real a desfazer).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedefinirSenhaRollbackTests {

	private static final String SENHA_ORIGINAL = "senha-original-com-quinze-mais";
	private static final String SENHA_NOVA = "senha-nova-com-quinze-mais-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoSpyBean
	private RedefinicaoSenhaAuditoriaRepository redefinicaoSenhaAuditoriaRepository;

	@Test
	void falhaAoGravarAuditoriaDesfazATrocaDeSenhaEDeixaAuditoriaVazia() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Rollback Senha Ltda", "Empresa Rollback Senha", "15151515000163",
						StatusCadastro.ATIVO));
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Administrador Rollback", "administrador.rollback.senha@criati.test",
						passwordEncoder.encode(SENHA_ORIGINAL), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		Usuario alvo = usuarioRepository.saveAndFlush(
				new Usuario("Alvo Rollback", "alvo.rollback.senha@criati.test",
						passwordEncoder.encode(SENHA_ORIGINAL), StatusCadastro.ATIVO));
		UsuarioEmpresa vinculoAlvo = usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(alvo, empresa, PerfilUsuario.USUARIO, StatusCadastro.ATIVO));
		String hashOriginal = alvo.getSenha();

		MockHttpSession session = login(administrador.getEmail(), SENHA_ORIGINAL);
		mockMvc.perform(post("/api/contexto/empresa-ativa").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresa.getId())))
				.andExpect(status().isOk());

		doThrow(new RuntimeException("Falha simulada ao gravar auditoria de redefinicao de senha"))
				.when(redefinicaoSenhaAuditoriaRepository).save(any());

		assertThatThrownBy(() -> mockMvc.perform(
				post("/api/contexto/usuarios/" + vinculoAlvo.getId() + "/redefinir-senha")
						.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "novaSenha": "%s", "confirmacaoSenha": "%s" }
								""".formatted(SENHA_NOVA, SENHA_NOVA))))
				.hasRootCauseMessage("Falha simulada ao gravar auditoria de redefinicao de senha");

		// Recarrega o usuario numa transacao/consulta nova (nao a mesma da
		// chamada que falhou): prova que o rollback foi fisico, nao apenas
		// que a excecao nao foi engolida em memoria.
		Usuario alvoRecarregado = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoRecarregado.getSenha()).isEqualTo(hashOriginal);
		assertThat(passwordEncoder.matches(SENHA_ORIGINAL, alvoRecarregado.getSenha())).isTrue();
		assertThat(passwordEncoder.matches(SENHA_NOVA, alvoRecarregado.getSenha())).isFalse();

		assertThat(redefinicaoSenhaAuditoriaRepository
				.findAllByEmpresaIdAndUsuarioAfetadoIdOrderByCriadoEmDesc(empresa.getId(), alvo.getId()))
				.isEmpty();
	}

	private MockHttpSession login(String email, String senha) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, senha)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) resultado.getRequest().getSession(false);
	}
}
