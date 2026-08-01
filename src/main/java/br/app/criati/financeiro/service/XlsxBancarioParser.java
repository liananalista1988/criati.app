package br.app.criati.financeiro.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.app.criati.exception.DadosInvalidosException;

@Component
public class XlsxBancarioParser {

	private static final Pattern VALOR_SEGURO = Pattern.compile("[+-]?\\d{1,17}(?:[.,]\\d{1,2})?");
	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/uuuu")
			.withResolverStyle(ResolverStyle.STRICT);
	private static final DateTimeFormatter DATA_BR_HIFEN = DateTimeFormatter.ofPattern("dd-MM-uuuu")
			.withResolverStyle(ResolverStyle.STRICT);
	private static final Set<String> ENTRADAS = Set.of("entrada", "receita", "credito", "credit", "c", "+");
	private static final Set<String> SAIDAS = Set.of("saida", "despesa", "debito", "debit", "d", "-");
	private static final Map<String, Coluna> CABECALHOS = criarCabecalhos();

	private final int maximoPlanilhas;
	private final int maximoLinhas;
	private final int maximoColunas;
	private final int maximoCaracteresCelula;
	private final int maximoEntradasZip;
	private final long maximoBytesDescompactados;

	public XlsxBancarioParser(
			@Value("${criati.financeiro.importacao-xlsx.maximo-planilhas:5}") int maximoPlanilhas,
			@Value("${criati.financeiro.importacao-xlsx.maximo-linhas:10000}") int maximoLinhas,
			@Value("${criati.financeiro.importacao-xlsx.maximo-colunas:50}") int maximoColunas,
			@Value("${criati.financeiro.importacao-xlsx.maximo-caracteres-celula:500}") int maximoCaracteresCelula,
			@Value("${criati.financeiro.importacao-xlsx.maximo-entradas-zip:200}") int maximoEntradasZip,
			@Value("${criati.financeiro.importacao-xlsx.maximo-bytes-descompactados:16777216}")
			long maximoBytesDescompactados) {
		if (maximoPlanilhas <= 0 || maximoLinhas <= 0 || maximoColunas < 3 || maximoCaracteresCelula <= 0
				|| maximoEntradasZip <= 0 || maximoBytesDescompactados <= 0) {
			throw new IllegalArgumentException("Limites da importacao XLSX devem ser positivos");
		}
		this.maximoPlanilhas = maximoPlanilhas;
		this.maximoLinhas = maximoLinhas;
		this.maximoColunas = maximoColunas;
		this.maximoCaracteresCelula = maximoCaracteresCelula;
		this.maximoEntradasZip = maximoEntradasZip;
		this.maximoBytesDescompactados = maximoBytesDescompactados;
		ZipSecureFile.setMinInflateRatio(0.01d);
		ZipSecureFile.setMaxEntrySize(maximoBytesDescompactados);
		ZipSecureFile.setMaxTextSize(maximoBytesDescompactados);
	}

	public List<TransacaoBancariaExtraida> parse(byte[] conteudo) {
		if (conteudo == null || conteudo.length == 0) {
			throw new DadosInvalidosException("Arquivo XLSX esta vazio");
		}
		validarAssinatura(conteudo);
		validarEstruturaZip(conteudo);
		try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(conteudo))) {
			validarWorkbook(workbook);
			Candidato primeiroValido = null;
			for (int indice = 0; indice < workbook.getNumberOfSheets(); indice++) {
				Sheet planilha = workbook.getSheetAt(indice);
				Candidato candidato = analisarPlanilha(planilha);
				if (primeiroValido == null && candidato != null) {
					primeiroValido = candidato;
				}
			}
			if (primeiroValido != null) {
				return extrair(primeiroValido);
			}
			throw new DadosInvalidosException(
					"XLSX deve possuir cabecalhos univocos para data, descricao e valor");
		} catch (DadosInvalidosException excecao) {
			throw excecao;
		} catch (EncryptedDocumentException excecao) {
			throw new DadosInvalidosException("Arquivo XLSX criptografado nao e aceito");
		} catch (IOException | InvalidFormatException | RuntimeException excecao) {
			throw new DadosInvalidosException("Arquivo XLSX esta malformado ou nao e um XLSX valido");
		}
	}

	private void validarAssinatura(byte[] conteudo) {
		try {
			FileMagic formato = FileMagic.valueOf(new ByteArrayInputStream(conteudo));
			if (formato == FileMagic.OLE2) {
				throw new DadosInvalidosException(
						"Arquivo XLSX criptografado ou formato legado .xls nao e aceito");
			}
			if (formato != FileMagic.OOXML) {
				throw new DadosInvalidosException("Conteudo do arquivo nao corresponde ao formato XLSX");
			}
		} catch (IOException excecao) {
			throw new DadosInvalidosException("Nao foi possivel validar o formato XLSX");
		}
	}

	private void validarEstruturaZip(byte[] conteudo) {
		int entradas = 0;
		long total = 0;
		boolean possuiContentTypes = false;
		boolean possuiWorkbook = false;
		Set<String> nomesEntradas = new HashSet<>();
		byte[] buffer = new byte[8192];
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(conteudo))) {
			ZipEntry entrada;
			while ((entrada = zip.getNextEntry()) != null) {
				entradas++;
				if (entradas > maximoEntradasZip) {
					throw new DadosInvalidosException("XLSX excede a quantidade maxima de entradas ZIP");
				}
				String nome = entrada.getName();
				validarNomeEntrada(nome);
				if (!nomesEntradas.add(nome)) {
					throw new DadosInvalidosException("XLSX possui entradas ZIP duplicadas");
				}
				validarConteudoAtivo(nome);
				possuiContentTypes |= "[Content_Types].xml".equals(nome);
				possuiWorkbook |= "xl/workbook.xml".equals(nome);
				long tamanhoEntrada = 0;
				int lidos;
				while ((lidos = zip.read(buffer)) != -1) {
					tamanhoEntrada += lidos;
					total += lidos;
					if (tamanhoEntrada > maximoBytesDescompactados || total > maximoBytesDescompactados) {
						throw new DadosInvalidosException("XLSX excede o limite de conteudo descompactado");
					}
				}
				long compactado = entrada.getCompressedSize();
				if (tamanhoEntrada > 100_000 && compactado > 0
						&& (double) compactado / (double) tamanhoEntrada < 0.01d) {
					throw new DadosInvalidosException("XLSX possui taxa de compressao insegura");
				}
			}
		} catch (DadosInvalidosException excecao) {
			throw excecao;
		} catch (IOException | RuntimeException excecao) {
			throw new DadosInvalidosException("Estrutura ZIP do arquivo XLSX e invalida");
		}
		if (!possuiContentTypes || !possuiWorkbook) {
			throw new DadosInvalidosException("Conteudo do arquivo nao corresponde ao formato XLSX");
		}
	}

	private void validarNomeEntrada(String nome) {
		if (nome == null || nome.isBlank() || nome.startsWith("/") || nome.startsWith("\\")
				|| nome.contains("\\") || nome.contains(":")
				|| List.of(nome.split("/", -1)).contains("..")) {
			throw new DadosInvalidosException("XLSX possui caminho interno inseguro");
		}
	}

	private void validarConteudoAtivo(String nome) {
		String normalizado = nome.toLowerCase(Locale.ROOT);
		if (normalizado.endsWith("vbaproject.bin") || normalizado.startsWith("xl/activex/")
				|| normalizado.startsWith("xl/embeddings/")) {
			throw new DadosInvalidosException("XLSX possui conteudo ativo ou embutido nao permitido");
		}
	}

	private void validarWorkbook(XSSFWorkbook workbook) throws InvalidFormatException {
		int quantidade = workbook.getNumberOfSheets();
		if (quantidade == 0) {
			throw new DadosInvalidosException("Arquivo XLSX nao possui planilhas");
		}
		if (quantidade > maximoPlanilhas) {
			throw new DadosInvalidosException("XLSX excede a quantidade maxima de planilhas");
		}
		for (int indice = 0; indice < quantidade; indice++) {
			if (workbook.isSheetHidden(indice) || workbook.isSheetVeryHidden(indice)) {
				throw new DadosInvalidosException("XLSX nao permite planilhas ocultas");
			}
		}
		for (PackagePart parte : workbook.getPackage().getParts()) {
			if (parte.isRelationshipPart()) {
				continue;
			}
			for (PackageRelationship relacionamento : parte.getRelationships()) {
				if (relacionamento.getTargetMode() == TargetMode.EXTERNAL) {
					throw new DadosInvalidosException("XLSX possui referencia externa insegura");
				}
			}
		}
	}

	private Candidato analisarPlanilha(Sheet planilha) {
		if (planilha.getLastRowNum() + 1 > maximoLinhas + 1) {
			throw new DadosInvalidosException("XLSX excede a quantidade maxima de linhas");
		}
		List<Linha> linhas = new ArrayList<>();
		for (Row row : planilha) {
			if (row.getRowNum() > maximoLinhas) {
				throw new DadosInvalidosException("XLSX excede a quantidade maxima de linhas");
			}
			short ultimaCelula = row.getLastCellNum();
			if (ultimaCelula > maximoColunas) {
				throw new DadosInvalidosException("XLSX excede a quantidade maxima de colunas");
			}
			List<ValorCelula> valores = new ArrayList<>();
			for (int coluna = 0; coluna < Math.max(0, ultimaCelula); coluna++) {
				valores.add(lerCelula(row.getCell(coluna)));
			}
			if (valores.stream().noneMatch(valor -> !valor.texto().isBlank())) {
				continue;
			}
			linhas.add(new Linha(row.getRowNum(), List.copyOf(valores)));
			if (linhas.size() > maximoLinhas + 1) {
				throw new DadosInvalidosException("XLSX excede a quantidade maxima de linhas");
			}
		}
		if (linhas.isEmpty()) {
			return null;
		}
		Map<Coluna, Integer> indices = mapearCabecalho(linhas.get(0).valores());
		return indices == null ? null : new Candidato(linhas, indices);
	}

	private ValorCelula lerCelula(Cell celula) {
		if (celula == null || celula.getCellType() == CellType.BLANK) {
			return ValorCelula.vazio();
		}
		if (celula.getCellType() == CellType.FORMULA) {
			throw new DadosInvalidosException("XLSX possui formula e nao pode ser importado");
		}
		if (celula.getHyperlink() != null) {
			throw new DadosInvalidosException("XLSX possui hyperlink e nao pode ser importado");
		}
		String texto;
		LocalDate data = null;
		switch (celula.getCellType()) {
			case STRING -> texto = celula.getStringCellValue();
			case NUMERIC -> {
				if (DateUtil.isCellDateFormatted(celula)) {
					data = celula.getLocalDateTimeCellValue().toLocalDate();
					texto = data.toString();
				} else {
					texto = NumberToTextConverter.toText(celula.getNumericCellValue());
				}
			}
			case BOOLEAN -> texto = Boolean.toString(celula.getBooleanCellValue());
			case ERROR -> throw new DadosInvalidosException("XLSX possui celula com erro");
			default -> throw new DadosInvalidosException("XLSX possui tipo de celula nao suportado");
		}
		if (texto.length() > maximoCaracteresCelula) {
			throw new DadosInvalidosException("XLSX possui celula excessivamente grande");
		}
		if (texto.chars().anyMatch(c -> c == 0 || Character.isISOControl(c) && c != '\t')) {
			throw new DadosInvalidosException("XLSX possui caracteres invalidos");
		}
		return new ValorCelula(texto, data);
	}

	private Map<Coluna, Integer> mapearCabecalho(List<ValorCelula> cabecalho) {
		Map<Coluna, Integer> indices = new HashMap<>();
		Set<String> nomes = new HashSet<>();
		boolean reconheceuAlguma = false;
		for (int indice = 0; indice < cabecalho.size(); indice++) {
			String original = cabecalho.get(indice).texto();
			validarFormulaTexto(original, false);
			String normalizado = normalizarCabecalho(original);
			if (normalizado.isEmpty()) {
				continue;
			}
			if (!nomes.add(normalizado)) {
				throw new DadosInvalidosException("XLSX possui cabecalhos duplicados");
			}
			Coluna coluna = CABECALHOS.get(normalizado);
			if (coluna != null) {
				reconheceuAlguma = true;
				if (indices.putIfAbsent(coluna, indice) != null) {
					throw new DadosInvalidosException("XLSX possui colunas ambiguas para " + coluna.rotulo);
				}
			}
		}
		if (!indices.keySet().containsAll(Set.of(Coluna.DATA, Coluna.DESCRICAO, Coluna.VALOR))) {
			if (reconheceuAlguma) {
				throw new DadosInvalidosException(
						"XLSX deve possuir cabecalhos univocos para data, descricao e valor");
			}
			return null;
		}
		return Map.copyOf(indices);
	}

	private List<TransacaoBancariaExtraida> extrair(Candidato candidato) {
		List<TransacaoBancariaExtraida> transacoes = new ArrayList<>();
		for (int indiceLinha = 1; indiceLinha < candidato.linhas().size(); indiceLinha++) {
			Linha linha = candidato.linhas().get(indiceLinha);
			ValorCelula dataCelula = campo(linha, candidato.indices(), Coluna.DATA);
			String descricao = normalizarObrigatorio(campo(linha, candidato.indices(), Coluna.DESCRICAO).texto(),
					"Descricao XLSX obrigatoria");
			String valorBruto = campo(linha, candidato.indices(), Coluna.VALOR).texto().trim();
			validarFormulaTexto(dataCelula.texto(), false);
			validarFormulaTexto(descricao, false);
			BigDecimal valor = parseValor(valorBruto);
			String tipo = textoOpcional(linha, candidato.indices(), Coluna.TIPO);
			if (tipo != null) {
				validarFormulaTexto(tipo, true);
				valor = aplicarTipo(valor, tipo);
			}
			String documento = limitar(normalizarOpcional(textoOpcional(linha, candidato.indices(), Coluna.DOCUMENTO)),
					100, "Documento XLSX excede o limite de 100 caracteres");
			String identificador = limitar(
					normalizarOpcional(textoOpcional(linha, candidato.indices(), Coluna.IDENTIFICADOR)), 150,
					"Identificador bancario XLSX excede o limite de 150 caracteres");
			validarFormulaTexto(documento, false);
			validarFormulaTexto(identificador, false);
			LocalDate data = dataCelula.data() != null ? dataCelula.data() : parseData(dataCelula.texto());
			transacoes.add(new TransacaoBancariaExtraida(data, valor,
					valor.signum() < 0 ? "DEBIT" : "CREDIT", descricao, identificador, documento));
		}
		if (transacoes.isEmpty()) {
			throw new DadosInvalidosException("Planilha XLSX nao possui transacoes bancarias");
		}
		return List.copyOf(transacoes);
	}

	private ValorCelula campo(Linha linha, Map<Coluna, Integer> indices, Coluna coluna) {
		int indice = indices.get(coluna);
		return indice < linha.valores().size() ? linha.valores().get(indice) : ValorCelula.vazio();
	}

	private String textoOpcional(Linha linha, Map<Coluna, Integer> indices, Coluna coluna) {
		Integer indice = indices.get(coluna);
		return indice == null || indice >= linha.valores().size() ? null : linha.valores().get(indice).texto();
	}

	private LocalDate parseData(String valor) {
		String limpa = valor.trim();
		for (DateTimeFormatter formato : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DATA_BR, DATA_BR_HIFEN,
				DateTimeFormatter.BASIC_ISO_DATE)) {
			try {
				return LocalDate.parse(limpa, formato);
			} catch (DateTimeParseException ignorada) {
				// Somente formatos explicitos e nao ambiguos.
			}
		}
		throw new DadosInvalidosException("Data invalida em transacao XLSX");
	}

	private BigDecimal parseValor(String valor) {
		if (!VALOR_SEGURO.matcher(valor).matches()) {
			throw new DadosInvalidosException("Valor invalido em transacao XLSX");
		}
		try {
			BigDecimal resultado = new BigDecimal(valor.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
			if (resultado.signum() == 0 || resultado.precision() > 19) {
				throw new ArithmeticException();
			}
			return resultado;
		} catch (ArithmeticException | NumberFormatException excecao) {
			throw new DadosInvalidosException("Valor invalido em transacao XLSX");
		}
	}

	private BigDecimal aplicarTipo(BigDecimal valor, String tipo) {
		String tipoLimpo = tipo.trim();
		String normalizado = tipoLimpo.equals("+") || tipoLimpo.equals("-")
				? tipoLimpo : normalizarCabecalho(tipoLimpo);
		if (ENTRADAS.contains(normalizado)) {
			if (valor.signum() < 0) {
				throw new DadosInvalidosException("Tipo e sinal do valor XLSX sao incompativeis");
			}
			return valor.abs();
		}
		if (SAIDAS.contains(normalizado)) {
			return valor.abs().negate();
		}
		throw new DadosInvalidosException("Tipo invalido em transacao XLSX");
	}

	private void validarFormulaTexto(String valor, boolean permiteSinal) {
		if (valor == null) {
			return;
		}
		String limpa = valor.stripLeading();
		if (permiteSinal && (limpa.equals("+") || limpa.equals("-"))) {
			return;
		}
		if (!limpa.isEmpty() && "=+-@".indexOf(limpa.charAt(0)) >= 0) {
			throw new DadosInvalidosException("XLSX possui texto com formula potencialmente insegura");
		}
	}

	private String normalizarObrigatorio(String valor, String mensagem) {
		String normalizado = normalizarOpcional(valor);
		if (normalizado == null) {
			throw new DadosInvalidosException(mensagem);
		}
		return normalizado;
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim().replaceAll("\\s+", " ");
	}

	private String limitar(String valor, int limite, String mensagem) {
		if (valor != null && valor.length() > limite) {
			throw new DadosInvalidosException(mensagem);
		}
		return valor;
	}

	private String normalizarCabecalho(String valor) {
		String semAcentos = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		return semAcentos.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	private static Map<String, Coluna> criarCabecalhos() {
		Map<String, Coluna> cabecalhos = new HashMap<>();
		adicionar(cabecalhos, Coluna.DATA, "data", "data_transacao", "data_movimento", "data_lancamento", "date");
		adicionar(cabecalhos, Coluna.DESCRICAO, "descricao", "historico", "memo", "description", "nome");
		adicionar(cabecalhos, Coluna.VALOR, "valor", "valor_transacao", "montante", "amount");
		adicionar(cabecalhos, Coluna.TIPO, "tipo", "sinal", "natureza", "debito_credito", "entrada_saida", "type");
		adicionar(cabecalhos, Coluna.DOCUMENTO, "documento", "numero_documento", "document", "referencia", "refnum");
		adicionar(cabecalhos, Coluna.IDENTIFICADOR, "identificador", "identificador_bancario", "fitid",
				"id_transacao", "transaction_id");
		return Map.copyOf(cabecalhos);
	}

	private static void adicionar(Map<String, Coluna> mapa, Coluna coluna, String... nomes) {
		for (String nome : nomes) {
			mapa.put(nome, coluna);
		}
	}

	private enum Coluna {
		DATA("data"), DESCRICAO("descricao"), VALOR("valor"), TIPO("tipo"),
		DOCUMENTO("documento"), IDENTIFICADOR("identificador");

		private final String rotulo;

		Coluna(String rotulo) {
			this.rotulo = rotulo;
		}
	}

	private record ValorCelula(String texto, LocalDate data) {
		static ValorCelula vazio() {
			return new ValorCelula("", null);
		}
	}

	private record Linha(int numero, List<ValorCelula> valores) {
	}

	private record Candidato(List<Linha> linhas, Map<Coluna, Integer> indices) {
	}
}
