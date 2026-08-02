package br.app.criati.trabalho.web;

import java.time.LocalDate;
import java.util.UUID;

import br.app.criati.shared.enums.PrioridadeTrabalho;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessoEmpresarialRequest(
		@NotBlank(message = "Titulo e obrigatorio") @Size(max = 200) String titulo,
		String descricao,
		UUID responsavelId,
		PrioridadeTrabalho prioridade,
		LocalDate prazo) {
}
