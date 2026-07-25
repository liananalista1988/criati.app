package br.app.criati.acesso.web;

import jakarta.validation.constraints.NotBlank;

// Contrato proprio, separado de IntegranteEmpresaResponse/AlterarPerfilRequest:
// nunca reaproveita os DTOs gerais de usuario para nao arriscar expor/aceitar
// senha em nenhum contrato que tambem carregue dados gerais. A politica
// minima de tamanho fica inteiramente a cargo de SenhaValidador (nao
// duplicada aqui em anotacoes), unica fonte de verdade ja usada em
// AceitarConviteService.
public record RedefinirSenhaRequest(@NotBlank String novaSenha, @NotBlank String confirmacaoSenha) {
}
