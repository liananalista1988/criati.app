package br.app.criati.convite.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

// Token de convite: 256 bits de entropia via SecureRandom, nunca um UUID
// simples. Apenas o hash SHA-256 (nao BCrypt - BCrypt e para senhas de
// usuario com custo intencionalmente alto; aqui o token ja e aleatorio de
// alta entropia, entao um hash rapido e determinístico basta, e permite
// localizar o convite por igualdade indexada no banco) e persistido; o token
// bruto nunca e gravado.
@Service
public class ConviteTokenService {

	private static final int TAMANHO_TOKEN_BYTES = 32;

	private final SecureRandom secureRandom = new SecureRandom();

	public String gerarTokenBruto() {
		byte[] bytes = new byte[TAMANHO_TOKEN_BYTES];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String calcularHash(String tokenBruto) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(tokenBruto.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException excecao) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel", excecao);
		}
	}
}
