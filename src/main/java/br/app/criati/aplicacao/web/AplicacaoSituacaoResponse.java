package br.app.criati.aplicacao.web;

import br.app.criati.aplicacao.service.SituacaoAplicacaoEmpresa;
import br.app.criati.shared.enums.StatusCadastro;

public record AplicacaoSituacaoResponse(
		String codigo,
		String nome,
		String descricao,
		StatusCadastro statusAplicacao,
		StatusCadastro statusVinculo) {

	public static AplicacaoSituacaoResponse from(SituacaoAplicacaoEmpresa situacao) {
		return new AplicacaoSituacaoResponse(
				situacao.aplicacao().getCodigo(),
				situacao.aplicacao().getNome(),
				situacao.aplicacao().getDescricao(),
				situacao.aplicacao().getStatus(),
				situacao.statusVinculo());
	}
}
