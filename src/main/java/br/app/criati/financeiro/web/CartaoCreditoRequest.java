package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.TipoCartao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartaoCreditoRequest(
		@NotBlank(message = "Nome e obrigatorio") @Size(max = 150) String nome,
		UUID titularId,
		UUID instituicaoId,
		@NotNull(message = "Tipo e obrigatorio") TipoCartao tipo,
		UUID cartaoPrincipalId,
		Bandeira bandeira,
		String ultimosQuatroDigitos,
		BigDecimal limiteTotal,
		BigDecimal limiteSaudavel,
		Integer diaFechamento,
		Integer diaVencimento,
		String observacao) {
}
