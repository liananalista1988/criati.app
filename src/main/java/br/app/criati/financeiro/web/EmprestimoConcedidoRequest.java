package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import jakarta.validation.constraints.NotNull;

public record EmprestimoConcedidoRequest(
		@NotNull(message = "Pessoa devedora e obrigatoria") UUID parteFinanceiraId,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		String descricao,
		@NotNull(message = "Valor principal e obrigatorio") BigDecimal valorPrincipal,
		@NotNull(message = "Data de concessao e obrigatoria") LocalDate dataConcessao,
		@NotNull(message = "Configuracao de cobranca e obrigatoria") TipoCobrancaEmprestimo tipoCobranca,
		BigDecimal percentualJuros,
		BigDecimal percentualMulta,
		@NotNull(message = "Forma de pagamento e obrigatoria") FormaPagamentoEmprestimo formaPagamento,
		Integer quantidadeParcelas) {
}
