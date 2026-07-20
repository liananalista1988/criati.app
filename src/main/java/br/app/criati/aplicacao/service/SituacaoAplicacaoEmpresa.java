package br.app.criati.aplicacao.service;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.shared.enums.StatusCadastro;

/**
 * Situacao de uma aplicacao do catalogo em relacao a uma empresa especifica.
 * {@code statusVinculo} nulo significa que a empresa nunca habilitou essa
 * aplicacao (nenhuma linha em EmpresaAplicacao ainda existe).
 */
public record SituacaoAplicacaoEmpresa(Aplicacao aplicacao, StatusCadastro statusVinculo) {

	public boolean ativaParaEmpresa() {
		return aplicacao.estaAtiva() && statusVinculo == StatusCadastro.ATIVO;
	}
}
