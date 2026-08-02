package br.app.criati.trabalho.service;

import java.util.List;

import br.app.criati.trabalho.model.ProcessoEmpresarial;

public record PaginaProcessosEmpresariais(List<ProcessoEmpresarial> itens, int pagina, int tamanho,
		long totalElementos, int totalPaginas, String ordenarPor, String direcao) {
}
