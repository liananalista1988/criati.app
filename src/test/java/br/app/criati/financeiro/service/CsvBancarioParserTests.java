package br.app.criati.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import br.app.criati.exception.DadosInvalidosException;

class CsvBancarioParserTests {

	private final CsvBancarioParser parser = new CsvBancarioParser(3, 8, 100);

	@Test
	void aceitaVirgulaEValoresAssinados() {
		var transacoes = parser.parse(bytes("data,descricao,valor\n2026-08-01,Salario,100.50\n01/08/2026,Mercado,-25.50\n"));

		assertThat(transacoes).hasSize(2);
		assertThat(transacoes.get(0).valor()).isEqualByComparingTo("100.50");
		assertThat(transacoes.get(0).tipoBancario()).isEqualTo("CREDIT");
		assertThat(transacoes.get(1).valor()).isEqualByComparingTo("-25.50");
		assertThat(transacoes.get(1).tipoBancario()).isEqualTo("DEBIT");
	}

	@Test
	void aceitaColunaDeSinalExplicita() {
		var transacoes = parser.parse(bytes("data,descricao,valor,sinal\n"
				+ "2026-08-01,Entrada,10,+\n2026-08-02,Saida,7,-\n"));

		assertThat(transacoes).extracting(TransacaoBancariaExtraida::valor)
				.containsExactly(new BigDecimal("10.00"), new BigDecimal("-7.00"));
	}

	@Test
	void aceitaPontoEVirgulaUtf8BomTipoDocumentoEIdentificador() {
		byte[] texto = bytes("data;descrição;valor;tipo;documento;identificador bancário\n"
				+ "01-08-2026;Crédito café;10,25;entrada;DOC-1;ID-1\n"
				+ "20260801;Débito mercado;7,30;saída;DOC-2;ID-2\n");
		byte[] comBom = new byte[texto.length + 3];
		comBom[0] = (byte) 0xEF;
		comBom[1] = (byte) 0xBB;
		comBom[2] = (byte) 0xBF;
		System.arraycopy(texto, 0, comBom, 3, texto.length);

		var transacoes = parser.parse(comBom);

		assertThat(transacoes).extracting(TransacaoBancariaExtraida::descricao)
				.containsExactly("Crédito café", "Débito mercado");
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::valor)
				.containsExactly(new BigDecimal("10.25"), new BigDecimal("-7.30"));
		assertThat(transacoes.get(0).documento()).isEqualTo("DOC-1");
		assertThat(transacoes.get(0).identificadorBancario()).isEqualTo("ID-1");
	}

	@Test
	void aceitaCsvInterComPreambuloCabecalhoRealValoresBrasileirosEOrdemDecrescente() {
		CsvBancarioParser parserInter = new CsvBancarioParser(10, 8, 100);
		String csv = "Extrato sanitizado;sem dados bancarios\n"
				+ "Periodo consultado;informacao ignorada\n"
				+ "Data Lançamento;Histórico;Descrição;Valor;Saldo\n"
				+ "03/08/2026;PIX recebido;Origem sanitizada;1.234,56;9.999,99\n"
				+ "02/08/2026;Tarifa;Pacote mensal;-12,34;9.987,65\n";

		var transacoes = parserInter.parse(bytes(csv));

		assertThat(transacoes).hasSize(2);
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::data)
				.containsExactly(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2));
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::valor)
				.containsExactly(new BigDecimal("1234.56"), new BigDecimal("-12.34"));
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::descricao)
				.containsExactly("PIX recebido - Origem sanitizada", "Tarifa - Pacote mensal");
	}

	@Test
	void aceitaCsvInterSemPreambuloENaoImportaSaldoComoTransacao() {
		String csv = "Data Lançamento;Histórico;Descrição;Valor;Saldo\n"
				+ "01/08/2026;Rendimento;Credito sanitizado;10,00;1.010,00\n";

		var transacoes = parser.parse(bytes(csv));

		assertThat(transacoes).singleElement().satisfies(transacao -> {
			assertThat(transacao.valor()).isEqualByComparingTo("10.00");
			assertThat(transacao.descricao()).isEqualTo("Rendimento - Credito sanitizado");
		});
	}

	@Test
	void rejeitaCabecalhosAusentesOuAmbiguos() {
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao\n2026-08-01,Teste\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("cabecalhos univocos");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor,amount\n2026-08-01,Teste,10,10\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("ambiguas");
	}

	@Test
	void rejeitaDataValorTipoEFormulaInvalidos() {
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n31/02/2026,Teste,10\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Data invalida");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n2026-08-01,Teste,1.234\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Valor invalido");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor,tipo\n2026-08-01,Teste,10,incerto\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Tipo invalido");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n2026-08-01,=2+2,10\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("formula");
	}

	@Test
	void rejeitaEstruturaOuConteudoExcessivoECaracteresInvalidos() {
		assertThatThrownBy(() -> parser.parse(bytes(""))).isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n2026-08-01,A,1\n"
				+ "2026-08-02,B,2\n2026-08-03,C,3\n2026-08-04,D,4\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("linhas");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor,a,b,c,d,e,f\n"
				+ "2026-08-01,A,1,2,3,4,5,6,7\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("colunas");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n2026-08-01," + "x".repeat(101) + ",1\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("longo");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n2026-08-01,A\u0000B,1\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("caracteres invalidos");
		assertThatThrownBy(() -> parser.parse(bytes("data,descricao,valor\n\n\n\n2026-08-01,A,1\n")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("linhas");
	}

	@Test
	void rejeitaUtf8InvalidoEConteudoDisfarcado() {
		assertThatThrownBy(() -> parser.parse(new byte[] {(byte) 0xC3, (byte) 0x28}))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("UTF-8");
		assertThatThrownBy(() -> parser.parse(bytes("<OFX><BANKTRANLIST></BANKTRANLIST></OFX>")))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("cabecalhos");
	}

	private byte[] bytes(String valor) {
		return valor.getBytes(StandardCharsets.UTF_8);
	}
}
