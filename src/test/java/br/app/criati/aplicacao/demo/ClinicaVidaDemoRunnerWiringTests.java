package br.app.criati.aplicacao.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.empresa.repository.EmpresaRepository;

// Contexto Spring dedicado (propriedade diferente dos demais testes forca um
// ApplicationContext novo): confirma que CRIATI_DADOS_DEMO_HABILITADOS chega
// de fato ao ClinicaVidaDemoRunner via application.properties + @Value, nao
// apenas quando o servico e chamado diretamente (ClinicaVidaDemoServiceTests).
@SpringBootTest(properties = "CRIATI_DADOS_DEMO_HABILITADOS=true")
@ActiveProfiles("test")
class ClinicaVidaDemoRunnerWiringTests {

	@Autowired
	private EmpresaRepository empresaRepository;

	@Test
	void deveCriarClinicaVidaDemoNaInicializacaoQuandoVariavelHabilitada() {
		assertThat(empresaRepository.findByCnpj("11222333000181")).isPresent();
	}
}
