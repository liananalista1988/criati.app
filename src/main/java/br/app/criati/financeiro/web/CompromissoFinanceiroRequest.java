package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.TipoValorCompromisso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompromissoFinanceiroRequest(
		@NotBlank(message = "Descricao e obrigatoria") String descricao,
		@NotNull(message = "Categoria e obrigatoria") UUID categoriaId,
		@NotNull(message = "Pessoa financeira e obrigatoria") UUID pessoaFinanceiraId,
		UUID parteFinanceiraId,
		UUID contaPadraoId,
		UUID recorrenciaId,
		@NotNull(message = "Tipo de valor e obrigatorio") TipoValorCompromisso tipoValor,
		BigDecimal valorPadrao,
		Integer diaVencimentoPadrao,
		FormaPagamentoLancamento formaPagamentoPadrao,
		String observacao) {
}
