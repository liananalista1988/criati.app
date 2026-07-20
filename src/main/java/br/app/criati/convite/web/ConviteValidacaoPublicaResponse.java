package br.app.criati.convite.web;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.app.criati.shared.enums.PerfilUsuario;

// Resposta publica: nunca revela o motivo da invalidez (inexistente,
// expirado, utilizado, revogado ou empresa inativa produzem exatamente a
// mesma resposta invalida()). Campos nulos sao omitidos do JSON.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConviteValidacaoPublicaResponse(
		boolean valido,
		String empresa,
		String emailMascarado,
		PerfilUsuario perfil,
		OffsetDateTime expiraEm) {

	public static ConviteValidacaoPublicaResponse valido(
			String empresa, String emailMascarado, PerfilUsuario perfil, OffsetDateTime expiraEm) {
		return new ConviteValidacaoPublicaResponse(true, empresa, emailMascarado, perfil, expiraEm);
	}

	public static ConviteValidacaoPublicaResponse invalido() {
		return new ConviteValidacaoPublicaResponse(false, null, null, null, null);
	}
}
