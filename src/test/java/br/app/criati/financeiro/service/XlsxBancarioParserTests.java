package br.app.criati.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import br.app.criati.exception.DadosInvalidosException;

class XlsxBancarioParserTests {

	private final XlsxBancarioParser parser = new XlsxBancarioParser(3, 3, 8, 100, 100, 4_194_304);

	@Test
	void aceitaXlsxValidoComCabecalhosNormalizadosDataExcelEntradaESaida() throws Exception {
		byte[] arquivo = workbookValido();

		var transacoes = parser.parse(arquivo);

		assertThat(transacoes).hasSize(2);
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::data)
				.containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::valor)
				.containsExactly(new BigDecimal("100.50"), new BigDecimal("-25.30"));
		assertThat(transacoes).extracting(TransacaoBancariaExtraida::tipoBancario)
				.containsExactly("CREDIT", "DEBIT");
		assertThat(transacoes.get(0).documento()).isEqualTo("DOC-1");
		assertThat(transacoes.get(0).identificadorBancario()).isEqualTo("ID-1");
	}

	@Test
	void usaPrimeiraPlanilhaComCabecalhosValidos() throws Exception {
		byte[] arquivo;
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			workbook.createSheet("Instrucoes").createRow(0).createCell(0).setCellValue("Leia antes de importar");
			preencherValido(workbook, workbook.createSheet("Movimentos"));
			arquivo = escrever(workbook);
		}

		assertThat(parser.parse(arquivo)).hasSize(2);
	}

	@Test
	void rejeitaArquivoVazioMalformadoRenomeadoECriptografado() throws Exception {
		assertThatThrownBy(() -> parser.parse(new byte[0]))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("vazio");
		assertThatThrownBy(() -> parser.parse(new byte[] {0x50, 0x4b, 0x03, 0x04, 1, 2, 3}))
				.isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> parser.parse("data,descricao,valor".getBytes()))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("formato XLSX");
		assertThatThrownBy(() -> parser.parse(criptografar(workbookValido())))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("criptografado");
	}

	@Test
	void rejeitaFormulaHyperlinkEPlanilhaOculta() throws Exception {
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(1).getCell(1).setCellFormula("CONCAT(\"A\",\"B\")"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("formula");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) -> {
			CreationHelper helper = workbook.getCreationHelper();
			var hyperlink = helper.createHyperlink(HyperlinkType.URL);
			hyperlink.setAddress("https://exemplo.invalid/coleta");
			sheet.getRow(1).getCell(1).setHyperlink(hyperlink);
		}))).isInstanceOf(DadosInvalidosException.class)
				.hasMessageMatching(".*(hyperlink|referencia externa).*");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				workbook.setSheetHidden(workbook.getSheetIndex(sheet), true))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("ocultas");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) -> {
			Sheet posterior = workbook.createSheet("Posterior");
			posterior.createRow(0).createCell(0).setCellFormula("1+1");
		}))).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("formula");
	}

	@Test
	void rejeitaQuantidadeExcessivaDePlanilhasLinhasColunasECelula() throws Exception {
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) -> {
			workbook.createSheet("Dois");
			workbook.createSheet("Tres");
			workbook.createSheet("Quatro");
		}))).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("planilhas");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) -> {
			Row extra1 = sheet.createRow(3);
			preencherTransacao(extra1, "2026-08-03", "C", 3, "+", "DOC-3", "ID-3");
			Row extra2 = sheet.createRow(4);
			preencherTransacao(extra2, "2026-08-04", "D", 4, "+", "DOC-4", "ID-4");
		}))).isInstanceOf(DadosInvalidosException.class).hasMessageContaining("linhas");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(0).createCell(8).setCellValue("Excesso"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("colunas");

		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(1).getCell(1).setCellValue("x".repeat(101)))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("grande");
	}

	@Test
	void rejeitaDataValorCabecalhosAusentesOuAmbiguos() throws Exception {
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(1).getCell(0).setCellValue("31/02/2026"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Data invalida");
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(1).getCell(2).setCellValue("1.234"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("Valor invalido");
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(0).getCell(2).setCellValue("Coluna desconhecida"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("cabecalhos univocos");
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(0).createCell(6).setCellValue("Amount"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("ambiguas");
	}

	@Test
	void rejeitaTextoComPrefixoDeFormulaMesmoSemFormulaExcel() throws Exception {
		assertThatThrownBy(() -> parser.parse(modificarValido((workbook, sheet) ->
				sheet.getRow(1).getCell(1).setCellValue("=WEBSERVICE(\"https://exemplo.invalid\")"))))
				.isInstanceOf(DadosInvalidosException.class).hasMessageContaining("formula potencialmente insegura");
	}

	private byte[] workbookValido() throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			preencherValido(workbook, workbook.createSheet("Movimentos"));
			return escrever(workbook);
		}
	}

	private byte[] modificarValido(AjusteWorkbook ajuste) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookValido()))) {
			Sheet sheet = workbook.getSheetAt(0);
			ajuste.aplicar(workbook, sheet);
			return escrever(workbook);
		}
	}

	private void preencherValido(Workbook workbook, Sheet sheet) {
		Row cabecalho = sheet.createRow(0);
		cabecalho.createCell(0).setCellValue("Dáta Movimento");
		cabecalho.createCell(1).setCellValue("Descrição");
		cabecalho.createCell(2).setCellValue("Montante");
		cabecalho.createCell(3).setCellValue("Sinal");
		cabecalho.createCell(4).setCellValue("Número Documento");
		cabecalho.createCell(5).setCellValue("Identificador Bancário");

		Row entrada = sheet.createRow(1);
		entrada.createCell(0).setCellValue(LocalDate.of(2026, 8, 1));
		CellStyle data = workbook.createCellStyle();
		data.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));
		entrada.getCell(0).setCellStyle(data);
		entrada.createCell(1).setCellValue("Crédito café");
		entrada.createCell(2).setCellValue(100.50);
		entrada.createCell(3).setCellValue("+");
		entrada.createCell(4).setCellValue("DOC-1");
		entrada.createCell(5).setCellValue("ID-1");

		Row saida = sheet.createRow(2);
		preencherTransacao(saida, "02/08/2026", "Mercado", 25.30, "-", "DOC-2", "ID-2");
	}

	private static void preencherTransacao(Row row, String data, String descricao, double valor, String sinal,
			String documento, String identificador) {
		row.createCell(0).setCellValue(data);
		row.createCell(1).setCellValue(descricao);
		row.createCell(2).setCellValue(valor);
		row.createCell(3).setCellValue(sinal);
		row.createCell(4).setCellValue(documento);
		row.createCell(5).setCellValue(identificador);
	}

	private byte[] escrever(Workbook workbook) throws IOException {
		ByteArrayOutputStream saida = new ByteArrayOutputStream();
		workbook.write(saida);
		return saida.toByteArray();
	}

	private byte[] criptografar(byte[] xlsx) throws Exception {
		try (POIFSFileSystem sistema = new POIFSFileSystem();
				OPCPackage pacote = OPCPackage.open(new ByteArrayInputStream(xlsx))) {
			EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile);
			Encryptor encryptor = info.getEncryptor();
			encryptor.confirmPassword("senha-teste");
			try (OutputStream dados = encryptor.getDataStream(sistema)) {
				pacote.save(dados);
			}
			ByteArrayOutputStream saida = new ByteArrayOutputStream();
			sistema.writeFilesystem(saida);
			return saida.toByteArray();
		}
	}

	@FunctionalInterface
	private interface AjusteWorkbook {
		void aplicar(XSSFWorkbook workbook, Sheet sheet) throws Exception;
	}
}
