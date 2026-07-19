package br.app.criati.usuario.web;

import java.util.UUID;

import br.app.criati.shared.enums.StatusCadastro;

public record UsuarioResponse(
		UUID id,
		String nome,
		String email,
		StatusCadastro status) {
}
