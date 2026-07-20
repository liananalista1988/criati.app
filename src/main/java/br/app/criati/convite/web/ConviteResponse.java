package br.app.criati.convite.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusConvite;

// Nunca inclui tokenHash nem token bruto.
public record ConviteResponse(
		UUID id,
		String email,
		PerfilUsuario perfil,
		StatusConvite status,
		OffsetDateTime expiraEm,
		OffsetDateTime criadoEm,
		OffsetDateTime utilizadoEm) {
}
