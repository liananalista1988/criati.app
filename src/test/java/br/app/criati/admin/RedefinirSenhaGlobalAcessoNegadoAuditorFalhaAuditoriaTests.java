package br.app.criati.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;
import br.app.criati.admin.repository.RedefinicaoSenhaGlobalAuditoriaRepository;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// @MockitoSpyBean sobre RedefinicaoSenhaGlobalAuditoriaService forca um
// ApplicationContext proprio (mesmo raciocinio ja usado em
// RedefinirSenhaRollbackTests): simula uma falha tecnica (ex.: banco
// indisponivel) na escrita da auditoria de SEM_PERMISSAO dentro de
// RedefinirSenhaGlobalAcessoNegadoAuditor, provando que essa falha nunca
// degrada a resposta de seguranca real - o 403 do AccessDeniedException
// original segue intacto, sempre via JsonAccessDeniedHandler.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RedefinirSenhaGlobalAcessoNegadoAuditorFalhaAuditoriaTests {

	private static final String SENHA = "senha-correta-com-quinze-mais";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RedefinicaoSenhaGlobalAuditoriaRepository redefinicaoSenhaGlobalAuditoriaRepository;

	@MockitoSpyBean
	private RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService;

	@Test
	void falhaAoRegistrarAuditoriaNaoAlteraResposta403DeAcessoNegado() throws Exception {
		Usuario usuarioComum = usuarioRepository.saveAndFlush(new Usuario("Usuario Comum Falha Auditoria",
				"usuario.comum.falha.auditoria@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		Usuario alvo = usuarioRepository.saveAndFlush(new Usuario("Alvo Falha Auditoria",
				"alvo.falha.auditoria@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		String hashOriginalAlvo = alvo.getSenha();
		MockHttpSession session = login(usuarioComum.getEmail());

		doThrow(new RuntimeException("Falha simulada ao registrar auditoria de acesso negado"))
				.when(auditoriaFalhaService).registrarFalha(any(), any(), any(), any(), any());

		// Resposta 403 real (AccessDeniedException do SecurityConfig, por falta
		// de ROLE_SUPERADMIN), gerada pelo JsonAccessDeniedHandler - nunca
		// substituida por um 500 ou por qualquer erro vazando da falha simulada
		// na escrita da auditoria.
		mockMvc.perform(post("/api/admin/usuarios/" + alvo.getId() + "/redefinir-senha")
				.session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "novaSenha": "senha-nova-com-quinze-mais", "confirmacaoSenha": "senha-nova-com-quinze-mais" }
						"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.message").value("Acesso negado"));

		// Acesso realmente negado: senha do alvo intacta.
		Usuario alvoAtual = usuarioRepository.findById(alvo.getId()).orElseThrow();
		assertThat(alvoAtual.getSenha()).isEqualTo(hashOriginalAlvo);

		// A falha simulada impede a gravacao (nenhum registro parcial/duplicado).
		List<RedefinicaoSenhaGlobalAuditoria> eventos =
				redefinicaoSenhaGlobalAuditoriaRepository.findAllByUsuarioAlvoIdOrderByCriadoEmDesc(alvo.getId());
		assertThat(eventos).isEmpty();
	}

	private MockHttpSession login(String email) throws Exception {
		MvcResult resultado = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) resultado.getRequest().getSession(false);
	}
}
