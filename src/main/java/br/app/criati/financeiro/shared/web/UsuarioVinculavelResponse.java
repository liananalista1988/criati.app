package br.app.criati.financeiro.shared.web;

import java.util.UUID;

import br.app.criati.usuario.model.Usuario;

public record UsuarioVinculavelResponse(UUID id, String nome, String email) {

	public static UsuarioVinculavelResponse from(Usuario usuario) {
		return new UsuarioVinculavelResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
	}
}
