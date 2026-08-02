package br.app.criati.trabalho.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.trabalho.model.TarefaEmpresarial;

public record TarefaEmpresarialResponse(UUID id, UUID processoId, String processoTitulo, String titulo,
		String descricao, UUID responsavelId, String responsavelNome, SituacaoTarefaTrabalho situacao,
		PrioridadeTrabalho prioridade, LocalDate prazo, OffsetDateTime dataConclusao, boolean atrasada,
		StatusCadastro status, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId,
		UUID atualizadoPorUsuarioId) {

	public static TarefaEmpresarialResponse from(TarefaEmpresarial tarefa) {
		return new TarefaEmpresarialResponse(tarefa.getId(),
				tarefa.getProcesso() == null ? null : tarefa.getProcesso().getId(),
				tarefa.getProcesso() == null ? null : tarefa.getProcesso().getTitulo(),
				tarefa.getTitulo(), tarefa.getDescricao(),
				tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getId(),
				tarefa.getResponsavel() == null ? null : tarefa.getResponsavel().getUsuario().getNome(),
				tarefa.getSituacao(), tarefa.getPrioridade(), tarefa.getPrazo(), tarefa.getDataConclusao(),
				tarefa.estaAtrasada(), tarefa.getStatus(), tarefa.getCriadoEm(), tarefa.getAtualizadoEm(),
				tarefa.getCriadoPor().getId(), tarefa.getAtualizadoPor() == null ? null : tarefa.getAtualizadoPor().getId());
	}
}
