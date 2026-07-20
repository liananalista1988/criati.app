package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.financeiro.service.ResumoCategoriaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;

public record ResumoCategoriaResponse(UUID categoriaId, String categoriaNome, TipoFinanceiro tipo, BigDecimal total) {

	public static ResumoCategoriaResponse from(ResumoCategoriaFinanceira resumo) {
		return new ResumoCategoriaResponse(
				resumo.categoriaId(), resumo.categoriaNome(), resumo.tipo(), resumo.total());
	}
}
