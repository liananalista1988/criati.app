package br.app.criati.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.exception.DadosInvalidosException;

class OfxParserTests {

	private final OfxParser parser = new OfxParser();

	@Test
	void extraiOfxValidoComValoresPositivoNegativoEDadosOpcionais() {
		var transacoes = parser.parse(ofx("""
				<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260730120000[-3:BRT]<TRNAMT>-12.34<FITID>abc-1
				<NAME>Mercado<MEMO>Compra mensal<CHECKNUM>987</STMTTRN>
				<STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260731<TRNAMT>100.00<NAME>Depósito</STMTTRN>
				"""));

		assertThat(transacoes).hasSize(2);
		assertThat(transacoes.get(0).data()).isEqualTo(LocalDate.of(2026, 7, 30));
		assertThat(transacoes.get(0).valor()).isEqualByComparingTo("-12.34");
		assertThat(transacoes.get(0).descricao()).isEqualTo("Compra mensal");
		assertThat(transacoes.get(0).identificadorBancario()).isEqualTo("abc-1");
		assertThat(transacoes.get(0).documento()).isEqualTo("987");
		assertThat(transacoes.get(1).valor()).isEqualByComparingTo("100.00");
		assertThat(transacoes.get(1).identificadorBancario()).isNull();
	}

	@Test
	void aceitaEncodingWindows1252ECaracteresEspeciais() {
		String arquivo = "OFXHEADER:100\nENCODING:USASCII\nCHARSET:1252\n\n<OFX><BANKTRANLIST>"
				+ "<STMTTRN><DTPOSTED>20260731<TRNAMT>-1.00<MEMO>Café &amp; pão</STMTTRN>"
				+ "</BANKTRANLIST></OFX>";
		var transacao = parser.parse(arquivo.getBytes(Charset.forName("windows-1252"))).get(0);
		assertThat(transacao.descricao()).isEqualTo("Café & pão");
		assertThat(transacao.tipoBancario()).isEqualTo("DEBIT");
	}

	@Test
	void rejeitaConteudoInvalidoOuMalformado() {
		assertThatThrownBy(() -> parser.parse("nao e ofx".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> parser.parse("<OFX><BANKTRANLIST></BANKTRANLIST></OFX>".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> parser.parse(("<!DOCTYPE x><OFX><BANKTRANLIST>" + transacaoValida()
				+ "</BANKTRANLIST></OFX>").getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(DadosInvalidosException.class);
	}

	@Test
	void rejeitaDataAusenteOuInvalida() {
		assertThatThrownBy(() -> parser.parse(ofx("<STMTTRN><TRNAMT>-1.00</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("sem data");
		assertThatThrownBy(() -> parser.parse(ofx("<STMTTRN><DTPOSTED>20260230<TRNAMT>-1.00</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Data invalida");
	}

	@Test
	void rejeitaValorAusenteZeroOuInvalido() {
		assertThatThrownBy(() -> parser.parse(ofx("<STMTTRN><DTPOSTED>20260731</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("sem data ou valor");
		assertThatThrownBy(() -> parser.parse(ofx("<STMTTRN><DTPOSTED>20260731<TRNAMT>0</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Valor invalido");
		assertThatThrownBy(() -> parser.parse(ofx("<STMTTRN><DTPOSTED>20260731<TRNAMT>abc</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Valor invalido");
	}

	private byte[] ofx(String transacoes) {
		return ("OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKTRANLIST>" + transacoes
				+ "</BANKTRANLIST></OFX>").getBytes(StandardCharsets.UTF_8);
	}

	private String transacaoValida() {
		return "<STMTTRN><DTPOSTED>20260731<TRNAMT>-1.00</STMTTRN>";
	}
}
