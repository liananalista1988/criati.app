package br.app.criati.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class SuperAdministradorBootstrapServiceTests {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Captor
	private ArgumentCaptor<Usuario> usuarioCaptor;

	@InjectMocks
	private SuperAdministradorBootstrapService service;

	@Test
	void deveCriarPrimeiroSuperAdministradorQuandoConfiguracaoCompleta() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(false);
		when(usuarioRepository.existsByEmailIgnoreCase("admin@criati.test")).thenReturn(false);
		when(passwordEncoder.encode("senha-bruta-do-bootstrap")).thenReturn("hash-codificado");

		service.executar("Superadministrador", "Admin@Criati.Test", "senha-bruta-do-bootstrap");

		verify(usuarioRepository).save(usuarioCaptor.capture());
		Usuario salvo = usuarioCaptor.getValue();
		assertThat(salvo.isSuperAdministrador()).isTrue();
		assertThat(salvo.getEmail()).isEqualTo("admin@criati.test");
		assertThat(salvo.getSenha()).isEqualTo("hash-codificado");
		assertThat(salvo.getSenha()).isNotEqualTo("senha-bruta-do-bootstrap");
	}

	@Test
	void naoDeveCriarQuandoJaExisteSuperAdministrador() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(true);

		service.executar("Outro Admin", "outro@criati.test", "senha-qualquer");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void naoDeveCriarComNomeAusente() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(false);

		service.executar(null, "admin@criati.test", "senha-bruta-do-bootstrap");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void naoDeveCriarComEmailAusente() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(false);

		service.executar("Superadministrador", "  ", "senha-bruta-do-bootstrap");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void naoDeveCriarComSenhaAusente() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(false);

		service.executar("Superadministrador", "admin@criati.test", "");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void naoDeveCriarQuandoEmailJaEstaEmUso() {
		when(usuarioRepository.existsBySuperAdministradorTrue()).thenReturn(false);
		when(usuarioRepository.existsByEmailIgnoreCase("existente@criati.test")).thenReturn(true);

		service.executar("Superadministrador", "existente@criati.test", "senha-bruta-do-bootstrap");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}
}
