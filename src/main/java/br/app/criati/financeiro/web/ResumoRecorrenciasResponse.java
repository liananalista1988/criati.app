package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.util.List;

import br.app.criati.financeiro.service.ProximaOcorrenciaResumo;
import br.app.criati.financeiro.service.ResumoRecorrenciasFinanceiras;

public record ResumoRecorrenciasResponse(long ativas, long pausadas, long encerradas,
		BigDecimal receitasRecorrentesPrevistas, BigDecimal despesasRecorrentesPrevistas,
		long ocorrenciasGeradasNaCompetencia, List<ProximaOcorrenciaResumo> proximasOcorrencias) {

	public static ResumoRecorrenciasResponse from(ResumoRecorrenciasFinanceiras r) {
		return new ResumoRecorrenciasResponse(r.ativas(), r.pausadas(), r.encerradas(),
				r.receitasRecorrentesPrevistas(), r.despesasRecorrentesPrevistas(), r.ocorrenciasGeradasNaCompetencia(),
				r.proximasOcorrencias());
	}
}
