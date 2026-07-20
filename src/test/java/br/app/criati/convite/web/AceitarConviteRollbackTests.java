package br.app.criati.convite.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import br.app.criati.acesso.model.UsuarioEmpresa;
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

// Contexto Spring dedicado: o @MockitoSpyBean sobre UsuarioEmpresaRepository
// forca um ApplicationContext proprio. Nenhuma regra de producao e alterada -
// o spy delega ao real em tudo, exceto o save() do vinculo, forcado a falhar
// dentro da MESMA transacao real de AceitarConviteService. Sem @Transactional
// nesta classe, de proposito, para que a falha provoque rollback FISICO
// imediato e visivel de fora (mesmo raciocinio de AdminEmpresaRollbackVinculoTests).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AceitarConviteRollbackTests {

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

	@MockitoSpyBean
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveDesfazerUsuarioEConviteQuandoCriacaoDoVinculoFalhar() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Rollback Convite Ltda", "Empresa Rollback Convite", "14141414000152",
						StatusCadastro.ATIVO));
		Usuario administrador = usuarioRepository.saveAndFlush(
				new Usuario("Administrador", "administrador.rollback.convite@criati.test",
						passwordEncoder.encode(SENHA_VALIDA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.save(
				new UsuarioEmpresa(administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				administrador.getId(), empresa.getId(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		ConviteCriado criado = conviteService.criar("rollback.aceite@criati.test", PerfilUsuario.USUARIO, contexto);

		doThrow(new RuntimeException("Falha simulada na criacao do vinculo"))
				.when(usuarioEmpresaRepository).save(argThat(
						vinculo -> vinculo != null && "rollback.aceite@criati.test".equals(vinculo.getUsuario().getEmail())));

		assertThatThrownBy(() -> mockMvc.perform(post("/api/convites/{token}/aceitar", criado.tokenBruto())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "nome": "Sera Revertido",
						  "senha": "%s",
						  "confirmacaoSenha": "%s"
						}
						""".formatted(SENHA_VALIDA, SENHA_VALIDA))))
				.hasRootCauseMessage("Falha simulada na criacao do vinculo");

		assertThat(usuarioRepository.existsByEmailIgnoreCase("rollback.aceite@criati.test")).isFalse();
		Convite conviteInalterado = conviteRepository.findById(criado.convite().getId()).orElseThrow();
		assertThat(conviteInalterado.getStatus()).isEqualTo(StatusConvite.PENDENTE);
		assertThat(conviteInalterado.getUtilizadoEm()).isNull();
	}
}
