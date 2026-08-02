package br.app.criati.trabalho.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.TipoEventoHistoricoTrabalho;
import br.app.criati.trabalho.model.HistoricoTrabalho;

public record HistoricoTrabalhoResponse(UUID id, TipoEventoHistoricoTrabalho tipoEvento, String descricao,
		String valorAnterior, String valorNovo, String autorNome, OffsetDateTime ocorridoEm) {

	public static HistoricoTrabalhoResponse from(HistoricoTrabalho historico) {
		return new HistoricoTrabalhoResponse(historico.getId(), historico.getTipoEvento(), historico.getDescricao(),
				historico.getValorAnterior(), historico.getValorNovo(), historico.getAutor().getNome(),
				historico.getOcorridoEm());
	}
}
