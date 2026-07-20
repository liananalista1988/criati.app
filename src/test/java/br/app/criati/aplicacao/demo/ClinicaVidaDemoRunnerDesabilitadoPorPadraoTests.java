package br.app.criati.aplicacao.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.empresa.repository.EmpresaRepository;

// Contexto padrao (mesmo cache usado pela maioria dos testes, sem overrides
// de propriedade): confirma que, sem CRIATI_DADOS_DEMO_HABILITADOS=true, a
// carga demo nunca executa automaticamente na inicializacao.
@SpringBootTest
@ActiveProfiles("test")
class ClinicaVidaDemoRunnerDesabilitadoPorPadraoTests {

	@Autowired
	private EmpresaRepository empresaRepository;

	@Test
	void naoDeveCriarClinicaVidaDemoPorPadrao() {
		assertThat(empresaRepository.findByCnpj("11222333000181")).isEmpty();
	}
}
