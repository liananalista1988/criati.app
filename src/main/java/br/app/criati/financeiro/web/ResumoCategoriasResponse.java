package br.app.criati.financeiro.web;

import br.app.criati.financeiro.service.ResumoCategoriasFinanceiras;

public record ResumoCategoriasResponse(long ativas, long receitas, long despesas,
		long comOrcamento, long principais, long subcategorias) {
	public static ResumoCategoriasResponse from(ResumoCategoriasFinanceiras resumo) {
		return new ResumoCategoriasResponse(resumo.ativas(), resumo.receitas(), resumo.despesas(),
				resumo.comOrcamento(), resumo.principais(), resumo.subcategorias());
	}
}
