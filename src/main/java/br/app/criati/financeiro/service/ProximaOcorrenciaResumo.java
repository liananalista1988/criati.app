package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import br.app.criati.shared.enums.TipoFinanceiro;

public record ProximaOcorrenciaResumo(UUID recorrenciaId, String descricao, TipoFinanceiro tipo,
		BigDecimal valorPadrao, YearMonth proximaCompetencia) {
}
