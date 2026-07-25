package br.app.criati.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;
import br.app.criati.admin.repository.RedefinicaoSenhaGlobalAuditoriaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.AutoRedefinicaoSenhaNaoPermitidaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.UsuarioStatusInvalidoException;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.validacao.SenhaValidador;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class RedefinirSenhaGlobalServiceTests {

	private static final String SENHA_NOVA = "senha-nova-com-quinze-mais";

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private SenhaValidador senhaValidador;

	@Mock
	private RedefinicaoSenhaGlobalAuditoriaRepository redefinicaoSenhaGlobalAuditoriaRepository;

	@Mock
	private RedefinicaoSenhaGlobalAuditoriaService auditoriaFalhaService;

	@InjectMocks
	private RedefinirSenhaGlobalService service;

	@Test
	void deveRedefinirSenhaDeUsuarioGlobalAtivoECriarAuditoriaDeSucesso() {
		Usuario superAdmin = criarUsuario("superadmin@criati.test", true, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo@criati.test", false, StatusCadastro.ATIVO);
		when(usuarioRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
		when(passwordEncoder.encode(SENHA_NOVA)).thenReturn("hash-codificado");

		service.redefinirSenha(alvo.getId(), SENHA_NOVA, SENHA_NOVA, superAdmin.getId(), "203.0.113.10");

		verify(senhaValidador).validar(SENHA_NOVA, SENHA_NOVA);
		assertThat(alvo.getSenha()).isEqualTo("hash-codificado");
		verify(usuarioRepository).save(alvo);
		verify(auditoriaFalhaService, never()).registrarFalha(any(), any(), any(), any(), any());

		org.mockito.ArgumentCaptor<RedefinicaoSenhaGlobalAuditoria> captor =
				org.mockito.ArgumentCaptor.forClass(RedefinicaoSenhaGlobalAuditoria.class);
		verify(redefinicaoSenhaGlobalAuditoriaRepository).save(captor.capture());
		RedefinicaoSenhaGlobalAuditoria evento = captor.getValue();
		assertThat(evento.getAdministrador()).isEqualTo(superAdmin);
		assertThat(evento.getUsuarioAlvoId()).isEqualTo(alvo.getId());
		assertThat(evento.getResultado()).isEqualTo(ResultadoAuditoriaSeguranca.SUCESSO);
		assertThat(evento.getMotivo()).isEqualTo(MotivoAuditoriaSeguranca.REDEFINICAO_CONCLUIDA);
		assertThat(evento.getIpOrigem()).isEqualTo("203.0.113.10");
	}

	// SEM_PERMISSAO nunca e alcancavel via HTTP real (SecurityConfig ja exige
	// ROLE_SUPERADMIN em qualquer verbo de /api/admin/**, barrando a
	// requisicao antes do controller) - mas o service tem seu proprio guard
	// redundante (defesa em profundidade, nunca so escondendo o botao na
	// tela), e este teste o exercita diretamente, como prova de que ele
	// existe e audita a tentativa mesmo que a rota HTTP nunca a alcance.
	@Test
	void deveRejeitarEAuditarQuandoChamadorNaoEhSuperAdministrador() {
		Usuario usuarioComum = criarUsuario("comum@criati.test", false, StatusCadastro.ATIVO);
		UUID alvoId = UUID.randomUUID();
		when(usuarioRepository.findById(usuarioComum.getId())).thenReturn(Optional.of(usuarioComum));

		assertThatThrownBy(() -> service.redefinirSenha(alvoId, SENHA_NOVA, SENHA_NOVA, usuarioComum.getId(), "127.0.0.1"))
				.isInstanceOf(AcessoNegadoException.class);

		verify(auditoriaFalhaService).registrarFalha(eq(usuarioComum), eq(alvoId),
				eq(ResultadoAuditoriaSeguranca.NEGADO), eq(MotivoAuditoriaSeguranca.SEM_PERMISSAO), eq("127.0.0.1"));
		verify(usuarioRepository, never()).save(any());
		verify(redefinicaoSenhaGlobalAuditoriaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarEAuditarAutoRedefinicao() {
		Usuario superAdmin = criarUsuario("superadmin.proprio@criati.test", true, StatusCadastro.ATIVO);
		when(usuarioRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));

		assertThatThrownBy(() -> service.redefinirSenha(superAdmin.getId(), SENHA_NOVA, SENHA_NOVA, superAdmin.getId(), "127.0.0.1"))
				.isInstanceOf(AutoRedefinicaoSenhaNaoPermitidaException.class);

		verify(auditoriaFalhaService).registrarFalha(eq(superAdmin), eq(superAdmin.getId()),
				eq(ResultadoAuditoriaSeguranca.NEGADO), eq(MotivoAuditoriaSeguranca.PROPRIO_USUARIO), eq("127.0.0.1"));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void deveRejeitarEAuditarUsuarioNaoEncontrado() {
		Usuario superAdmin = criarUsuario("superadmin.naoencontrado@criati.test", true, StatusCadastro.ATIVO);
		UUID idInexistente = UUID.randomUUID();
		when(usuarioRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.redefinirSenha(idInexistente, SENHA_NOVA, SENHA_NOVA, superAdmin.getId(), "127.0.0.1"))
				.isInstanceOf(UsuarioNaoEncontradoException.class);

		verify(auditoriaFalhaService).registrarFalha(eq(superAdmin), eq(idInexistente),
				eq(ResultadoAuditoriaSeguranca.NEGADO), eq(MotivoAuditoriaSeguranca.USUARIO_NAO_ENCONTRADO), eq("127.0.0.1"));
	}

	@Test
	void deveRejeitarEAuditarUsuarioAlvoInativo() {
		Usuario superAdmin = criarUsuario("superadmin.inativo@criati.test", true, StatusCadastro.ATIVO);
		Usuario alvoInativo = criarUsuario("alvo.inativo@criati.test", false, StatusCadastro.INATIVO);
		when(usuarioRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		when(usuarioRepository.findById(alvoInativo.getId())).thenReturn(Optional.of(alvoInativo));

		assertThatThrownBy(() -> service.redefinirSenha(alvoInativo.getId(), SENHA_NOVA, SENHA_NOVA, superAdmin.getId(), "127.0.0.1"))
				.isInstanceOf(UsuarioStatusInvalidoException.class);

		verify(auditoriaFalhaService).registrarFalha(eq(superAdmin), eq(alvoInativo.getId()),
				eq(ResultadoAuditoriaSeguranca.NEGADO), eq(MotivoAuditoriaSeguranca.USUARIO_INATIVO), eq("127.0.0.1"));
		verify(usuarioRepository, never()).save(any());
	}

	@Test
	void deveRejeitarEAuditarSenhaInvalida() {
		Usuario superAdmin = criarUsuario("superadmin.senhainvalida@criati.test", true, StatusCadastro.ATIVO);
		Usuario alvo = criarUsuario("alvo.senhainvalida@criati.test", false, StatusCadastro.ATIVO);
		when(usuarioRepository.findById(superAdmin.getId())).thenReturn(Optional.of(superAdmin));
		when(usuarioRepository.findById(alvo.getId())).thenReturn(Optional.of(alvo));
		org.mockito.Mockito.doThrow(new DadosInvalidosException("Senha deve possuir no minimo 15 caracteres"))
				.when(senhaValidador).validar("curta", "curta");

		assertThatThrownBy(() -> service.redefinirSenha(alvo.getId(), "curta", "curta", superAdmin.getId(), "127.0.0.1"))
				.isInstanceOf(DadosInvalidosException.class);

		verify(auditoriaFalhaService).registrarFalha(eq(superAdmin), eq(alvo.getId()),
				eq(ResultadoAuditoriaSeguranca.FALHA_VALIDACAO), eq(MotivoAuditoriaSeguranca.SENHA_INVALIDA), eq("127.0.0.1"));
		verify(usuarioRepository, never()).save(any());
		verify(redefinicaoSenhaGlobalAuditoriaRepository, never()).save(any());
	}

	private Usuario criarUsuario(String email, boolean superAdministrador, StatusCadastro status) {
		Usuario usuario = superAdministrador
				? Usuario.criarSuperAdministrador("Usuario Teste", email, "hash-de-teste")
				: new Usuario("Usuario Teste", email, "hash-de-teste", status);
		ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
		return usuario;
	}
}
