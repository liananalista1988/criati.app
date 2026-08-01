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
	void perfilBancoBrasilIgnoraSaldosInformativosComDataInvalidaEFitidVazio() {
		var transacoes = parser.parse(ofxBb("""
				<STMTTRN><DTPOSTED>DATA-INVALIDA<TRNAMT>0<FITID><NAME>Saldo Anterior</STMTTRN>
				<STMTTRN><DTPOSTED>INVALIDA<TRNAMT>0<MEMO>Saldo do dia</STMTTRN>
				<STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260801120000[-3:BRT]<TRNAMT>25.50
				<FITID>BB-SAN-1<NAME>PIX<MEMO>Recebimento sanitizado</STMTTRN>
				"""));

		assertThat(transacoes).singleElement().satisfies(transacao -> {
			assertThat(transacao.data()).isEqualTo(LocalDate.of(2026, 8, 1));
			assertThat(transacao.valor()).isEqualByComparingTo("25.50");
			assertThat(transacao.identificadorBancario()).isEqualTo("BB-SAN-1");
			assertThat(transacao.descricao()).isEqualTo("PIX - Recebimento sanitizado");
		});
	}

	@Test
	void perfilBancoBrasilPreservaPixTarifaJurosIofESinais() {
		var transacoes = parser.parse(ofxBb("""
				<STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260804<TRNAMT>100.00<FITID>BB-PIX
				<NAME>PIX<MEMO>Credito sanitizado</STMTTRN>
				<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260803<TRNAMT>-10.00<FITID>BB-TARIFA
				<NAME>Tarifa<MEMO>Servico sanitizado</STMTTRN>
				<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260802<TRNAMT>-2.50<FITID>BB-JUROS
				<NAME>Juros<MEMO>Encargo sanitizado</STMTTRN>
				<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260801<TRNAMT>-1.25<FITID>BB-IOF
				<NAME>IOF<MEMO>Tributo sanitizado</STMTTRN>
				"""));

		assertThat(transacoes).extracting(TransacaoBancariaExtraida::valor)
				.containsExactly(new java.math.BigDecimal("100.00"), new java.math.BigDecimal("-10.00"),
						new java.math.BigDecimal("-2.50"), new java.math.BigDecimal("-1.25"));
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::tipoBancario)
				.containsExactly("CREDIT", "DEBIT", "DEBIT", "DEBIT");
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::descricao)
				.containsExactly("PIX - Credito sanitizado", "Tarifa - Servico sanitizado",
						"Juros - Encargo sanitizado", "IOF - Tributo sanitizado");
	}

	@Test
	void perfilBancoBrasilExigeDataEFitidNasMovimentacoesReais() {
		assertThatThrownBy(() -> parser.parse(ofxBb(
				"<STMTTRN><DTPOSTED>20260230<TRNAMT>-1.00<FITID>BB-DATA<MEMO>Tarifa</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Data invalida");
		assertThatThrownBy(() -> parser.parse(ofxBb(
				"<STMTTRN><DTPOSTED>20260801<TRNAMT>-1.00<MEMO>Tarifa</STMTTRN>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("sem FITID");
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

	private byte[] ofxBb(String transacoes) {
		return ("OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKACCTFROM><BANKID>001</BANKACCTFROM>"
				+ "<BANKTRANLIST><DTSTART>INVALIDO<DTEND>INVALIDO" + transacoes
				+ "</BANKTRANLIST></OFX>").getBytes(StandardCharsets.UTF_8);
	}

	private String transacaoValida() {
		return "<STMTTRN><DTPOSTED>20260731<TRNAMT>-1.00</STMTTRN>";
	}
}
