package br.app.criati.usuario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CadastrarUsuarioServiceTests {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Captor
	private ArgumentCaptor<Usuario> usuarioCaptor;

	@InjectMocks
	private CadastrarUsuarioService service;

	@Test
	void deveCadastrarUsuarioAplicandoHashENaoArmazenarSenhaBruta() {
		when(passwordEncoder.encode("senha-bruta-segura")).thenReturn("hash-codificado-pelo-encoder");
		when(usuarioRepository.save(any(Usuario.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Usuario resultado = service.executar(
				"Usuario Criati",
				"usuario@criati.test",
				"senha-bruta-segura");

		verify(usuarioRepository).save(usuarioCaptor.capture());
		Usuario usuarioSalvo = usuarioCaptor.getValue();
		assertThat(resultado).isSameAs(usuarioSalvo);
		assertThat(usuarioSalvo.getNome()).isEqualTo("Usuario Criati");
		assertThat(usuarioSalvo.getEmail()).isEqualTo("usuario@criati.test");
		assertThat(usuarioSalvo.getSenha()).isEqualTo("hash-codificado-pelo-encoder");
		assertThat(usuarioSalvo.getSenha()).isNotEqualTo("senha-bruta-segura");
		assertThat(usuarioSalvo.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void deveNormalizarEmail() {
		when(passwordEncoder.encode(any())).thenReturn("hash-codificado-pelo-encoder");

		service.executar("Usuario Normalizado", "  Usuario@Criati.Test  ", "senha-bruta-segura");

		verify(usuarioRepository).existsByEmailIgnoreCase("usuario@criati.test");
		verify(usuarioRepository).save(usuarioCaptor.capture());
		assertThat(usuarioCaptor.getValue().getEmail()).isEqualTo("usuario@criati.test");
	}

	@Test
	void deveRejeitarEmailDuplicado() {
		when(usuarioRepository.existsByEmailIgnoreCase("usuario@criati.test")).thenReturn(true);

		assertThatThrownBy(() -> service.executar(
				"Usuario Duplicado",
				"  Usuario@Criati.Test  ",
				"senha-bruta-segura"))
				.isInstanceOf(EmailJaCadastradoException.class);

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void deveRejeitarEmailVazio() {
		assertThatThrownBy(() -> service.executar("Usuario", "   ", "senha-bruta-segura"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("E-mail e obrigatorio");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void deveRejeitarSenhaVazia() {
		assertThatThrownBy(() -> service.executar("Usuario", "usuario@criati.test", "   "))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Senha e obrigatoria");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}
}
