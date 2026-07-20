package br.app.criati.shared.validacao;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import br.app.criati.exception.DadosInvalidosException;

class SenhaValidadorTests {

	private final SenhaValidador validador = new SenhaValidador();

	@Test
	void deveAceitarSenhaValidaComConfirmacaoIgual() {
		assertThatCode(() -> validador.validar("senha-bastante-segura-123", "senha-bastante-segura-123"))
				.doesNotThrowAnyException();
	}

	@Test
	void deveRejeitarSenhaNula() {
		assertThatThrownBy(() -> validador.validar(null, null))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Senha e obrigatoria");
	}

	@Test
	void deveRejeitarSenhaVazia() {
		assertThatThrownBy(() -> validador.validar("   ", "   "))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Senha e obrigatoria");
	}

	@Test
	void deveRejeitarSenhaMenorQueTamanhoMinimo() {
		assertThatThrownBy(() -> validador.validar("curta123", "curta123"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Senha deve possuir no minimo 15 caracteres");
	}

	@Test
	void deveRejeitarSenhaMaiorQueTamanhoMaximo() {
		String senhaGigante = "a".repeat(73);
		assertThatThrownBy(() -> validador.validar(senhaGigante, senhaGigante))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Senha deve possuir no maximo 72 caracteres");
	}

	@Test
	void deveRejeitarQuandoConfirmacaoDivergente() {
		assertThatThrownBy(() -> validador.validar("senha-bastante-segura-123", "outra-senha-bastante-diferente"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Confirmacao de senha nao corresponde a senha");
	}
}
