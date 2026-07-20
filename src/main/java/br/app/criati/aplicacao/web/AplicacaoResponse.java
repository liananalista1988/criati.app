package br.app.criati.aplicacao.web;

import java.util.UUID;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.shared.enums.StatusCadastro;

public record AplicacaoResponse(
		UUID id,
		String codigo,
		String nome,
		String descricao,
		StatusCadastro status) {

	public static AplicacaoResponse from(Aplicacao aplicacao) {
		return new AplicacaoResponse(
				aplicacao.getId(),
				aplicacao.getCodigo(),
				aplicacao.getNome(),
				aplicacao.getDescricao(),
				aplicacao.getStatus());
	}
}
