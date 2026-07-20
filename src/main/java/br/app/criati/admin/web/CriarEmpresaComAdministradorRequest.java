package br.app.criati.admin.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarEmpresaComAdministradorRequest(
		@Valid @NotNull(message = "Dados da empresa sao obrigatorios") DadosEmpresaInicial empresa,
		@Valid @NotNull(message = "Dados do administrador sao obrigatorios") DadosAdministradorInicial administrador) {

	public record DadosEmpresaInicial(
			@NotBlank(message = "Nome e obrigatorio") String nome,
			String nomeFantasia,
			@NotBlank(message = "CNPJ e obrigatorio") String cnpj) {
	}

	public record DadosAdministradorInicial(
			@NotBlank(message = "Nome e obrigatorio") String nome,
			@NotBlank(message = "E-mail e obrigatorio")
			@Email(message = "E-mail deve ser valido") String email,
			@NotBlank(message = "Senha e obrigatoria") String senha) {
	}
}
