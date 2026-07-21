package br.app.criati.admin.web;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.app.criati.convite.web.ConviteResponse;
import br.app.criati.empresa.web.EmpresaResponse;

// tokenBruto so e preenchido quando criati.convite.expor-token-bruto=true
// (padrao apenas em local/test); omitido do JSON quando nulo, mesmo padrao de
// CriarConviteResponse.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmpresaComConviteAdministradorResponse(
		EmpresaResponse empresa,
		List<String> aplicacoesHabilitadas,
		ConviteResponse convite,
		String tokenBruto) {
}
