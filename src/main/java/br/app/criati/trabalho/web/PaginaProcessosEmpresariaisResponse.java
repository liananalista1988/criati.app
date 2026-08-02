package br.app.criati.trabalho.web;

import java.util.List;

public record PaginaProcessosEmpresariaisResponse(List<ProcessoEmpresarialResponse> itens, int pagina, int tamanho,
		long totalElementos, int totalPaginas, String ordenarPor, String direcao) {
}
