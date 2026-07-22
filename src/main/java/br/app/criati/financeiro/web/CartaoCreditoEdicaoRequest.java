package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.UUID;

import br.app.criati.shared.enums.Bandeira;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartaoCreditoEdicaoRequest(
		@NotBlank(message = "Nome e obrigatorio") @Size(max = 150) String nome,
		@NotNull(message = "Titular e obrigatorio") UUID titularId,
		@NotNull(message = "Instituicao e obrigatoria") UUID instituicaoId,
		@NotNull(message = "Bandeira e obrigatoria") Bandeira bandeira,
		String ultimosQuatroDigitos,
		BigDecimal limiteTotal,
		BigDecimal limiteSaudavel,
		Integer diaFechamento,
		Integer diaVencimento,
		String observacao) {
}
