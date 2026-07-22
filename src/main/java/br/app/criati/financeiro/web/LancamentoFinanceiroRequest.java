package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LancamentoFinanceiroRequest(
		@NotNull(message = "Conta e obrigatoria") UUID contaId,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		@NotNull(message = "Tipo e obrigatorio") TipoFinanceiro tipo,
		@NotBlank(message = "Descricao e obrigatoria") String descricao,
		@NotNull(message = "Valor e obrigatorio") BigDecimal valor,
		@NotNull(message = "Data de competencia e obrigatoria") LocalDate dataCompetencia,
		UUID pessoaFinanceiraId,
		UUID parteFinanceiraId,
		LocalDate dataVencimento,
		StatusLancamentoFinanceiro status,
		LocalDate dataPagamento,
		LocalDate dataLiquidacao,
		FormaPagamentoLancamento formaPagamento,
		String observacao) {

	public LocalDate dataLiquidacaoEfetiva() {
		return dataLiquidacao != null ? dataLiquidacao : dataPagamento;
	}
}
