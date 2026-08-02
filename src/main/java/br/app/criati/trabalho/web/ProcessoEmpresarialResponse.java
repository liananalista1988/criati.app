package br.app.criati.trabalho.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoProcessoTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.trabalho.model.ProcessoEmpresarial;
import br.app.criati.trabalho.service.ProcessoEmpresarialService.ContagemTarefasProcesso;

public record ProcessoEmpresarialResponse(UUID id, String titulo, String descricao, UUID responsavelId,
		String responsavelNome, SituacaoProcessoTrabalho situacao, PrioridadeTrabalho prioridade,
		LocalDate dataAbertura, LocalDate prazo, OffsetDateTime dataConclusao, boolean atrasado,
		long quantidadeTarefas, long quantidadeTarefasConcluidas, StatusCadastro status, OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm, UUID criadoPorUsuarioId, UUID atualizadoPorUsuarioId) {

	public static ProcessoEmpresarialResponse from(ProcessoEmpresarial processo, ContagemTarefasProcesso tarefas) {
		return new ProcessoEmpresarialResponse(processo.getId(), processo.getTitulo(), processo.getDescricao(),
				processo.getResponsavel() == null ? null : processo.getResponsavel().getId(),
				processo.getResponsavel() == null ? null : processo.getResponsavel().getUsuario().getNome(),
				processo.getSituacao(), processo.getPrioridade(), processo.getDataAbertura(), processo.getPrazo(),
				processo.getDataConclusao(), processo.estaAtrasado(), tarefas.total(), tarefas.concluidas(),
				processo.getStatus(), processo.getCriadoEm(), processo.getAtualizadoEm(),
				processo.getCriadoPor().getId(), processo.getAtualizadoPor() == null ? null : processo.getAtualizadoPor().getId());
	}
}
