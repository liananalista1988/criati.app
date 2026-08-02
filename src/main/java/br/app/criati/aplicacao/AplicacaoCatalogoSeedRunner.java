package br.app.criati.aplicacao;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.StatusCadastro;

/**
 * Garante que o catalogo inicial (FINANCEIRO, CLINICA) exista, de forma
 * idempotente, em qualquer perfil. Complementa
 * V4__criar_estrutura_aplicacoes.sql: em Postgres (local/homolog/prod) a
 * migration ja insere essas linhas e esta rotina apenas confirma que existem;
 * no perfil test (Flyway desabilitado, schema gerado por
 * ddl-auto=create-drop) esta rotina e quem efetivamente semeia o catalogo.
 *
 * {@code @Order(HIGHEST_PRECEDENCE)}: precisa rodar antes de qualquer outro
 * ApplicationRunner que dependa do catalogo (ex.: ClinicaVidaDemoRunner), pois
 * o Spring Boot NAO garante ordem entre ApplicationRunners sem @Order.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AplicacaoCatalogoSeedRunner implements ApplicationRunner {

	private final AplicacaoRepository aplicacaoRepository;

	public AplicacaoCatalogoSeedRunner(AplicacaoRepository aplicacaoRepository) {
		this.aplicacaoRepository = aplicacaoRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		CodigoAplicacao.catalogoOrdenado().stream()
				.filter(CodigoAplicacao::isPersistidoNoCatalogoAtual)
				.forEach(this::seedSeAusente);
	}

	// Sem @Transactional proprio: cada chamada de AplicacaoRepository (findByCodigo,
	// save) ja e transacional por si so (proxy padrao do Spring Data); um
	// @Transactional aqui seria ineficaz mesmo assim, por autoinvocacao (run()
	// chama este metodo via "this", nunca pelo proxy do bean).
	private void seedSeAusente(CodigoAplicacao modulo) {
		if (aplicacaoRepository.findByCodigo(modulo.name()).isPresent()) {
			return;
		}
		aplicacaoRepository.save(new Aplicacao(modulo.name(), modulo.getNomeExibicao(),
				modulo.getDescricaoCurta(), StatusCadastro.ATIVO));
	}
}
