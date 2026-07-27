package br.app.criati.admin.web;

// Contrato proprio da redefinicao GLOBAL de senha - nunca reaproveita
// RedefinirSenhaRequest (br.app.criati.acesso.web, fluxo empresarial) nem
// nenhum DTO geral de usuario. Politica minima de tamanho e obrigatoriedade
// (null/vazio) ficam inteiramente a cargo de SenhaValidador, dentro do
// service: sem @NotBlank aqui de proposito, para que campos ausentes/vazios
// cheguem ate o service e sejam auditados como FALHA_VALIDACAO/SENHA_INVALIDA
// (Bean Validation bloquearia antes do controller, sem gerar auditoria - ver
// RedefinirSenhaGlobalService).
public record RedefinirSenhaGlobalRequest(String novaSenha, String confirmacaoSenha) {
}
