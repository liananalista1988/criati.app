package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;

public record CartaoCreditoResponse(UUID id, String nome, UUID titularId, String titularNome, UUID instituicaoId,
		String instituicaoNome, TipoCartao tipo, UUID cartaoPrincipalId, String cartaoPrincipalNome,
		Bandeira bandeira, String ultimosQuatroDigitos, BigDecimal limiteTotal, BigDecimal limiteSaudavel,
		BigDecimal limiteComprometido, BigDecimal limiteDisponivel, Integer diaFechamento, Integer diaVencimento,
		StatusCadastro status, boolean bloqueado, boolean bloqueadoEfetivo, String motivoBloqueio,
		long quantidadeCartoesVirtuais, boolean possivelDuplicidade, String observacao, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static CartaoCreditoResponse from(CartaoCredito c, long quantidadeCartoesVirtuais,
			boolean possivelDuplicidade) {
		return new CartaoCreditoResponse(c.getId(), c.getNome(), c.getTitular().getId(), c.getTitular().getNome(),
				c.getInstituicao().getId(), c.getInstituicao().getNome(), c.getTipo(),
				c.getCartaoPrincipal() == null ? null : c.getCartaoPrincipal().getId(),
				c.getCartaoPrincipal() == null ? null : c.getCartaoPrincipal().getNome(), c.getBandeira(),
				c.getUltimosQuatroDigitos(), c.getLimiteTotalEfetivo(), c.getLimiteSaudavelEfetivo(),
				c.getLimiteComprometidoEfetivo(), c.getLimiteDisponivelEfetivo(), c.getDiaFechamentoEfetivo(),
				c.getDiaVencimentoEfetivo(), c.getStatus(), c.isBloqueado(), c.estaBloqueadoEfetivo(),
				c.getMotivoBloqueio(), quantidadeCartoesVirtuais, possivelDuplicidade, c.getObservacao(),
				c.getCriadoEm(), c.getAtualizadoEm(), c.getCriadoPor() == null ? null : c.getCriadoPor().getId(),
				c.getAtualizadoPor() == null ? null : c.getAtualizadoPor().getId());
	}
}
