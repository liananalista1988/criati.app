package br.app.criati.admin.web;

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
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// Contexto Spring dedicado: o @MockitoSpyBean sobre UsuarioEmpresaRepository
// forca um ApplicationContext proprio, isolado dos demais testes. Nenhuma
// regra de producao e alterada - o spy delega para a implementacao real do
// Spring Data em tudo, exceto o save() do vinculo, forcado a falhar dentro da
// MESMA transacao real de AdminEmpresaService (@Transactional padrao, dono da
// transacao aqui, sem ambiente externo). Sem @Transactional nesta classe de
// teste, de proposito: a falha deve provocar rollback FISICO imediato,
// visivel de fora, e nao apenas ficar marcada para rollback futuro.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEmpresaRollbackVinculoTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@MockitoSpyBean
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveDesfazerEmpresaEUsuarioQuandoCriacaoDoVinculoFalhar() throws Exception {
		doThrow(new RuntimeException("Falha simulada na criacao do vinculo"))
				.when(usuarioEmpresaRepository).save(any(UsuarioEmpresa.class));

		MockHttpSession session = loginSuperAdministrador("superadmin.rollback.vinculo@criati.test");

		assertThatThrownBy(() -> mockMvc.perform(post("/api/admin/empresas")
				.session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "empresa": {
						    "nome": "Empresa Sera Revertida Por Falha No Vinculo Ltda",
						    "cnpj": "66666666000196"
						  },
						  "administrador": {
						    "nome": "Administrador Sera Revertido",
						    "email": "administrador.sera.revertido@criati.test",
						    "senha": "senha-do-administrador"
						  }
						}
						""")))
				.hasRootCauseMessage("Falha simulada na criacao do vinculo");

		assertThat(empresaRepository.existsByCnpj("66666666000196")).isFalse();
		assertThat(usuarioRepository.existsByEmailIgnoreCase("administrador.sera.revertido@criati.test")).isFalse();
		assertThat(usuarioEmpresaRepository.count()).isZero();
	}

	private MockHttpSession loginSuperAdministrador(String email) throws Exception {
		Usuario usuario = Usuario.criarSuperAdministrador("Usuario Teste", email, passwordEncoder.encode(SENHA));
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
