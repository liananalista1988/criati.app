package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;
import jakarta.validation.constraints.NotNull;

/**
 * valor e opcional para tipo=INTEGRAL: o service sempre usa o saldo devido
 * calculado, ignorando qualquer valor informado - obrigatorio para PARCIAL e
 * MINIMO (validado no service, nao aqui, pois a obrigatoriedade depende do
 * tipo).
 */
public record RegistrarPagamentoFaturaRequest(
		@NotNull(message = "Conta de pagamento e obrigatoria") UUID contaPagamentoId,
		@NotNull(message = "Data de pagamento e obrigatoria") LocalDate dataPagamento,
		BigDecimal valor,
		@NotNull(message = "Tipo de pagamento e obrigatorio") TipoPagamentoFaturaCartao tipo,
		FormaPagamentoLancamento formaPagamento) {
}
