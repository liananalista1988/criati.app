package br.app.criati.financeiro.shared.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public record PessoaFinanceiraResponse(
		UUID id,
		String nome,
		String apelido,
		UUID usuarioId,
		String usuarioNome,
		String usuarioEmail,
		StatusCadastro status,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	public static PessoaFinanceiraResponse from(PessoaFinanceira pessoa) {
		return new PessoaFinanceiraResponse(
				pessoa.getId(),
				pessoa.getNome(),
				pessoa.getApelido(),
				pessoa.getUsuario() == null ? null : pessoa.getUsuario().getId(),
				pessoa.getUsuario() == null ? null : pessoa.getUsuario().getNome(),
				pessoa.getUsuario() == null ? null : pessoa.getUsuario().getEmail(),
				pessoa.getStatus(),
				pessoa.getCriadoEm(),
				pessoa.getAtualizadoEm());
	}
}
