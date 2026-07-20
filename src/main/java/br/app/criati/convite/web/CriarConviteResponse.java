package br.app.criati.convite.web;

import com.fasterxml.jackson.annotation.JsonInclude;

// tokenBruto so e preenchido quando criati.convite.expor-token-bruto=true
// (padrao apenas em local/test); omitido do JSON quando nulo.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CriarConviteResponse(ConviteResponse convite, String tokenBruto) {
}
