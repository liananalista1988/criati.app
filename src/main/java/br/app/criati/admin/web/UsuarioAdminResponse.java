package br.app.criati.admin.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.StatusCadastro;

// Nunca inclui senha/hash/token: apenas dados globais + contadores agregados.
public record UsuarioAdminResponse(
		UUID id,
		String nome,
		String email,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		long quantidadeEmpresas,
		long vinculosAtivos) {
}
