package br.app.criati.acesso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class VincularUsuarioEmpresaServiceTests {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private EmpresaRepository empresaRepository;

	@Mock
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Captor
	private ArgumentCaptor<UsuarioEmpresa> vinculoCaptor;

	@InjectMocks
	private VincularUsuarioEmpresaService service;

	@Test
	void deveCriarVinculoValido() {
		UUID usuarioId = UUID.randomUUID();
		UUID empresaId = UUID.randomUUID();
		Usuario usuario = criarUsuario();
		Empresa empresa = criarEmpresa();
		ContextoEmpresaAtual contexto = contextoAdministrador(empresaId);
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
		when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		UsuarioEmpresa resultado = service.executar(usuarioId, empresaId, PerfilUsuario.GESTOR, contexto);

		verify(usuarioEmpresaRepository).save(vinculoCaptor.capture());
		UsuarioEmpresa vinculoSalvo = vinculoCaptor.getValue();
		assertThat(resultado).isSameAs(vinculoSalvo);
		assertThat(vinculoSalvo.getUsuario()).isSameAs(usuario);
		assertThat(vinculoSalvo.getEmpresa()).isSameAs(empresa);
		assertThat(vinculoSalvo.getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
		assertThat(vinculoSalvo.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void deveRejeitarVinculoDuplicado() {
		UUID usuarioId = UUID.randomUUID();
		UUID empresaId = UUID.randomUUID();
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(criarUsuario()));
		when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(criarEmpresa()));
		when(usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(usuarioId, empresaId)).thenReturn(true);

		assertThatThrownBy(() -> service.executar(
				usuarioId, empresaId, PerfilUsuario.USUARIO, contextoAdministrador(empresaId)))
				.isInstanceOf(UsuarioEmpresaJaVinculadoException.class);

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	@Test
	void deveRejeitarUsuarioInexistente() {
		UUID usuarioId = UUID.randomUUID();
		UUID empresaId = UUID.randomUUID();
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.executar(
				usuarioId, empresaId, PerfilUsuario.USUARIO, contextoAdministrador(empresaId)))
				.isInstanceOf(UsuarioNaoEncontradoException.class);

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	@Test
	void deveRejeitarEmpresaInexistente() {
		UUID usuarioId = UUID.randomUUID();
		UUID empresaId = UUID.randomUUID();
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(criarUsuario()));
		when(empresaRepository.findById(empresaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.executar(
				usuarioId, empresaId, PerfilUsuario.USUARIO, contextoAdministrador(empresaId)))
				.isInstanceOf(EmpresaNaoEncontradaException.class);

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	@Test
	void deveRejeitarPerfilNulo() {
		UUID empresaId = UUID.randomUUID();

		assertThatThrownBy(() -> service.executar(
				UUID.randomUUID(), empresaId, null, contextoAdministrador(empresaId)))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Perfil e obrigatorio");

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	@Test
	void deveRejeitarQuandoEmpresaDaOperacaoNaoEhAEmpresaAtivaDoContexto() {
		UUID empresaAtiva = UUID.randomUUID();
		UUID outraEmpresa = UUID.randomUUID();

		assertThatThrownBy(() -> service.executar(
				UUID.randomUUID(), outraEmpresa, PerfilUsuario.USUARIO, contextoAdministrador(empresaAtiva)))
				.isInstanceOf(AcessoNegadoException.class);

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	@Test
	void deveRejeitarQuandoChamadorNaoEhAdministradorDaEmpresaAtiva() {
		UUID empresaId = UUID.randomUUID();
		ContextoEmpresaAtual contextoGestor = new ContextoEmpresaAtual(
				UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.GESTOR);

		assertThatThrownBy(() -> service.executar(
				UUID.randomUUID(), empresaId, PerfilUsuario.USUARIO, contextoGestor))
				.isInstanceOf(AcessoNegadoException.class);

		verify(usuarioEmpresaRepository, never()).save(any(UsuarioEmpresa.class));
	}

	private ContextoEmpresaAtual contextoAdministrador(UUID empresaId) {
		return new ContextoEmpresaAtual(UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
	}

	private Usuario criarUsuario() {
		return new Usuario(
				"Usuario de Teste",
				"usuario@criati.test",
				"senha-hash-de-teste",
				StatusCadastro.ATIVO);
	}

	private Empresa criarEmpresa() {
		return new Empresa(
				"Empresa de Teste Ltda",
				"Empresa de Teste",
				"12345678000190",
				StatusCadastro.ATIVO);
	}
}
