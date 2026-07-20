package br.app.criati.convite.service;

import br.app.criati.convite.model.Convite;

// Carrega o token bruto apenas no retorno imediato da criacao (uso interno,
// nunca persistido, nunca logado). O controller decide se o expoe na
// resposta HTTP dependendo do perfil ativo.
public record ConviteCriado(Convite convite, String tokenBruto) {
}
