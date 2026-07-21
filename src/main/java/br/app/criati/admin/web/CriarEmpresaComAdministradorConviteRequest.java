package br.app.criati.admin.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarEmpresaComAdministradorConviteRequest(
		@Valid @NotNull(message = "Dados da empresa sao obrigatorios") DadosEmpresaInicialRequest empresa,
		List<String> aplicacoesIniciais,
		@Valid @NotNull(message = "Dados do administrador sao obrigatorios") DadosAdministradorConvite administrador) {

	public record DadosAdministradorConvite(
			@NotBlank(message = "Nome e obrigatorio") String nome,
			@NotBlank(message = "E-mail e obrigatorio")
			@Email(message = "E-mail deve ser valido") String email) {
	}
}
