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

import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class CadastrarUsuarioServiceTests {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Captor
	private ArgumentCaptor<Usuario> usuarioCaptor;

	@InjectMocks
	private CadastrarUsuarioService service;

	@Test
	void deveCadastrarUsuarioValido() {
		when(usuarioRepository.save(any(Usuario.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Usuario resultado = service.executar(
				"Usuario Criati",
				"usuario@criati.test",
				"senha-hash-segura");

		verify(usuarioRepository).save(usuarioCaptor.capture());
		Usuario usuarioSalvo = usuarioCaptor.getValue();
		assertThat(resultado).isSameAs(usuarioSalvo);
		assertThat(usuarioSalvo.getNome()).isEqualTo("Usuario Criati");
		assertThat(usuarioSalvo.getEmail()).isEqualTo("usuario@criati.test");
		assertThat(usuarioSalvo.getSenha()).isEqualTo("senha-hash-segura");
		assertThat(usuarioSalvo.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void deveNormalizarEmail() {
		service.executar("Usuario Normalizado", "  Usuario@Criati.Test  ", "senha-hash-segura");

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
				"senha-hash-segura"))
				.isInstanceOf(EmailJaCadastradoException.class);

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void deveRejeitarEmailVazio() {
		assertThatThrownBy(() -> service.executar("Usuario", "   ", "senha-hash-segura"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("E-mail e obrigatorio");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}

	@Test
	void deveRejeitarSenhaHashVazia() {
		assertThatThrownBy(() -> service.executar("Usuario", "usuario@criati.test", "   "))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Hash da senha e obrigatorio");

		verify(usuarioRepository, never()).save(any(Usuario.class));
	}
}
