package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;

public record ResumoRecorrenciasFinanceiras(long ativas, long pausadas, long encerradas,
		BigDecimal receitasRecorrentesPrevistas, BigDecimal despesasRecorrentesPrevistas,
		long ocorrenciasGeradasNaCompetencia, List<ProximaOcorrenciaResumo> proximasOcorrencias) {
}
