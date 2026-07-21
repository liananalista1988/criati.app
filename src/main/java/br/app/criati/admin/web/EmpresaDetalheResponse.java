package br.app.criati.admin.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import br.app.criati.shared.enums.StatusCadastro;

public record EmpresaDetalheResponse(
		UUID id,
		String nome,
		String nomeFantasia,
		String cnpj,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm,
		long quantidadeUsuariosAtivos,
		long administradoresAtivos,
		List<String> aplicacoesHabilitadas,
		long convitesPendentes,
		SituacaoOperacional situacaoOperacional) {

	// Calculada em memoria (nao persistida): nenhum campo novo no banco.
	// PRONTA exige simultaneamente empresa ativa, ao menos um Administrador
	// ativo e ao menos uma aplicacao habilitada; qualquer ausencia disso (ou
	// convite de Administrador ainda pendente) mantem a empresa PENDENTE.
	public enum SituacaoOperacional {
		PRONTA,
		PENDENTE
	}
}
