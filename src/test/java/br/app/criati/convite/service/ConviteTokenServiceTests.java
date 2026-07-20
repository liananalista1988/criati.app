package br.app.criati.convite.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConviteTokenServiceTests {

	private final ConviteTokenService service = new ConviteTokenService();

	@Test
	void deveGerarTokensDiferentesEmChamadasSucessivas() {
		String token1 = service.gerarTokenBruto();
		String token2 = service.gerarTokenBruto();

		assertThat(token1).isNotEqualTo(token2);
		assertThat(token1).hasSizeGreaterThanOrEqualTo(40);
	}

	@Test
	void deveGerarHashesDiferentesParaTokensDiferentes() {
		String hash1 = service.calcularHash(service.gerarTokenBruto());
		String hash2 = service.calcularHash(service.gerarTokenBruto());

		assertThat(hash1).isNotEqualTo(hash2);
	}

	@Test
	void deveGerarHashDeterministicoParaOMesmoToken() {
		String token = service.gerarTokenBruto();

		assertThat(service.calcularHash(token)).isEqualTo(service.calcularHash(token));
	}

	@Test
	void hashNaoDeveConterOTokenBrutoNemSerIgualAEle() {
		String token = service.gerarTokenBruto();
		String hash = service.calcularHash(token);

		assertThat(hash).isNotEqualTo(token);
		assertThat(hash).doesNotContain(token);
		assertThat(hash).hasSize(64);
	}
}
