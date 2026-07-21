package br.app.criati.admin.web;

import jakarta.validation.constraints.NotBlank;

// Duplica de proposito os campos de CriarEmpresaComAdministradorRequest.DadosEmpresaInicial:
// aquele DTO ja e coberto por testes existentes e nao deve ser alterado; este e
// reutilizado pelos dois novos fluxos de onboarding (usuario existente/convite).
public record DadosEmpresaInicialRequest(
		@NotBlank(message = "Nome e obrigatorio") String nome,
		String nomeFantasia,
		@NotBlank(message = "CNPJ e obrigatorio") String cnpj) {
}
