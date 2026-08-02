package br.app.criati.financeiro.web;

import java.util.List;

import br.app.criati.financeiro.service.PaginaLancamentosFinanceiros;

public record PaginaLancamentosResponse(List<LancamentoFinanceiroResponse> itens, int pagina, int tamanho,
		long totalElementos, int totalPaginas, String ordenarPor, String direcao) {

	public static PaginaLancamentosResponse from(PaginaLancamentosFinanceiros pagina) {
		return new PaginaLancamentosResponse(
				pagina.itens().stream().map(LancamentoFinanceiroResponse::from).toList(),
				pagina.pagina(), pagina.tamanho(), pagina.totalElementos(), pagina.totalPaginas(),
				pagina.ordenarPor(), pagina.direcao());
	}
}
