package br.app.criati.financeiro.shared.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public record ParteFinanceiraResponse(
		UUID id,
		String nome,
		TipoParteFinanceira tipo,
		String documento,
		String apelido,
		String observacao,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static ParteFinanceiraResponse from(ParteFinanceira parte) {
		return new ParteFinanceiraResponse(
				parte.getId(), parte.getNome(), parte.getTipo(), parte.getDocumento(), parte.getApelido(),
				parte.getObservacao(), parte.getStatus(), parte.getCriadoEm(), parte.getAtualizadoEm());
	}
}
