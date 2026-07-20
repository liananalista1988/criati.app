package br.app.criati.acesso.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.AutoAlteracaoNaoPermitidaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UltimoAdministradorAtivoException;
import br.app.criati.exception.VinculoStatusInvalidoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;

@ExtendWith(MockitoExtension.class)
class GerenciarUsuarioEmpresaServiceTests {

	@Mock
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@InjectMocks
	private GerenciarUsuarioEmpresaService service;

	private final Empresa empresaAtiva = criarEmpresa();
	private final UUID empresaAtivaId = empresaAtiva.getId();

	// --- listar ---------------------------------------------------------

	@Test
	void deveListarApenasIntegrantesDaEmpresaAtiva() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("membro@criati.test"), PerfilUsuario.GESTOR, StatusCadastro.ATIVO);
		when(usuarioEmpresaRepository.findAllByEmpresaId(empresaAtivaId)).thenReturn(List.of(vinculo));

		List<UsuarioEmpresa> resultado = service.listar(contextoAdministrador(), null, null, null);

		assertThat(resultado).containsExactly(vinculo);
	}

	@Test
	void deveFiltrarPorStatus() {
		UsuarioEmpresa ativo = criarVinculo(criarUsuario("ativo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		UsuarioEmpresa inativo = criarVinculo(criarUsuario("inativo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.INATIVO);
		when(usuarioEmpresaRepository.findAllByEmpresaId(empresaAtivaId)).thenReturn(List.of(ativo, inativo));

		List<UsuarioEmpresa> resultado = service.listar(contextoAdministrador(), StatusCadastro.INATIVO, null, null);

		assertThat(resultado).containsExactly(inativo);
	}

	@Test
	void deveFiltrarPorPerfil() {
		UsuarioEmpresa gestor = criarVinculo(criarUsuario("gestor@criati.test"), PerfilUsuario.GESTOR, StatusCadastro.ATIVO);
		UsuarioEmpresa usuario = criarVinculo(criarUsuario("usuario@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		when(usuarioEmpresaRepository.findAllByEmpresaId(empresaAtivaId)).thenReturn(List.of(gestor, usuario));

		List<UsuarioEmpresa> resultado = service.listar(contextoAdministrador(), null, PerfilUsuario.GESTOR, null);

		assertThat(resultado).containsExactly(gestor);
	}

	@Test
	void deveFiltrarPorBuscaNoNomeOuEmail() {
		UsuarioEmpresa maria = criarVinculo(criarUsuario("Maria Silva", "maria@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		UsuarioEmpresa joao = criarVinculo(criarUsuario("Joao Souza", "joao@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		when(usuarioEmpresaRepository.findAllByEmpresaId(empresaAtivaId)).thenReturn(List.of(maria, joao));

		List<UsuarioEmpresa> resultado = service.listar(contextoAdministrador(), null, null, "mar");

		assertThat(resultado).containsExactly(maria);
	}

	@Test
	void deveRejeitarListagemQuandoChamadorNaoEhAdministrador() {
		assertThatThrownBy(() -> service.listar(contextoGestor(), null, null, null))
				.isInstanceOf(AcessoNegadoException.class);
	}

	// --- buscar ----------------------------------------------------------

	@Test
	void deveConsultarVinculoDaEmpresaAtiva() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("consulta@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculo.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculo));

		UsuarioEmpresa resultado = service.buscar(vinculo.getId(), contextoAdministrador());

		assertThat(resultado).isSameAs(vinculo);
	}

	@Test
	void deveRejeitarConsultaDeVinculoDeOutraEmpresa() {
		UUID idDeOutraEmpresa = UUID.randomUUID();
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(idDeOutraEmpresa, empresaAtivaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(idDeOutraEmpresa, contextoAdministrador()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void deveRejeitarConsultaQuandoChamadorNaoEhAdministrador() {
		assertThatThrownBy(() -> service.buscar(UUID.randomUUID(), contextoGestor()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	// --- alterarPerfil -----------------------------------------------------

	@Test
	void deveAlterarPerfilDeUsuarioParaGestor() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("alvo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		mockarBusca(vinculo);
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

		UsuarioEmpresa resultado = service.alterarPerfil(vinculo.getId(), PerfilUsuario.GESTOR, contextoAdministrador());

		assertThat(resultado.getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
	}

	@Test
	void deveRejeitarPerfilNulo() {
		assertThatThrownBy(() -> service.alterarPerfil(UUID.randomUUID(), null, contextoAdministrador()))
				.isInstanceOf(DadosInvalidosException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarAlteracaoDeVinculoDeOutraEmpresa() {
		UUID idAlvo = UUID.randomUUID();
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(idAlvo, empresaAtivaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.alterarPerfil(idAlvo, PerfilUsuario.GESTOR, contextoAdministrador()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void deveRejeitarAlteracaoQuandoChamadorNaoEhAdministrador() {
		assertThatThrownBy(() -> service.alterarPerfil(UUID.randomUUID(), PerfilUsuario.GESTOR, contextoGestor()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	@Test
	void deveRejeitarAlteracaoDeVinculoInativo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("inativo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.INATIVO);
		mockarBusca(vinculo);

		assertThatThrownBy(() -> service.alterarPerfil(vinculo.getId(), PerfilUsuario.GESTOR, contextoAdministrador()))
				.isInstanceOf(VinculoStatusInvalidoException.class);
	}

	@Test
	void deveRejeitarRebaixamentoDoUltimoAdministradorAtivo() {
		Usuario usuarioAlvo = criarUsuario("admin@criati.test");
		UsuarioEmpresa vinculo = criarVinculo(usuarioAlvo, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(UUID.randomUUID());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculo.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculo));
		when(usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				empresaAtivaId, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO)).thenReturn(1L);

		assertThatThrownBy(() -> service.alterarPerfil(vinculo.getId(), PerfilUsuario.GESTOR, contexto))
				.isInstanceOf(UltimoAdministradorAtivoException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void devePermitirRebaixarAdministradorQuandoHaOutroAtivo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("admin2@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(UUID.randomUUID());
		mockarBusca(vinculo);
		when(usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				empresaAtivaId, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO)).thenReturn(2L);
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

		UsuarioEmpresa resultado = service.alterarPerfil(vinculo.getId(), PerfilUsuario.GESTOR, contexto);

		assertThat(resultado.getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
	}

	@Test
	void deveRejeitarAutoRebaixamento() {
		UsuarioEmpresa vinculoDoChamador = criarVinculo(criarUsuario("proprio@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(vinculoDoChamador.getId());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculoDoChamador.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculoDoChamador));

		assertThatThrownBy(() -> service.alterarPerfil(vinculoDoChamador.getId(), PerfilUsuario.GESTOR, contexto))
				.isInstanceOf(AutoAlteracaoNaoPermitidaException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	// --- suspender -----------------------------------------------------

	@Test
	void deveSuspenderVinculoComum() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("suspenso@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		mockarBusca(vinculo);
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

		UsuarioEmpresa resultado = service.suspender(vinculo.getId(), contextoAdministrador());

		assertThat(resultado.getStatus()).isEqualTo(StatusCadastro.INATIVO);
	}

	@Test
	void deveRejeitarAutoSuspensao() {
		UsuarioEmpresa vinculoDoChamador = criarVinculo(criarUsuario("proprio@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(vinculoDoChamador.getId());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculoDoChamador.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculoDoChamador));

		assertThatThrownBy(() -> service.suspender(vinculoDoChamador.getId(), contexto))
				.isInstanceOf(AutoAlteracaoNaoPermitidaException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarSuspensaoDoUltimoAdministradorAtivoDiretoPeloServico() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("admin@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(UUID.randomUUID());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculo.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculo));
		when(usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				empresaAtivaId, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO)).thenReturn(1L);

		assertThatThrownBy(() -> service.suspender(vinculo.getId(), contexto))
				.isInstanceOf(UltimoAdministradorAtivoException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarSuspensaoDeVinculoJaInativo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("jainativo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.INATIVO);
		mockarBusca(vinculo);

		assertThatThrownBy(() -> service.suspender(vinculo.getId(), contextoAdministrador()))
				.isInstanceOf(VinculoStatusInvalidoException.class);
	}

	@Test
	void deveRejeitarSuspensaoDeVinculoDeOutraEmpresa() {
		UUID idAlvo = UUID.randomUUID();
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(idAlvo, empresaAtivaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.suspender(idAlvo, contextoAdministrador()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	// --- reativar --------------------------------------------------------

	@Test
	void deveReativarVinculoInativoPreservandoPerfil() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("reativa@criati.test"), PerfilUsuario.GESTOR, StatusCadastro.INATIVO);
		mockarBusca(vinculo);
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

		UsuarioEmpresa resultado = service.reativar(vinculo.getId(), contextoAdministrador());

		assertThat(resultado.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(resultado.getPerfil()).isEqualTo(PerfilUsuario.GESTOR);
	}

	@Test
	void deveRejeitarReativacaoDeVinculoJaAtivo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("jaativo@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		mockarBusca(vinculo);

		assertThatThrownBy(() -> service.reativar(vinculo.getId(), contextoAdministrador()))
				.isInstanceOf(VinculoStatusInvalidoException.class);
	}

	@Test
	void deveRejeitarReativacaoDeVinculoDeOutraEmpresa() {
		UUID idAlvo = UUID.randomUUID();
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(idAlvo, empresaAtivaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.reativar(idAlvo, contextoAdministrador()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	// --- remover -----------------------------------------------------------

	@Test
	void deveRemoverLogicamenteVinculoComum() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("remove@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.ATIVO);
		mockarBusca(vinculo);
		when(usuarioEmpresaRepository.save(any(UsuarioEmpresa.class))).thenAnswer(inv -> inv.getArgument(0));

		service.remover(vinculo.getId(), contextoAdministrador());

		assertThat(vinculo.getStatus()).isEqualTo(StatusCadastro.INATIVO);
		assertThat(vinculo.getUsuario()).isNotNull();
	}

	@Test
	void deveRejeitarAutoRemocao() {
		UsuarioEmpresa vinculoDoChamador = criarVinculo(criarUsuario("proprio@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(vinculoDoChamador.getId());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculoDoChamador.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculoDoChamador));

		assertThatThrownBy(() -> service.remover(vinculoDoChamador.getId(), contexto))
				.isInstanceOf(AutoAlteracaoNaoPermitidaException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarRemocaoDoUltimoAdministradorAtivo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("admin@criati.test"), PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		ContextoEmpresaAtual contexto = contextoAdministradorComVinculo(UUID.randomUUID());
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculo.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculo));
		when(usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				empresaAtivaId, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO)).thenReturn(1L);

		assertThatThrownBy(() -> service.remover(vinculo.getId(), contexto))
				.isInstanceOf(UltimoAdministradorAtivoException.class);

		verify(usuarioEmpresaRepository, never()).save(any());
	}

	@Test
	void deveRejeitarRemocaoDeVinculoJaInativo() {
		UsuarioEmpresa vinculo = criarVinculo(criarUsuario("jaremovido@criati.test"), PerfilUsuario.USUARIO, StatusCadastro.INATIVO);
		mockarBusca(vinculo);

		assertThatThrownBy(() -> service.remover(vinculo.getId(), contextoAdministrador()))
				.isInstanceOf(VinculoStatusInvalidoException.class);
	}

	@Test
	void deveRejeitarRemocaoDeVinculoDeOutraEmpresa() {
		UUID idAlvo = UUID.randomUUID();
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(idAlvo, empresaAtivaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.remover(idAlvo, contextoAdministrador()))
				.isInstanceOf(AcessoNegadoException.class);
	}

	// --- auxiliares ------------------------------------------------------

	private void mockarBusca(UsuarioEmpresa vinculo) {
		when(usuarioEmpresaRepository.findByIdAndEmpresaId(vinculo.getId(), empresaAtivaId))
				.thenReturn(Optional.of(vinculo));
	}

	private ContextoEmpresaAtual contextoAdministrador() {
		return contextoAdministradorComVinculo(UUID.randomUUID());
	}

	private ContextoEmpresaAtual contextoAdministradorComVinculo(UUID vinculoChamadorId) {
		return new ContextoEmpresaAtual(UUID.randomUUID(), empresaAtivaId, vinculoChamadorId, PerfilUsuario.ADMINISTRADOR);
	}

	private ContextoEmpresaAtual contextoGestor() {
		return new ContextoEmpresaAtual(UUID.randomUUID(), empresaAtivaId, UUID.randomUUID(), PerfilUsuario.GESTOR);
	}

	private Usuario criarUsuario(String email) {
		return criarUsuario("Usuario de Teste", email);
	}

	private Usuario criarUsuario(String nome, String email) {
		Usuario usuario = new Usuario(nome, email, "senha-hash-de-teste", StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
		return usuario;
	}

	private Empresa criarEmpresa() {
		Empresa empresa = new Empresa("Empresa de Teste Ltda", "Empresa de Teste", "12345678000190", StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(empresa, "id", UUID.randomUUID());
		return empresa;
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, PerfilUsuario perfil, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresaAtiva, perfil, status);
		ReflectionTestUtils.setField(vinculo, "id", UUID.randomUUID());
		return vinculo;
	}
}
