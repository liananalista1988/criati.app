package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.financeiro.model.InstituicaoFinanceira;

public record InstituicaoFinanceiraResponse(UUID id, String nome, String codigo, boolean global) {

	public static InstituicaoFinanceiraResponse from(InstituicaoFinanceira instituicao) {
		return new InstituicaoFinanceiraResponse(
				instituicao.getId(), instituicao.getNome(), instituicao.getCodigo(), instituicao.getEmpresa() == null);
	}
}
