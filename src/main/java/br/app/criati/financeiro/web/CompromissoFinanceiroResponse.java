package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.CompromissoFinanceiro;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.TipoValorCompromisso;

public record CompromissoFinanceiroResponse(UUID id, String descricao, UUID categoriaId, String categoriaNome,
		UUID pessoaFinanceiraId, String pessoaFinanceiraNome, UUID parteFinanceiraId, String parteFinanceiraNome,
		UUID contaPadraoId, String contaPadraoNome, UUID recorrenciaId, TipoValorCompromisso tipoValor,
		BigDecimal valorPadrao, Integer diaVencimentoPadrao, FormaPagamentoLancamento formaPagamentoPadrao,
		boolean ativo, String observacao, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm,
		UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static CompromissoFinanceiroResponse from(CompromissoFinanceiro c) {
		return new CompromissoFinanceiroResponse(c.getId(), c.getDescricao(), c.getCategoria().getId(),
				c.getCategoria().getNome(), c.getPessoaFinanceira().getId(), c.getPessoaFinanceira().getNome(),
				c.getParteFinanceira() == null ? null : c.getParteFinanceira().getId(),
				c.getParteFinanceira() == null ? null : c.getParteFinanceira().getNome(),
				c.getContaPadrao() == null ? null : c.getContaPadrao().getId(),
				c.getContaPadrao() == null ? null : c.getContaPadrao().getNome(),
				c.getRecorrencia() == null ? null : c.getRecorrencia().getId(), c.getTipoValor(), c.getValorPadrao(),
				c.getDiaVencimentoPadrao(), c.getFormaPagamentoPadrao(), c.isAtivo(), c.getObservacao(),
				c.getCriadoEm(), c.getAtualizadoEm(), c.getCriadoPor() == null ? null : c.getCriadoPor().getId(),
				c.getAtualizadoPor() == null ? null : c.getAtualizadoPor().getId());
	}
}
