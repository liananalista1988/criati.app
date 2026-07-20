package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;

public record CategoriaFinanceiraResponse(UUID id, String nome, TipoFinanceiro tipo, StatusCadastro status) {

	public static CategoriaFinanceiraResponse from(CategoriaFinanceira categoria) {
		return new CategoriaFinanceiraResponse(
				categoria.getId(), categoria.getNome(), categoria.getTipo(), categoria.getStatus());
	}
}
