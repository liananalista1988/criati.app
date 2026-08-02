package br.app.criati.financeiro.service;

import java.util.List;

import br.app.criati.financeiro.model.LancamentoFinanceiro;

public record PaginaLancamentosFinanceiros(List<LancamentoFinanceiro> itens, int pagina, int tamanho,
		long totalElementos, int totalPaginas, String ordenarPor, String direcao) {
}
