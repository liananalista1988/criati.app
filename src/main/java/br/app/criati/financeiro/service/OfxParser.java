package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import br.app.criati.exception.DadosInvalidosException;

@Component
public class OfxParser {

	private static final Pattern BLOCO_TRANSACAO = Pattern.compile(
			"(?is)<STMTTRN\\b[^>]*>(.*?)(?=<STMTTRN\\b|</BANKTRANLIST>|</OFX>)");
	private static final Pattern CABECALHO_ENCODING = Pattern.compile("(?im)^\\s*ENCODING\\s*:\\s*([^\\r\\n]+)");
	private static final Pattern CABECALHO_CHARSET = Pattern.compile("(?im)^\\s*CHARSET\\s*:\\s*([^\\r\\n]+)");

	public List<TransacaoBancariaExtraida> parse(byte[] conteudo) {
		String texto = decodificar(conteudo).replace("\uFEFF", "");
		String caixaAlta = texto.toUpperCase(Locale.ROOT);
		if (texto.indexOf('\0') >= 0 || caixaAlta.contains("<!DOCTYPE") || caixaAlta.contains("<!ENTITY")) {
			throw invalido();
		}
		if (!caixaAlta.contains("<OFX>") || !caixaAlta.contains("<BANKTRANLIST>")) {
			throw invalido();
		}

		List<TransacaoBancariaExtraida> transacoes = new ArrayList<>();
		Matcher matcher = BLOCO_TRANSACAO.matcher(texto);
		while (matcher.find()) {
			transacoes.add(extrairTransacao(matcher.group(1)));
		}
		if (transacoes.isEmpty()) {
			throw new DadosInvalidosException("Arquivo OFX nao possui transacoes bancarias");
		}
		return List.copyOf(transacoes);
	}

	private TransacaoBancariaExtraida extrairTransacao(String bloco) {
		String dataBruta = tag(bloco, "DTPOSTED");
		String valorBruto = tag(bloco, "TRNAMT");
		if (dataBruta == null || valorBruto == null) {
			throw new DadosInvalidosException("Transacao OFX sem data ou valor");
		}
		LocalDate data = parseData(dataBruta);
		BigDecimal valor = parseValor(valorBruto);
		String tipo = limitar(normalizar(tag(bloco, "TRNTYPE")), 40);
		if (tipo == null) {
			tipo = valor.signum() < 0 ? "DEBIT" : "CREDIT";
		}
		String memo = normalizar(tag(bloco, "MEMO"));
		String nome = normalizar(tag(bloco, "NAME"));
		String descricao = limitar(memo != null ? memo : nome, 500);
		String identificador = limitar(normalizar(tag(bloco, "FITID")), 150);
		String documento = normalizar(tag(bloco, "CHECKNUM"));
		if (documento == null) {
			documento = normalizar(tag(bloco, "REFNUM"));
		}
		return new TransacaoBancariaExtraida(data, valor, tipo.toUpperCase(Locale.ROOT), descricao,
				identificador, limitar(documento, 100));
	}

	private LocalDate parseData(String valor) {
		String digitos = valor.replaceAll("[^0-9].*$", "");
		if (digitos.length() < 8) {
			throw new DadosInvalidosException("Data invalida em transacao OFX");
		}
		try {
			return LocalDate.of(Integer.parseInt(digitos.substring(0, 4)),
					Integer.parseInt(digitos.substring(4, 6)), Integer.parseInt(digitos.substring(6, 8)));
		} catch (DateTimeException | NumberFormatException excecao) {
			throw new DadosInvalidosException("Data invalida em transacao OFX");
		}
	}

	private BigDecimal parseValor(String valor) {
		try {
			BigDecimal normalizado = new BigDecimal(valor.trim()).setScale(2, RoundingMode.HALF_UP);
			if (normalizado.signum() == 0 || normalizado.precision() > 19) {
				throw new NumberFormatException();
			}
			return normalizado;
		} catch (NumberFormatException excecao) {
			throw new DadosInvalidosException("Valor invalido em transacao OFX");
		}
	}

	private String tag(String bloco, String tag) {
		Pattern pattern = Pattern.compile("(?is)<" + tag + "\\b[^>]*>\\s*([^<\\r\\n]+)");
		Matcher matcher = pattern.matcher(bloco);
		return matcher.find() ? decodificarEntidades(matcher.group(1)) : null;
	}

	private String decodificar(byte[] conteudo) {
		String cabecalho = new String(conteudo, StandardCharsets.ISO_8859_1);
		String encoding = grupo(CABECALHO_ENCODING, cabecalho);
		String charset = grupo(CABECALHO_CHARSET, cabecalho);
		if ((encoding != null && encoding.toUpperCase(Locale.ROOT).contains("UTF-8"))
				|| (charset != null && charset.toUpperCase(Locale.ROOT).contains("UTF-8"))) {
			return decodificarEstrito(conteudo, StandardCharsets.UTF_8);
		}
		if ((encoding != null && encoding.toUpperCase(Locale.ROOT).contains("UNICODE"))) {
			return decodificarEstrito(conteudo, StandardCharsets.UTF_16);
		}
		if ((charset != null && charset.contains("1252"))) {
			return new String(conteudo, Charset.forName("windows-1252"));
		}
		try {
			return decodificarEstrito(conteudo, StandardCharsets.UTF_8);
		} catch (DadosInvalidosException excecao) {
			return new String(conteudo, Charset.forName("windows-1252"));
		}
	}

	private String decodificarEstrito(byte[] conteudo, Charset charset) {
		try {
			return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(conteudo)).toString();
		} catch (CharacterCodingException excecao) {
			throw new DadosInvalidosException("Encoding do arquivo OFX e invalido");
		}
	}

	private String grupo(Pattern pattern, String valor) {
		Matcher matcher = pattern.matcher(valor);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private String normalizar(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return valor.trim().replaceAll("\\s+", " ");
	}

	private String limitar(String valor, int limite) {
		return valor == null || valor.length() <= limite ? valor : valor.substring(0, limite);
	}

	private String decodificarEntidades(String valor) {
		return valor.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&quot;", "\"").replace("&apos;", "'");
	}

	private DadosInvalidosException invalido() {
		return new DadosInvalidosException("Conteudo do arquivo OFX e invalido ou incompativel");
	}
}
