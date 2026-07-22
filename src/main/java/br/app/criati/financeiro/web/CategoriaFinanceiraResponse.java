package br.app.criati.financeiro.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;

public record CategoriaFinanceiraResponse(UUID id, String nome, String descricao, TipoFinanceiro tipo,
		UUID categoriaPaiId, String categoriaPaiNome, int nivel, long quantidadeSubcategorias,
		int ordem, boolean permiteOrcamento, StatusCadastro status, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static CategoriaFinanceiraResponse from(CategoriaFinanceira categoria, long quantidadeSubcategorias) {
		CategoriaFinanceira pai = categoria.getCategoriaPai();
		return new CategoriaFinanceiraResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao(),
				categoria.getTipo(), pai == null ? null : pai.getId(), pai == null ? null : pai.getNome(),
				pai == null ? 1 : 2, quantidadeSubcategorias, categoria.getOrdemExibicao(),
				categoria.isPermiteOrcamento(), categoria.getStatus(), categoria.getCriadoEm(), categoria.getAtualizadoEm(),
				categoria.getCriadoPor() == null ? null : categoria.getCriadoPor().getId(),
				categoria.getAtualizadoPor() == null ? null : categoria.getAtualizadoPor().getId());
	}

	public static CategoriaFinanceiraResponse from(CategoriaFinanceira categoria) {
		return from(categoria, 0);
	}
}
