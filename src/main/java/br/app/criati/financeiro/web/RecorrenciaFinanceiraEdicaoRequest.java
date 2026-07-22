package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecorrenciaFinanceiraEdicaoRequest(
		@NotBlank(message = "Descricao e obrigatoria") String descricao,
		@NotNull(message = "Valor padrao e obrigatorio") BigDecimal valorPadrao,
		@NotNull(message = "Conta e obrigatoria") UUID contaId,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		@NotNull(message = "Pessoa financeira e obrigatoria") UUID pessoaFinanceiraId,
		UUID parteFinanceiraId,
		FormaPagamentoLancamento formaPagamento,
		@NotNull(message = "Intervalo e obrigatorio") Integer intervalo,
		@NotNull(message = "Dia de referencia e obrigatorio") Integer dia,
		Integer mes,
		LocalDate dataFinal,
		boolean gerarAutomaticamente,
		String observacao) {
}
