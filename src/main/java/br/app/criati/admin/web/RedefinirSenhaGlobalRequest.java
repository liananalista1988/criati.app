package br.app.criati.admin.web;

import jakarta.validation.constraints.NotBlank;

// Contrato proprio da redefinicao GLOBAL de senha - nunca reaproveita
// RedefinirSenhaRequest (br.app.criati.acesso.web, fluxo empresarial) nem
// nenhum DTO geral de usuario. Politica minima de tamanho fica inteiramente
// a cargo de SenhaValidador, nao duplicada aqui em anotacoes.
public record RedefinirSenhaGlobalRequest(@NotBlank String novaSenha, @NotBlank String confirmacaoSenha) {
}
