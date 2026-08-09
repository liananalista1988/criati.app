package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import br.app.criati.exception.DadosInvalidosException;

@Component
public class OfxParser {

	private static final Pattern BLOCO_TRANSACAO = Pattern.compile(
			"(?is)<STMTTRN\\b[^>]*>(.*?)(?=<STMTTRN\\b|</BANKTRANLIST>|</OFX>)");
	private static final Pattern BLOCO_CONTA_ORIGEM = Pattern.compile(
			"(?is)<(BANKACCTFROM|CCACCTFROM)\\b[^>]*>(.*?)</\\1>");
	private static final Pattern CABECALHO_ENCODING = Pattern.compile("(?im)^\\s*ENCODING\\s*:\\s*([^\\r\\n]+)");
	private static final Pattern CABECALHO_CHARSET = Pattern.compile("(?im)^\\s*CHARSET\\s*:\\s*([^\\r\\n]+)");
	private static final List<PerfilOfx> PERFIS = List.of(new PerfilOfxBancoBrasil(), new PerfilOfxGenerico());

	public List<TransacaoBancariaExtraida> parse(byte[] conteudo) {
		String texto = validarConteudoBasico(conteudo);
		String caixaAlta = texto.toUpperCase(Locale.ROOT);
		if (!caixaAlta.contains("<OFX>") || !caixaAlta.contains("<BANKTRANLIST>")) {
			throw invalido();
		}
		PerfilOfx perfil = PERFIS.stream().filter(item -> item.reconhece(texto)).findFirst()
				.orElseThrow(this::invalido);

		List<TransacaoBancariaExtraida> transacoes = new ArrayList<>();
		Matcher matcher = BLOCO_TRANSACAO.matcher(texto);
		while (matcher.find()) {
			DadosTransacaoOfx dados = lerDados(matcher.group(1));
			if (!perfil.ignorar(dados)) {
				transacoes.add(extrairTransacao(dados, perfil));
			}
		}
		if (transacoes.isEmpty()) {
			throw new DadosInvalidosException("Arquivo OFX nao possui transacoes bancarias");
		}
		return List.copyOf(transacoes);
	}

	/**
	 * Metadado de identificacao da conta de origem (BANKACCTFROM/CCACCTFROM),
	 * usado apenas para sugerir automaticamente a conta financeira do lote -
	 * nunca para decidir transacao alguma. Somente leitura: nao lanca excecao
	 * por ausencia da secao (nem todo banco a inclui) nem por tag faltante,
	 * apenas pelas mesmas violacoes de seguranca ja aplicadas em {@link #parse}.
	 * Chamar somente apos {@link #parse} ja ter validado o arquivo com sucesso.
	 */
	public Optional<IdentificacaoBancariaOfx> identificarConta(byte[] conteudo) {
		String texto = validarConteudoBasico(conteudo);
		Matcher matcher = BLOCO_CONTA_ORIGEM.matcher(texto);
		if (!matcher.find()) {
			return Optional.empty();
		}
		String bloco = matcher.group(2);
		IdentificacaoBancariaOfx identificacao = new IdentificacaoBancariaOfx(
				normalizar(tag(bloco, "BANKID")), normalizar(tag(bloco, "BRANCHID")),
				normalizar(tag(bloco, "ACCTID")), normalizar(tag(bloco, "ACCTTYPE")));
		boolean tudoVazio = identificacao.bankId() == null && identificacao.branchId() == null
				&& identificacao.acctId() == null && identificacao.acctType() == null;
		return tudoVazio ? Optional.empty() : Optional.of(identificacao);
	}

	private String validarConteudoBasico(byte[] conteudo) {
		String texto = decodificar(conteudo).replace("﻿", "");
		String caixaAlta = texto.toUpperCase(Locale.ROOT);
		if (texto.indexOf('\0') >= 0 || caixaAlta.contains("<!DOCTYPE") || caixaAlta.contains("<!ENTITY")) {
			throw invalido();
		}
		return texto;
	}

	private DadosTransacaoOfx lerDados(String bloco) {
		return new DadosTransacaoOfx(tag(bloco, "DTPOSTED"), tag(bloco, "TRNAMT"),
				tag(bloco, "TRNTYPE"), tag(bloco, "NAME"), tag(bloco, "MEMO"),
				tag(bloco, "FITID"), tag(bloco, "CHECKNUM"), tag(bloco, "REFNUM"));
	}

	private TransacaoBancariaExtraida extrairTransacao(DadosTransacaoOfx dados, PerfilOfx perfil) {
		String dataBruta = dados.data();
		String valorBruto = dados.valor();
		if (dataBruta == null || valorBruto == null) {
			throw new DadosInvalidosException("Transacao OFX sem data ou valor");
		}
		LocalDate data = parseData(dataBruta);
		BigDecimal valor = parseValor(valorBruto);
		String tipo = limitar(normalizar(dados.tipo()), 40);
		if (tipo == null) {
			tipo = valor.signum() < 0 ? "DEBIT" : "CREDIT";
		}
		String descricao = limitar(perfil.descricao(dados), 500);
		String identificador = limitar(normalizar(dados.fitid()), 150);
		perfil.validarMovimentacao(identificador);
		String documento = normalizar(dados.checknum());
		if (documento == null) {
			documento = normalizar(dados.refnum());
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

	private static String tag(String bloco, String tag) {
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

	private static String normalizarComparacao(String valor) {
		if (valor == null || valor.isBlank()) {
			return "";
		}
		return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
				.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static String combinar(String nome, String memo) {
		String nomeNormalizado = normalizarEstatico(nome);
		String memoNormalizado = normalizarEstatico(memo);
		if (nomeNormalizado == null) {
			return memoNormalizado;
		}
		if (memoNormalizado == null || nomeNormalizado.equalsIgnoreCase(memoNormalizado)) {
			return nomeNormalizado;
		}
		return nomeNormalizado + " - " + memoNormalizado;
	}

	private static String normalizarEstatico(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim().replaceAll("\\s+", " ");
	}

	private String limitar(String valor, int limite) {
		return valor == null || valor.length() <= limite ? valor : valor.substring(0, limite);
	}

	private static String decodificarEntidades(String valor) {
		return valor.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
				.replace("&quot;", "\"").replace("&apos;", "'");
	}

	private DadosInvalidosException invalido() {
		return new DadosInvalidosException("Conteudo do arquivo OFX e invalido ou incompativel");
	}

	private interface PerfilOfx {
		boolean reconhece(String documento);

		boolean ignorar(DadosTransacaoOfx dados);

		String descricao(DadosTransacaoOfx dados);

		void validarMovimentacao(String fitid);
	}

	private static final class PerfilOfxBancoBrasil implements PerfilOfx {
		@Override
		public boolean reconhece(String documento) {
			String bankId = normalizarEstatico(tag(documento, "BANKID"));
			return bankId != null && bankId.replaceFirst("^0+", "").equals("1");
		}

		@Override
		public boolean ignorar(DadosTransacaoOfx dados) {
			return informativo(dados.nome()) || informativo(dados.memo());
		}

		private boolean informativo(String valor) {
			String normalizado = normalizarComparacao(valor);
			return normalizado.equals("saldo anterior") || normalizado.equals("saldo do dia");
		}

		@Override
		public String descricao(DadosTransacaoOfx dados) {
			return combinar(dados.nome(), dados.memo());
		}

		@Override
		public void validarMovimentacao(String fitid) {
			if (fitid == null) {
				throw new DadosInvalidosException("Movimentacao OFX do Banco do Brasil sem FITID");
			}
		}
	}

	private static final class PerfilOfxGenerico implements PerfilOfx {
		@Override
		public boolean reconhece(String documento) {
			return true;
		}

		@Override
		public boolean ignorar(DadosTransacaoOfx dados) {
			return false;
		}

		@Override
		public String descricao(DadosTransacaoOfx dados) {
			String memo = normalizarEstatico(dados.memo());
			return memo != null ? memo : normalizarEstatico(dados.nome());
		}

		@Override
		public void validarMovimentacao(String fitid) {
			// O fallback generico preserva a compatibilidade com OFX sem FITID.
		}
	}

	private record DadosTransacaoOfx(String data, String valor, String tipo, String nome,
			String memo, String fitid, String checknum, String refnum) {
	}
}
