package br.app.criati.admin.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;

// Nunca inclui senha/hash/token: apenas dados globais + os vinculos e
// convites pendentes relacionados a este usuario, para visao administrativa.
public record UsuarioDetalheAdminResponse(
		UUID id,
		String nome,
		String email,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		boolean superAdministrador,
		List<VinculoResumo> vinculos,
		List<ConvitePendenteResumo> convitesPendentes) {

	public record VinculoResumo(
			UUID usuarioEmpresaId,
			UUID empresaId,
			String empresaNome,
			PerfilUsuario perfil,
			StatusCadastro status,
			OffsetDateTime criadoEm) {
	}

	public record ConvitePendenteResumo(
			UUID conviteId,
			UUID empresaId,
			String empresaNome,
			PerfilUsuario perfil,
			StatusConvite status,
			OffsetDateTime expiraEm,
			OffsetDateTime criadoEm) {
	}
}
