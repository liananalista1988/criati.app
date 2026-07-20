package br.app.criati.aplicacao.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.model.EmpresaAplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClinicaVidaDemoServiceTests {

	private static final String CNPJ_DEMO = "11222333000181";

	@Autowired
	private ClinicaVidaDemoService clinicaVidaDemoService;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private AplicacaoRepository aplicacaoRepository;

	@Autowired
	private EmpresaAplicacaoRepository empresaAplicacaoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void deveCriarEmpresaClinicaVidaDemoComAplicacaoClinicaHabilitada() {
		long usuariosAntes = usuarioRepository.count();

		clinicaVidaDemoService.executar();

		Empresa empresa = empresaRepository.findByCnpj(CNPJ_DEMO)
				.orElseThrow(() -> new AssertionError("Empresa Clinica Vida Demo nao foi criada"));
		assertThat(empresa.getNome()).isEqualTo("Clinica Vida Demo");
		assertThat(empresa.getStatus()).isEqualTo(StatusCadastro.ATIVO);

		Aplicacao clinica = aplicacaoRepository.findByCodigo("CLINICA")
				.orElseThrow(() -> new AssertionError("Aplicacao CLINICA nao existe no catalogo"));
		assertThat(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresa.getId(), clinica.getId(), StatusCadastro.ATIVO)).isTrue();

		assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
	}

	@Test
	void naoDeveCriarEmpresaDemoFinanceira() {
		clinicaVidaDemoService.executar();

		Empresa empresa = empresaRepository.findByCnpj(CNPJ_DEMO).orElseThrow();
		Aplicacao financeiro = aplicacaoRepository.findByCodigo("FINANCEIRO")
				.orElseThrow(() -> new AssertionError("Aplicacao FINANCEIRO nao existe no catalogo"));

		assertThat(empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
				empresa.getId(), financeiro.getId(), StatusCadastro.ATIVO)).isFalse();
	}

	@Test
	void deveSerIdempotenteNaoDuplicandoEmpresaNemVinculo() {
		clinicaVidaDemoService.executar();
		clinicaVidaDemoService.executar();

		long quantidadeEmpresasDemo = empresaRepository.findAll().stream()
				.filter(e -> CNPJ_DEMO.equals(e.getCnpj()))
				.count();
		assertThat(quantidadeEmpresasDemo).isEqualTo(1);

		Empresa empresa = empresaRepository.findByCnpj(CNPJ_DEMO).orElseThrow();
		List<EmpresaAplicacao> vinculos = empresaAplicacaoRepository.findAllByEmpresaId(empresa.getId());
		assertThat(vinculos).hasSize(1);
		assertThat(vinculos.get(0).getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}
}
