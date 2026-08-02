package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.financeiro.service.DadosRegraClassificacaoImportacao;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.TipoFinanceiro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RegraClassificacaoImportacaoRequest(
		UUID contaId,
		@NotNull UUID categoriaId,
		UUID pessoaFinanceiraId,
		@NotBlank @Size(max = 500) String descricaoReferencia,
		@Size(max = 300) String padrao,
		@NotNull EstrategiaComparacaoRegraImportacao estrategiaComparacao,
		@PositiveOrZero int prioridade,
		@NotNull TipoFinanceiro tipo,
		FormaPagamentoLancamento formaPagamento,
		EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
		@NotNull NivelConfiancaRegraImportacao nivelConfianca,
		@NotNull AplicacaoRegraClassificacaoImportacao aplicacao) {

	DadosRegraClassificacaoImportacao toCommand() {
		return new DadosRegraClassificacaoImportacao(contaId, categoriaId, pessoaFinanceiraId, descricaoReferencia,
				padrao, estrategiaComparacao, prioridade, tipo, formaPagamento, encaminhamentoSugerido,
				nivelConfianca, aplicacao);
	}
}
