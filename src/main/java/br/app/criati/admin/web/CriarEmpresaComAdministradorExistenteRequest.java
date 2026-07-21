package br.app.criati.admin.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CriarEmpresaComAdministradorExistenteRequest(
		@Valid @NotNull(message = "Dados da empresa sao obrigatorios") DadosEmpresaInicialRequest empresa,
		List<String> aplicacoesIniciais,
		@NotNull(message = "Administrador e obrigatorio") UUID administradorUsuarioId) {
}
