package br.app.criati.trabalho.service;

import java.util.List;

import br.app.criati.trabalho.model.TarefaEmpresarial;

public record PaginaTarefasEmpresariais(List<TarefaEmpresarial> itens, int pagina, int tamanho,
		long totalElementos, int totalPaginas, String ordenarPor, String direcao) {
}
