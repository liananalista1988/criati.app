package br.app.criati.convite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ConviteServiceTests {

	@Mock
	private ConviteRepository conviteRepository;

	@Mock
	private EmpresaRepository empresaRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private ConviteTokenService conviteTokenService;

	@Mock
	private ConviteNotificador conviteNotificador;

	private ConviteService service;

	private ConviteService criarServico() {
		ConviteService instancia = new ConviteService(
				conviteRepository, empresaRepository, usuarioRepository, conviteTokenService, conviteNotificador, 72L);
		return instancia;
	}

	@Test
	void deveCriarConviteQuandoChamadorEhAdministrador() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		Empresa empresa = criarEmpresa(empresaId);
		Usuario administrador = criarUsuario(usuarioId);
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(usuarioId, empresaId, UUID.randomUUID(),
				PerfilUsuario.ADMINISTRADOR);

		when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(administrador));
		when(conviteRepository.findByEmpresaIdAndEmailIgnoreCaseAndStatus(
				empresaId, "convidado@criati.test", StatusConvite.PENDENTE))
				.thenReturn(Optional.empty());
		when(conviteTokenService.gerarTokenBruto()).thenReturn("token-bruto-gerado");
		when(conviteTokenService.calcularHash("token-bruto-gerado")).thenReturn("hash-do-token");
		when(conviteRepository.save(any(Convite.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		ConviteCriado resultado = service.criar("Convidado@Criati.Test", PerfilUsuario.GESTOR, contexto);

		assertThat(resultado.tokenBruto()).isEqualTo("token-bruto-gerado");
		assertThat(resultado.convite().getEmail()).isEqualTo("convidado@criati.test");
		assertThat(resultado.convite().getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
		assertThat(resultado.convite().getStatus()).isEqualTo(StatusConvite.PENDENTE);
		verify(conviteNotificador).notificar(resultado.convite(), "token-bruto-gerado");
	}

	@Test
	void deveRevogarConvitePendenteExistenteAntesDeCriarNovo() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		Empresa empresa = criarEmpresa(empresaId);
		Usuario administrador = criarUsuario(usuarioId);
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(usuarioId, empresaId, UUID.randomUUID(),
				PerfilUsuario.ADMINISTRADOR);
		Convite pendenteAntigo = new Convite(
				empresa, "convidado@criati.test", PerfilUsuario.USUARIO, "hash-antigo",
				OffsetDateTime.now().plusHours(10), administrador);

		when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(administrador));
		when(conviteRepository.findByEmpresaIdAndEmailIgnoreCaseAndStatus(
				empresaId, "convidado@criati.test", StatusConvite.PENDENTE))
				.thenReturn(Optional.of(pendenteAntigo));
		when(conviteTokenService.gerarTokenBruto()).thenReturn("token-novo");
		when(conviteTokenService.calcularHash("token-novo")).thenReturn("hash-novo");
		when(conviteRepository.save(any(Convite.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

		service.criar("convidado@criati.test", PerfilUsuario.GESTOR, contexto);

		assertThat(pendenteAntigo.getStatus()).isEqualTo(StatusConvite.REVOGADO);
		verify(conviteRepository).save(pendenteAntigo);
	}

	@Test
	void deveRejeitarCriacaoQuandoChamadorNaoEhAdministrador() {
		service = criarServico();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PerfilUsuario.GESTOR);

		assertThatThrownBy(() -> service.criar("convidado@criati.test", PerfilUsuario.USUARIO, contexto))
				.isInstanceOf(AcessoNegadoException.class);

		verify(conviteRepository, never()).save(any(Convite.class));
	}

	@Test
	void deveRejeitarEmailAusente() {
		service = criarServico();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);

		assertThatThrownBy(() -> service.criar(" ", PerfilUsuario.USUARIO, contexto))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("E-mail e obrigatorio");
	}

	@Test
	void deveRejeitarPerfilAusente() {
		service = criarServico();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);

		assertThatThrownBy(() -> service.criar("convidado@criati.test", null, contexto))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Perfil e obrigatorio");
	}

	@Test
	void deveLancarNullPointerExceptionQuandoContextoForNulo() {
		service = criarServico();

		assertThatThrownBy(() -> service.criar("convidado@criati.test", PerfilUsuario.USUARIO, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void deveListarApenasConvitesDaEmpresaDoContexto() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		when(conviteRepository.findAllByEmpresaId(empresaId)).thenReturn(List.of());

		List<Convite> resultado = service.listarPorEmpresa(contexto);

		assertThat(resultado).isEmpty();
		verify(conviteRepository).findAllByEmpresaId(empresaId);
	}

	@Test
	void deveRejeitarListagemQuandoNaoAdministrador() {
		service = criarServico();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PerfilUsuario.USUARIO);

		assertThatThrownBy(() -> service.listarPorEmpresa(contexto)).isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void deveRevogarConvitePendenteDaPropriaEmpresa() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		UUID conviteId = UUID.randomUUID();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		Convite convite = criarConvitePendente(empresaId);
		ReflectionTestUtils.setField(convite, "id", conviteId);
		when(conviteRepository.findByIdAndEmpresaId(conviteId, empresaId)).thenReturn(Optional.of(convite));

		service.revogar(conviteId, contexto);

		assertThat(convite.getStatus()).isEqualTo(StatusConvite.REVOGADO);
		verify(conviteRepository).save(convite);
	}

	@Test
	void deveRejeitarRevogacaoDeConviteDeOutraEmpresa() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		UUID conviteId = UUID.randomUUID();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		when(conviteRepository.findByIdAndEmpresaId(conviteId, empresaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.revogar(conviteId, contexto)).isInstanceOf(AcessoNegadoException.class);

		verify(conviteRepository, never()).save(any(Convite.class));
	}

	@Test
	void deveRejeitarRevogacaoDeConviteJaUtilizado() {
		service = criarServico();
		UUID empresaId = UUID.randomUUID();
		UUID conviteId = UUID.randomUUID();
		ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
				UUID.randomUUID(), empresaId, UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
		Convite convite = criarConvitePendente(empresaId);
		convite.marcarUtilizado(OffsetDateTime.now());
		when(conviteRepository.findByIdAndEmpresaId(conviteId, empresaId)).thenReturn(Optional.of(convite));

		assertThatThrownBy(() -> service.revogar(conviteId, contexto))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Convite nao pode ser revogado");

		verify(conviteRepository, never()).save(convite);
	}

	@Test
	void buscarValidoPeloTokenDeveRetornarVazioParaTokenInexistente() {
		service = criarServico();
		when(conviteTokenService.calcularHash("token-qualquer")).thenReturn("hash-qualquer");
		when(conviteRepository.findByTokenHash("hash-qualquer")).thenReturn(Optional.empty());

		assertThat(service.buscarValidoPeloToken("token-qualquer")).isEmpty();
	}

	@Test
	void buscarValidoPeloTokenDeveRetornarVazioParaConviteExpirado() {
		service = criarServico();
		Empresa empresa = criarEmpresa(UUID.randomUUID());
		Usuario administrador = criarUsuario(UUID.randomUUID());
		Convite convite = new Convite(
				empresa, "convidado@criati.test", PerfilUsuario.USUARIO, "hash-qualquer",
				OffsetDateTime.now().minusHours(1), administrador);
		when(conviteTokenService.calcularHash("token-expirado")).thenReturn("hash-qualquer");
		when(conviteRepository.findByTokenHash("hash-qualquer")).thenReturn(Optional.of(convite));

		assertThat(service.buscarValidoPeloToken("token-expirado")).isEmpty();
	}

	@Test
	void buscarValidoPeloTokenDeveRetornarVazioQuandoEmpresaInativa() {
		service = criarServico();
		Empresa empresa = new Empresa("Empresa Inativa Ltda", "Empresa Inativa", "99999999000199",
				StatusCadastro.INATIVO);
		Usuario administrador = criarUsuario(UUID.randomUUID());
		Convite convite = new Convite(
				empresa, "convidado@criati.test", PerfilUsuario.USUARIO, "hash-qualquer",
				OffsetDateTime.now().plusHours(10), administrador);
		when(conviteTokenService.calcularHash("token-empresa-inativa")).thenReturn("hash-qualquer");
		when(conviteRepository.findByTokenHash("hash-qualquer")).thenReturn(Optional.of(convite));

		assertThat(service.buscarValidoPeloToken("token-empresa-inativa")).isEmpty();
	}

	@Test
	void buscarValidoPeloTokenDeveRetornarConviteQuandoValido() {
		service = criarServico();
		Empresa empresa = criarEmpresa(UUID.randomUUID());
		Usuario administrador = criarUsuario(UUID.randomUUID());
		Convite convite = new Convite(
				empresa, "convidado@criati.test", PerfilUsuario.USUARIO, "hash-qualquer",
				OffsetDateTime.now().plusHours(10), administrador);
		when(conviteTokenService.calcularHash("token-valido")).thenReturn("hash-qualquer");
		when(conviteRepository.findByTokenHash("hash-qualquer")).thenReturn(Optional.of(convite));

		assertThat(service.buscarValidoPeloToken("token-valido")).contains(convite);
	}

	private Empresa criarEmpresa(UUID id) {
		Empresa empresa = new Empresa("Empresa de Teste Ltda", "Empresa de Teste", "12345678000190",
				StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(empresa, "id", id);
		return empresa;
	}

	private Usuario criarUsuario(UUID id) {
		Usuario usuario = new Usuario("Usuario de Teste", "usuario@criati.test", "hash-de-teste", StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(usuario, "id", id);
		return usuario;
	}

	private Convite criarConvitePendente(UUID empresaId) {
		Empresa empresa = criarEmpresa(empresaId);
		Usuario administrador = criarUsuario(UUID.randomUUID());
		return new Convite(empresa, "convidado@criati.test", PerfilUsuario.USUARIO, "hash-qualquer",
				OffsetDateTime.now().plusHours(10), administrador);
	}
}
