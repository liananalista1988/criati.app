package br.app.criati.security.web;

import java.util.UUID;

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;

public record LoginResponse(
		UUID id,
		String nome,
		String email,
		StatusCadastro status) {

	public static LoginResponse from(Usuario usuario) {
		return new LoginResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getStatus());
	}
}
