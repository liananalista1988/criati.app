package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.usuario.model.Usuario;

/** Testes de dominio puros (sem Spring/JPA) para calculo de limites efetivos e ciclo de vida do cartao. */
class CartaoCreditoTests {

	private final Empresa empresa = new Empresa("Residencia Teste", "Residencia Teste", "11111111000177", StatusCadastro.ATIVO);
	private final Usuario autor = new Usuario("Autor Teste", "autor.cartao@criati.test", "hash", StatusCadastro.ATIVO);
	private final PessoaFinanceira titular = new PessoaFinanceira(empresa, "Pessoa", null, null, autor);
	private final InstituicaoFinanceira instituicao = new InstituicaoFinanceira(empresa, "Banco Teste", "999", autor);

	private CartaoCredito fisico(BigDecimal limiteTotal, BigDecimal limiteSaudavel, Integer fechamento, Integer vencimento) {
		return new CartaoCredito(empresa, titular, instituicao, "Cartao Fisico", TipoCartao.FISICO, null,
				Bandeira.VISA, "1234", limiteTotal, limiteSaudavel, fechamento, vencimento, null, autor);
	}

	private CartaoCredito virtual(CartaoCredito principal) {
		return new CartaoCredito(empresa, titular, instituicao, "Cartao Virtual", TipoCartao.VIRTUAL, principal,
				Bandeira.VISA, "5678", null, null, null, null, null, autor);
	}

	@Test
	void criaCartaoFisicoComCamposBasicos() {
		CartaoCredito c = fisico(new BigDecimal("5000.00"), new BigDecimal("2000.00"), 5, 12);
		assertThat(c.ehPrincipal()).isTrue();
		assertThat(c.ehVirtual()).isFalse();
		assertThat(c.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(c.isBloqueado()).isFalse();
		assertThat(c.getLimiteTotalEfetivo()).isEqualByComparingTo("5000.00");
		assertThat(c.getLimiteSaudavelEfetivo()).isEqualByComparingTo("2000.00");
		assertThat(c.getDiaFechamentoEfetivo()).isEqualTo(5);
		assertThat(c.getDiaVencimentoEfetivo()).isEqualTo(12);
	}

	@Test
	void criaCartaoVirtualReferenciandoPrincipal() {
		CartaoCredito principal = fisico(new BigDecimal("5000.00"), null, 5, 12);
		CartaoCredito v = virtual(principal);
		assertThat(v.ehVirtual()).isTrue();
		assertThat(v.ehPrincipal()).isFalse();
		assertThat(v.getCartaoPrincipal()).isSameAs(principal);
	}

	@Test
	void virtualNaoPossuiLimiteProprioEDelegaAoPrincipal() {
		CartaoCredito principal = fisico(new BigDecimal("5000.00"), new BigDecimal("3000.00"), 5, 12);
		CartaoCredito v = virtual(principal);
		assertThat(v.getLimiteTotal()).isNull();
		assertThat(v.getLimiteSaudavel()).isNull();
		assertThat(v.getLimiteTotalEfetivo()).isEqualByComparingTo("5000.00");
		assertThat(v.getLimiteSaudavelEfetivo()).isEqualByComparingTo("3000.00");
	}

	@Test
	void virtualDelegaFechamentoEVencimentoAoPrincipal() {
		CartaoCredito principal = fisico(new BigDecimal("5000.00"), null, 5, 12);
		CartaoCredito v = virtual(principal);
		assertThat(v.getDiaFechamento()).isNull();
		assertThat(v.getDiaVencimento()).isNull();
		assertThat(v.getDiaFechamentoEfetivo()).isEqualTo(5);
		assertThat(v.getDiaVencimentoEfetivo()).isEqualTo(12);
	}

	@Test
	void limiteComprometidoEfetivoEZeroPoisNaoExistemComprasNestaEntrega() {
		CartaoCredito c = fisico(new BigDecimal("5000.00"), null, 5, 12);
		assertThat(c.getLimiteComprometidoEfetivo()).isEqualByComparingTo("0.00");
	}

	@Test
	void limiteDisponivelEfetivoEIgualAoLimiteTotalSemComprasEValidoParaVirtualTambem() {
		CartaoCredito principal = fisico(new BigDecimal("5000.00"), null, 5, 12);
		CartaoCredito v = virtual(principal);
		assertThat(principal.getLimiteDisponivelEfetivo()).isEqualByComparingTo("5000.00");
		assertThat(v.getLimiteDisponivelEfetivo()).isEqualByComparingTo("5000.00");
	}

	@Test
	void bloquearPrincipalRefleteNoEfetivoDoVirtualSemAlterarCampoProprio() {
		CartaoCredito principal = fisico(new BigDecimal("5000.00"), null, 5, 12);
		CartaoCredito v = virtual(principal);
		assertThat(v.estaBloqueadoEfetivo()).isFalse();
		principal.bloquear("Suspeita de fraude", autor);
		assertThat(principal.isBloqueado()).isTrue();
		assertThat(v.isBloqueado()).isFalse();
		assertThat(v.estaBloqueadoEfetivo()).isTrue();
	}

	@Test
	void bloquearEDesbloquearAtualizamMotivoECampoProprio() {
		CartaoCredito c = fisico(new BigDecimal("1000.00"), null, 5, 12);
		c.bloquear("Perda do cartao", autor);
		assertThat(c.isBloqueado()).isTrue();
		assertThat(c.getMotivoBloqueio()).isEqualTo("Perda do cartao");
		c.desbloquear(autor);
		assertThat(c.isBloqueado()).isFalse();
		assertThat(c.getMotivoBloqueio()).isNull();
	}

	@Test
	void inativarEReativarAlteramStatus() {
		CartaoCredito c = fisico(new BigDecimal("1000.00"), null, 5, 12);
		c.inativar(autor);
		assertThat(c.getStatus()).isEqualTo(StatusCadastro.INATIVO);
		assertThat(c.estaAtivo()).isFalse();
		c.reativar(autor);
		assertThat(c.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(c.estaAtivo()).isTrue();
	}

	@Test
	void nomeNuloNaoEAceitoPelaEntidade() {
		assertThrows(NullPointerException.class, () -> new CartaoCredito(empresa, titular, instituicao, null,
				TipoCartao.FISICO, null, Bandeira.VISA, null, BigDecimal.TEN, null, 5, 12, null, autor));
	}

	@Test
	void titularNuloNaoEAceitoPelaEntidade() {
		assertThrows(NullPointerException.class, () -> new CartaoCredito(empresa, null, instituicao, "Cartao",
				TipoCartao.FISICO, null, Bandeira.VISA, null, BigDecimal.TEN, null, 5, 12, null, autor));
	}

	@Test
	void instituicaoNulaNaoEAceitaPelaEntidade() {
		assertThrows(NullPointerException.class, () -> new CartaoCredito(empresa, titular, null, "Cartao",
				TipoCartao.FISICO, null, Bandeira.VISA, null, BigDecimal.TEN, null, 5, 12, null, autor));
	}

	@Test
	void bandeiraNulaNaoEAceitaPelaEntidade() {
		assertThrows(NullPointerException.class, () -> new CartaoCredito(empresa, titular, instituicao, "Cartao",
				TipoCartao.FISICO, null, null, null, BigDecimal.TEN, null, 5, 12, null, autor));
	}
}
