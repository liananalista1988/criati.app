package br.app.criati.aplicacao.service;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.model.EmpresaAplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AplicacaoInativaException;
import br.app.criati.exception.AplicacaoNaoEncontradaException;
import br.app.criati.exception.EmpresaInativaException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.shared.enums.StatusCadastro;

@ExtendWith(MockitoExtension.class)
class AplicacaoServiceTests {

	@Mock
	private AplicacaoRepository aplicacaoRepository;

	@Mock
	private EmpresaAplicacaoRepository empresaAplicacaoRepository;

	@Mock
	private EmpresaRepository empresaRepository;

	@InjectMocks
	private AplicacaoService service;

	private final Empresa empresaAtiva = criarEmpresa(StatusCadastro.ATIVO);
	private final Aplicacao aplicacaoAtiva = criarAplicacao("FINANCEIRO", StatusCadastro.ATIVO);

	@Test
	void deveHabilitarCriandoVinculoQuandoNaoExiste() {
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.findByEmpresaIdAndAplicacaoId(empresaAtiva.getId(), aplicacaoAtiva.getId()))
				.thenReturn(Optional.empty());
		when(empresaAplicacaoRepository.save(any(EmpresaAplicacao.class))).thenAnswer(inv -> inv.getArgument(0));

		EmpresaAplicacao resultado = service.habilitar(empresaAtiva.getId(), "FINANCEIRO");

		assertThat(resultado.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(resultado.getEmpresa()).isEqualTo(empresaAtiva);
		assertThat(resultado.getAplicacao()).isEqualTo(aplicacaoAtiva);
	}

	@Test
	void habilitarEIdempotenteENaoDuplicaVinculoJaExistente() {
		EmpresaAplicacao existente = new EmpresaAplicacao(empresaAtiva, aplicacaoAtiva, StatusCadastro.ATIVO);
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.findByEmpresaIdAndAplicacaoId(empresaAtiva.getId(), aplicacaoAtiva.getId()))
				.thenReturn(Optional.of(existente));
		when(empresaAplicacaoRepository.save(any(EmpresaAplicacao.class))).thenAnswer(inv -> inv.getArgument(0));

		service.habilitar(empresaAtiva.getId(), "FINANCEIRO");

		verify(empresaAplicacaoRepository).save(existente);
	}

	@Test
	void deveRejeitarHabilitarQuandoEmpresaInativa() {
		Empresa empresaInativa = criarEmpresa(StatusCadastro.INATIVO);
		when(empresaRepository.findById(empresaInativa.getId())).thenReturn(Optional.of(empresaInativa));

		assertThatThrownBy(() -> service.habilitar(empresaInativa.getId(), "FINANCEIRO"))
				.isInstanceOf(EmpresaInativaException.class);

		verify(empresaAplicacaoRepository, never()).save(any());
	}

	@Test
	void deveRejeitarHabilitarQuandoAplicacaoInativa() {
		Aplicacao aplicacaoInativa = criarAplicacao("CLINICA", StatusCadastro.INATIVO);
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("CLINICA")).thenReturn(Optional.of(aplicacaoInativa));

		assertThatThrownBy(() -> service.habilitar(empresaAtiva.getId(), "CLINICA"))
				.isInstanceOf(AplicacaoInativaException.class);

		verify(empresaAplicacaoRepository, never()).save(any());
	}

	@Test
	void deveLancarNaoEncontradaQuandoEmpresaInexistenteAoHabilitar() {
		UUID empresaId = UUID.randomUUID();
		when(empresaRepository.findById(empresaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.habilitar(empresaId, "FINANCEIRO"))
				.isInstanceOf(EmpresaNaoEncontradaException.class);
	}

	@Test
	void deveLancarNaoEncontradaQuandoCodigoInexistenteAoHabilitar() {
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("INEXISTENTE")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.habilitar(empresaAtiva.getId(), "INEXISTENTE"))
				.isInstanceOf(AplicacaoNaoEncontradaException.class);
	}

	@Test
	void desabilitarEIdempotenteQuandoVinculoNuncaExistiu() {
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.findByEmpresaIdAndAplicacaoId(empresaAtiva.getId(), aplicacaoAtiva.getId()))
				.thenReturn(Optional.empty());

		service.desabilitar(empresaAtiva.getId(), "FINANCEIRO");

		verify(empresaAplicacaoRepository, never()).save(any());
	}

	@Test
	void desabilitarMarcaVinculoExistenteComoInativoSemExcluirFisicamente() {
		EmpresaAplicacao vinculo = new EmpresaAplicacao(empresaAtiva, aplicacaoAtiva, StatusCadastro.ATIVO);
		when(empresaRepository.findById(empresaAtiva.getId())).thenReturn(Optional.of(empresaAtiva));
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.findByEmpresaIdAndAplicacaoId(empresaAtiva.getId(), aplicacaoAtiva.getId()))
				.thenReturn(Optional.of(vinculo));
		when(empresaAplicacaoRepository.save(any(EmpresaAplicacao.class))).thenAnswer(inv -> inv.getArgument(0));

		service.desabilitar(empresaAtiva.getId(), "FINANCEIRO");

		assertThat(vinculo.getStatus()).isEqualTo(StatusCadastro.INATIVO);
		verify(empresaAplicacaoRepository, never()).delete(any());
		verify(empresaAplicacaoRepository, never()).deleteById(any());
	}

	@Test
	void possuiAplicacaoAtivaRetornaFalseQuandoVinculoInativo() {
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresaAtiva.getId(), aplicacaoAtiva.getId(), StatusCadastro.ATIVO)).thenReturn(false);

		assertThat(service.possuiAplicacaoAtiva(empresaAtiva.getId(), "FINANCEIRO")).isFalse();
	}

	@Test
	void possuiAplicacaoAtivaRetornaFalseQuandoAplicacaoInativaMesmoComVinculoAtivo() {
		Aplicacao aplicacaoInativa = criarAplicacao("CLINICA", StatusCadastro.INATIVO);
		when(aplicacaoRepository.findByCodigo("CLINICA")).thenReturn(Optional.of(aplicacaoInativa));

		assertThat(service.possuiAplicacaoAtiva(empresaAtiva.getId(), "CLINICA")).isFalse();

		verify(empresaAplicacaoRepository, never())
				.existsByEmpresaIdAndAplicacaoIdAndStatus(any(), any(), any());
	}

	@Test
	void possuiAplicacaoAtivaRetornaTrueQuandoVinculoEAplicacaoAtivos() {
		when(aplicacaoRepository.findByCodigo("FINANCEIRO")).thenReturn(Optional.of(aplicacaoAtiva));
		when(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresaAtiva.getId(), aplicacaoAtiva.getId(), StatusCadastro.ATIVO)).thenReturn(true);

		assertThat(service.possuiAplicacaoAtiva(empresaAtiva.getId(), "FINANCEIRO")).isTrue();
	}

	@Test
	void possuiAplicacaoAtivaRetornaFalseQuandoCodigoInexistente() {
		when(aplicacaoRepository.findByCodigo("INEXISTENTE")).thenReturn(Optional.empty());

		assertThat(service.possuiAplicacaoAtiva(empresaAtiva.getId(), "INEXISTENTE")).isFalse();
	}

	private Empresa criarEmpresa(StatusCadastro status) {
		Empresa empresa = new Empresa("Empresa de Teste Ltda", "Empresa de Teste", "12345678000190", status);
		ReflectionTestUtils.setField(empresa, "id", UUID.randomUUID());
		return empresa;
	}

	private Aplicacao criarAplicacao(String codigo, StatusCadastro status) {
		Aplicacao aplicacao = new Aplicacao(codigo, codigo, "descricao de teste", status);
		ReflectionTestUtils.setField(aplicacao, "id", UUID.randomUUID());
		return aplicacao;
	}
}
