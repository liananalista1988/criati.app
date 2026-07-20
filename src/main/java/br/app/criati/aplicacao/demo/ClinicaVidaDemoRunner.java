package br.app.criati.aplicacao.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Depende do catalogo (AplicacaoService.habilitar busca a Aplicacao CLINICA
// por codigo): precisa rodar depois de AplicacaoCatalogoSeedRunner, que roda
// em HIGHEST_PRECEDENCE. Sem @Order aqui, o Spring Boot nao garante essa
// ordem entre ApplicationRunners.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ClinicaVidaDemoRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ClinicaVidaDemoRunner.class);

	private final ClinicaVidaDemoService clinicaVidaDemoService;
	private final boolean habilitado;

	public ClinicaVidaDemoRunner(
			ClinicaVidaDemoService clinicaVidaDemoService,
			@Value("${criati.dados-demo.habilitados:false}") boolean habilitado) {
		this.clinicaVidaDemoService = clinicaVidaDemoService;
		this.habilitado = habilitado;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!habilitado) {
			log.info("Carga demo Clinica Vida Demo desabilitada (padrao); nenhuma acao realizada.");
			return;
		}
		clinicaVidaDemoService.executar();
	}
}
