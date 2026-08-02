package br.app.criati.aplicacao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.SituacaoDisponibilidadeModulo;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;

@ExtendWith(MockitoExtension.class)
class ModuloDisponibilidadeServiceTests {

	@Mock
	private AplicacaoService aplicacaoService;

	@InjectMocks
	private ModuloDisponibilidadeService service;

	private final ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
			UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PerfilUsuario.USUARIO);

	@Test
	void catalogoCentralTemCodigosUnicosOrdemEstavelEDistingueModulosFuturos() {
		List<CodigoAplicacao> catalogo = service.listarCatalogoTecnico();

		assertThat(catalogo).extracting(Enum::name).doesNotHaveDuplicates()
				.containsExactly("FINANCEIRO", "CLINICA", "TAREFAS_PROCESSOS", "ESTOQUE");
		assertThat(CodigoAplicacao.TAREFAS_PROCESSOS.getSituacaoDisponibilidade())
				.isEqualTo(SituacaoDisponibilidadeModulo.INDISPONIVEL);
		assertThat(CodigoAplicacao.TAREFAS_PROCESSOS.getRotaInicial()).isNull();
	}

	@Test
	void listaSomenteModulosConhecidosDisponiveisEHabilitadosParaAEmpresaAtual() {
		when(aplicacaoService.listarAtivasDaEmpresa(contexto.empresaId())).thenReturn(List.of(
				aplicacao("CLINICA"), aplicacao("DESCONHECIDA"), aplicacao("FINANCEIRO")));

		assertThat(service.listarDisponiveis(contexto)).extracting(item -> item.modulo().name())
				.containsExactly("FINANCEIRO", "CLINICA");
		verify(aplicacaoService).listarAtivasDaEmpresa(contexto.empresaId());
	}

	@Test
	void informaNenhumUmOuVariosModulosSemInventarRota() {
		when(aplicacaoService.listarAtivasDaEmpresa(contexto.empresaId()))
				.thenReturn(List.of())
				.thenReturn(List.of(aplicacao("FINANCEIRO")))
				.thenReturn(List.of(aplicacao("FINANCEIRO"), aplicacao("CLINICA")));

		assertThat(service.rotaInicialQuandoUnico(contexto)).isEmpty();
		assertThat(service.rotaInicialQuandoUnico(contexto)).contains("/app/financeiro");
		assertThat(service.rotaInicialQuandoUnico(contexto)).isEmpty();
	}

	@Test
	void permissaoDeVisualizacaoConsideraContextoModuloTecnicoEVinculoDaEmpresa() {
		when(aplicacaoService.possuiAplicacaoAtiva(contexto.empresaId(), "FINANCEIRO")).thenReturn(true);

		assertThat(service.podeVisualizar(CodigoAplicacao.FINANCEIRO, contexto)).isTrue();
		assertThat(service.podeVisualizar(CodigoAplicacao.ESTOQUE, contexto)).isFalse();
		assertThat(service.podeVisualizar(null, contexto)).isFalse();
		assertThat(service.podeVisualizar(CodigoAplicacao.FINANCEIRO, null)).isFalse();
	}

	private Aplicacao aplicacao(String codigo) {
		return new Aplicacao(codigo, codigo, "Descrição", StatusCadastro.ATIVO);
	}
}
