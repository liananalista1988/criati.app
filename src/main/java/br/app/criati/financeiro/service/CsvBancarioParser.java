package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.app.criati.exception.DadosInvalidosException;

@Component
public class CsvBancarioParser {

	private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
	private static final Pattern VALOR_SEGURO = Pattern.compile("[+-]?\\d{1,17}(?:[.,]\\d{1,2})?");
	private static final Pattern VALOR_BRASILEIRO = Pattern.compile(
			"[+-]?(?:\\d{1,3}(?:\\.\\d{3})*|\\d{1,17})(?:,\\d{1,2})?");
	private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/uuuu")
			.withResolverStyle(ResolverStyle.STRICT);
	private static final DateTimeFormatter DATA_BR_HIFEN = DateTimeFormatter.ofPattern("dd-MM-uuuu")
			.withResolverStyle(ResolverStyle.STRICT);
	private static final Set<String> ENTRADAS = Set.of("entrada", "receita", "credito", "credit", "c", "+");
	private static final Set<String> SAIDAS = Set.of("saida", "despesa", "debito", "debit", "d", "-");

	private static final Map<String, Coluna> CABECALHOS = criarCabecalhos();
	private static final List<PerfilCsv> PERFIS = List.of(new PerfilCsvBancoInter(), new PerfilCsvGenerico());

	private final int maximoLinhas;
	private final int maximoColunas;
	private final int maximoCaracteresCampo;

	public CsvBancarioParser(
			@Value("${criati.financeiro.importacao-csv.maximo-linhas:10000}") int maximoLinhas,
			@Value("${criati.financeiro.importacao-csv.maximo-colunas:50}") int maximoColunas,
			@Value("${criati.financeiro.importacao-csv.maximo-caracteres-campo:500}") int maximoCaracteresCampo) {
		if (maximoLinhas <= 0 || maximoColunas < 3 || maximoCaracteresCampo <= 0) {
			throw new IllegalArgumentException("Limites da importacao CSV devem ser positivos");
		}
		this.maximoLinhas = maximoLinhas;
		this.maximoColunas = maximoColunas;
		this.maximoCaracteresCampo = maximoCaracteresCampo;
	}

	public List<TransacaoBancariaExtraida> parse(byte[] conteudo) {
		String texto = decodificarUtf8(conteudo);
		validarQuantidadeLinhas(texto);
		Candidato virgula = analisar(texto, ',');
		Candidato pontoEVirgula = analisar(texto, ';');
		if (virgula.valido() && pontoEVirgula.valido()) {
			throw new DadosInvalidosException("Separador CSV ambiguo; use somente virgula ou ponto e virgula");
		}
		Candidato escolhido = virgula.valido() ? virgula : pontoEVirgula.valido() ? pontoEVirgula : null;
		if (escolhido == null) {
			throw erroMaisEspecifico(virgula.erro(), pontoEVirgula.erro());
		}
		return extrair(escolhido.registros(), escolhido.plano());
	}

	private Candidato analisar(String texto, char separador) {
		try {
			List<List<String>> registros = lerRegistros(texto, separador);
			for (PerfilCsv perfil : PERFIS) {
				PlanoCsv plano = perfil.identificar(registros);
				if (plano != null) {
					validarRegistrosDeDados(registros, plano);
					return new Candidato(registros, plano, null);
				}
			}
			throw new DadosInvalidosException("CSV deve possuir cabecalhos univocos para data, descricao e valor");
		} catch (DadosInvalidosException excecao) {
			return new Candidato(null, null, excecao);
		}
	}

	private List<TransacaoBancariaExtraida> extrair(List<List<String>> registros, PlanoCsv plano) {
		List<TransacaoBancariaExtraida> transacoes = new ArrayList<>();
		Map<Coluna, Integer> indices = plano.indices();
		for (int linha = plano.indiceCabecalho() + 1; linha < registros.size(); linha++) {
			List<String> registro = registros.get(linha);
			String dataBruta = campo(registro, indices, Coluna.DATA);
			String descricao = descricao(registro, plano);
			String valorBruto = campo(registro, indices, Coluna.VALOR).trim();
			validarFormula(dataBruta);
			validarFormula(descricao);
			BigDecimal valor = plano.valorBrasileiro() ? parseValorBrasileiro(valorBruto) : parseValor(valorBruto);
			String tipo = campoOpcional(registro, indices, Coluna.TIPO);
			if (tipo != null) {
				if (!tipo.trim().equals("+") && !tipo.trim().equals("-")) {
					validarFormula(tipo);
				}
				valor = aplicarTipo(valor, tipo);
			}
			String documento = limitar(normalizarOpcional(campoOpcional(registro, indices, Coluna.DOCUMENTO)),
					100, "Documento CSV excede o limite de 100 caracteres");
			String identificador = limitar(
					normalizarOpcional(campoOpcional(registro, indices, Coluna.IDENTIFICADOR)), 150,
					"Identificador bancario CSV excede o limite de 150 caracteres");
			validarFormula(documento);
			validarFormula(identificador);
			transacoes.add(new TransacaoBancariaExtraida(parseData(dataBruta), valor,
					valor.signum() < 0 ? "DEBIT" : "CREDIT", descricao, identificador, documento));
		}
		if (transacoes.isEmpty()) {
			throw new DadosInvalidosException("Arquivo CSV nao possui transacoes bancarias");
		}
		return List.copyOf(transacoes);
	}

	private String descricao(List<String> registro, PlanoCsv plano) {
		String descricao = campo(registro, plano.indices(), Coluna.DESCRICAO);
		validarFormula(descricao);
		String descricaoNormalizada = normalizarOpcional(descricao);
		Integer indiceHistorico = plano.indiceHistorico();
		if (indiceHistorico == null) {
			return normalizarTexto(descricao, "Descricao CSV obrigatoria");
		}
		String historico = registro.get(indiceHistorico);
		validarFormula(historico);
		String historicoNormalizado = normalizarOpcional(historico);
		String combinada;
		if (historicoNormalizado == null) {
			combinada = descricaoNormalizada;
		} else if (descricaoNormalizada == null || historicoNormalizado.equalsIgnoreCase(descricaoNormalizada)) {
			combinada = historicoNormalizado;
		} else {
			combinada = historicoNormalizado + " - " + descricaoNormalizada;
		}
		if (combinada == null) {
			throw new DadosInvalidosException("Descricao CSV obrigatoria");
		}
		return limitar(combinada, 500, "Descricao CSV excede o limite de 500 caracteres");
	}

	private List<List<String>> lerRegistros(String texto, char separador) {
		List<List<String>> registros = new ArrayList<>();
		List<String> registro = new ArrayList<>();
		StringBuilder campo = new StringBuilder();
		boolean entreAspas = false;
		boolean aposAspas = false;
		for (int indice = 0; indice < texto.length(); indice++) {
			char atual = texto.charAt(indice);
			if (entreAspas) {
				if (atual == '"') {
					if (indice + 1 < texto.length() && texto.charAt(indice + 1) == '"') {
						adicionarCaractere(campo, '"');
						indice++;
					} else {
						entreAspas = false;
						aposAspas = true;
					}
				} else if (atual == '\r' || atual == '\n') {
					throw new DadosInvalidosException("CSV nao permite quebra de linha dentro de celula");
				} else {
					adicionarCaractere(campo, atual);
				}
				continue;
			}
			if (aposAspas && atual != separador && atual != '\r' && atual != '\n') {
				throw new DadosInvalidosException("Estrutura de aspas do CSV e invalida");
			}
			if (atual == '"') {
				if (campo.length() != 0 || aposAspas) {
					throw new DadosInvalidosException("Estrutura de aspas do CSV e invalida");
				}
				entreAspas = true;
			} else if (atual == separador) {
				adicionarCampo(registro, campo);
				aposAspas = false;
			} else if (atual == '\r' || atual == '\n') {
				adicionarCampo(registro, campo);
				adicionarRegistro(registros, registro);
				registro = new ArrayList<>();
				aposAspas = false;
				if (atual == '\r' && indice + 1 < texto.length() && texto.charAt(indice + 1) == '\n') {
					indice++;
				}
			} else {
				adicionarCaractere(campo, atual);
			}
		}
		if (entreAspas) {
			throw new DadosInvalidosException("Estrutura de aspas do CSV e invalida");
		}
		if (campo.length() > 0 || !registro.isEmpty() || aposAspas) {
			adicionarCampo(registro, campo);
			adicionarRegistro(registros, registro);
		}
		if (registros.isEmpty()) {
			throw new DadosInvalidosException("Arquivo CSV esta vazio");
		}
		if (registros.stream().noneMatch(item -> item.size() >= 3)) {
			throw new DadosInvalidosException("Nao foi possivel detectar com seguranca o separador CSV");
		}
		return registros;
	}

	private void validarRegistrosDeDados(List<List<String>> registros, PlanoCsv plano) {
		int colunas = registros.get(plano.indiceCabecalho()).size();
		for (int indice = plano.indiceCabecalho() + 1; indice < registros.size(); indice++) {
			if (registros.get(indice).size() != colunas) {
				throw new DadosInvalidosException("CSV possui quantidade inconsistente de colunas");
			}
		}
	}

	private void adicionarCaractere(StringBuilder campo, char valor) {
		campo.append(valor);
		if (campo.length() > maximoCaracteresCampo) {
			throw new DadosInvalidosException("CSV possui campo excessivamente longo");
		}
	}

	private void adicionarCampo(List<String> registro, StringBuilder campo) {
		registro.add(campo.toString());
		campo.setLength(0);
		if (registro.size() > maximoColunas) {
			throw new DadosInvalidosException("CSV excede a quantidade maxima de colunas");
		}
	}

	private void adicionarRegistro(List<List<String>> registros, List<String> registro) {
		if (registro.stream().allMatch(String::isBlank)) {
			return;
		}
		registros.add(List.copyOf(registro));
		if (registros.size() > maximoLinhas + 1) {
			throw new DadosInvalidosException("CSV excede a quantidade maxima de linhas");
		}
	}

	private static Map<Coluna, Integer> mapearCabecalho(List<String> cabecalho) {
		Map<Coluna, Integer> indices = new HashMap<>();
		Set<String> nomes = new HashSet<>();
		for (int indice = 0; indice < cabecalho.size(); indice++) {
			String original = cabecalho.get(indice);
			validarFormulaEstatica(original);
			String normalizado = normalizarCabecalho(original);
			if (normalizado.isEmpty() || !nomes.add(normalizado)) {
				throw new DadosInvalidosException("CSV possui cabecalhos vazios ou duplicados");
			}
			Coluna coluna = CABECALHOS.get(normalizado);
			if (coluna != null && indices.putIfAbsent(coluna, indice) != null) {
				throw new DadosInvalidosException("CSV possui colunas ambiguas para " + coluna.rotulo);
			}
		}
		if (!indices.keySet().containsAll(Set.of(Coluna.DATA, Coluna.DESCRICAO, Coluna.VALOR))) {
			throw new DadosInvalidosException("CSV deve possuir cabecalhos univocos para data, descricao e valor");
		}
		return Map.copyOf(indices);
	}

	private String campo(List<String> registro, Map<Coluna, Integer> indices, Coluna coluna) {
		return registro.get(indices.get(coluna));
	}

	private String campoOpcional(List<String> registro, Map<Coluna, Integer> indices, Coluna coluna) {
		Integer indice = indices.get(coluna);
		return indice == null ? null : registro.get(indice);
	}

	private LocalDate parseData(String valor) {
		String limpa = valor.trim();
		for (DateTimeFormatter formato : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DATA_BR, DATA_BR_HIFEN,
				DateTimeFormatter.BASIC_ISO_DATE)) {
			try {
				return LocalDate.parse(limpa, formato);
			} catch (DateTimeParseException ignorada) {
				// Tenta apenas os formatos explicitos e nao ambiguos da allowlist.
			}
		}
		throw new DadosInvalidosException("Data invalida em transacao CSV");
	}

	private BigDecimal parseValor(String valor) {
		if (!VALOR_SEGURO.matcher(valor).matches()) {
			throw new DadosInvalidosException("Valor invalido em transacao CSV");
		}
		try {
			BigDecimal resultado = new BigDecimal(valor.replace(',', '.')).setScale(2, RoundingMode.UNNECESSARY);
			if (resultado.signum() == 0 || resultado.precision() > 19) {
				throw new ArithmeticException();
			}
			return resultado;
		} catch (ArithmeticException | NumberFormatException excecao) {
			throw new DadosInvalidosException("Valor invalido em transacao CSV");
		}
	}

	private BigDecimal parseValorBrasileiro(String valor) {
		if (!VALOR_BRASILEIRO.matcher(valor).matches()) {
			throw new DadosInvalidosException("Valor invalido em transacao CSV");
		}
		try {
			BigDecimal resultado = new BigDecimal(valor.replace(".", "").replace(',', '.'))
					.setScale(2, RoundingMode.UNNECESSARY);
			if (resultado.signum() == 0 || resultado.precision() > 19) {
				throw new ArithmeticException();
			}
			return resultado;
		} catch (ArithmeticException | NumberFormatException excecao) {
			throw new DadosInvalidosException("Valor invalido em transacao CSV");
		}
	}

	private BigDecimal aplicarTipo(BigDecimal valor, String tipo) {
		String tipoLimpo = tipo.trim();
		String normalizado = tipoLimpo.equals("+") || tipoLimpo.equals("-")
				? tipoLimpo
				: normalizarCabecalho(tipoLimpo);
		if (ENTRADAS.contains(normalizado)) {
			if (valor.signum() < 0) {
				throw new DadosInvalidosException("Tipo e sinal do valor CSV sao incompativeis");
			}
			return valor.abs();
		}
		if (SAIDAS.contains(normalizado)) {
			return valor.abs().negate();
		}
		throw new DadosInvalidosException("Tipo invalido em transacao CSV");
	}

	private String decodificarUtf8(byte[] conteudo) {
		int inicio = possuiBom(conteudo) ? BOM_UTF8.length : 0;
		try {
			String texto = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(conteudo, inicio, conteudo.length - inicio)).toString();
			if (texto.indexOf('\uFEFF') >= 0 || texto.chars().anyMatch(c -> c == 0
					|| (Character.isISOControl(c) && c != '\r' && c != '\n' && c != '\t'))) {
				throw new DadosInvalidosException("CSV possui caracteres invalidos");
			}
			return texto;
		} catch (CharacterCodingException excecao) {
			throw new DadosInvalidosException("Encoding do arquivo CSV deve ser UTF-8 valido");
		}
	}

	private boolean possuiBom(byte[] conteudo) {
		return conteudo.length >= BOM_UTF8.length && conteudo[0] == BOM_UTF8[0]
				&& conteudo[1] == BOM_UTF8[1] && conteudo[2] == BOM_UTF8[2];
	}

	private void validarQuantidadeLinhas(String texto) {
		int linhas = texto.isEmpty() ? 0 : 1;
		for (int indice = 0; indice < texto.length(); indice++) {
			char atual = texto.charAt(indice);
			if (atual == '\r' || atual == '\n') {
				linhas++;
				if (atual == '\r' && indice + 1 < texto.length() && texto.charAt(indice + 1) == '\n') {
					indice++;
				}
			}
		}
		if (!texto.isEmpty() && (texto.endsWith("\n") || texto.endsWith("\r"))) {
			linhas--;
		}
		if (linhas > maximoLinhas + 1) {
			throw new DadosInvalidosException("CSV excede a quantidade maxima de linhas");
		}
	}

	private void validarFormula(String valor) {
		validarFormulaEstatica(valor);
	}

	private static void validarFormulaEstatica(String valor) {
		if (valor == null) {
			return;
		}
		String limpa = valor.stripLeading();
		if (!limpa.isEmpty() && "=+-@".indexOf(limpa.charAt(0)) >= 0) {
			throw new DadosInvalidosException("CSV possui celula com formula potencialmente insegura");
		}
	}

	private String normalizarTexto(String valor, String mensagem) {
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

	private static String normalizarCabecalho(String valor) {
		String semAcentos = Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		return semAcentos.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
	}

	private DadosInvalidosException erroMaisEspecifico(DadosInvalidosException primeiro,
			DadosInvalidosException segundo) {
		for (DadosInvalidosException erro : List.of(primeiro, segundo)) {
			if (erro != null && !erro.getMessage().startsWith("Nao foi possivel detectar")
					&& !erro.getMessage().startsWith("CSV deve possuir cabecalhos")) {
				return erro;
			}
		}
		return new DadosInvalidosException("CSV deve possuir cabecalhos univocos para data, descricao e valor");
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
		DATA("data"),
		DESCRICAO("descricao"),
		VALOR("valor"),
		TIPO("tipo"),
		DOCUMENTO("documento"),
		IDENTIFICADOR("identificador");

		private final String rotulo;

		Coluna(String rotulo) {
			this.rotulo = rotulo;
		}
	}

	private interface PerfilCsv {
		PlanoCsv identificar(List<List<String>> registros);
	}

	private static final class PerfilCsvBancoInter implements PerfilCsv {
		private static final Set<String> CABECALHOS_INTER = Set.of(
				"data_lancamento", "historico", "descricao", "valor", "saldo");

		@Override
		public PlanoCsv identificar(List<List<String>> registros) {
			PlanoCsv encontrado = null;
			for (int linha = 0; linha < registros.size(); linha++) {
				List<String> registro = registros.get(linha);
				Map<String, Integer> indices = new HashMap<>();
				for (int coluna = 0; coluna < registro.size(); coluna++) {
					String original = registro.get(coluna);
					String nome = normalizarCabecalho(original);
					if (CABECALHOS_INTER.contains(nome) && indices.putIfAbsent(nome, coluna) != null) {
						throw new DadosInvalidosException("CSV Inter possui cabecalhos duplicados");
					}
				}
				if (!indices.keySet().containsAll(CABECALHOS_INTER)) {
					continue;
				}
				registro.forEach(CsvBancarioParser::validarFormulaEstatica);
				if (encontrado != null) {
					throw new DadosInvalidosException("CSV Inter possui mais de um cabecalho compativel");
				}
				Map<Coluna, Integer> colunas = Map.of(
						Coluna.DATA, indices.get("data_lancamento"),
						Coluna.DESCRICAO, indices.get("descricao"),
						Coluna.VALOR, indices.get("valor"));
				encontrado = new PlanoCsv(linha, colunas, indices.get("historico"), true);
			}
			return encontrado;
		}
	}

	private static final class PerfilCsvGenerico implements PerfilCsv {
		@Override
		public PlanoCsv identificar(List<List<String>> registros) {
			Map<Coluna, Integer> indices = mapearCabecalho(registros.get(0));
			return new PlanoCsv(0, indices, null, false);
		}
	}

	private record PlanoCsv(int indiceCabecalho, Map<Coluna, Integer> indices,
			Integer indiceHistorico, boolean valorBrasileiro) {
	}

	private record Candidato(List<List<String>> registros, PlanoCsv plano,
			DadosInvalidosException erro) {
		boolean valido() {
			return erro == null;
		}
	}
}
