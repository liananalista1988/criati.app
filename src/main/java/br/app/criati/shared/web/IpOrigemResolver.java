package br.app.criati.shared.web;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Encapsula a extracao do IP de origem de uma requisicao HTTP - primeiro uso
 * desse tipo de captura no sistema (CRIATI-SEG-001, redefinicao global de
 * senha). Hoje usa apenas {@code getRemoteAddr()} (conexao direta); nao confia
 * em {@code X-Forwarded-For} ou cabecalhos semelhantes enquanto nao existir
 * uma configuracao formal de proxy confiavel (confiar nesses cabecalhos sem
 * validar a origem permitiria a um cliente forjar o IP registrado na
 * auditoria). Ponto unico de mudanca quando essa configuracao existir.
 */
@Component
public class IpOrigemResolver {

	public String resolver(HttpServletRequest request) {
		return request == null ? null : request.getRemoteAddr();
	}
}
