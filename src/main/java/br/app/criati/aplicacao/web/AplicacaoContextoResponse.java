package br.app.criati.aplicacao.web;

import br.app.criati.aplicacao.service.ModuloDisponivelEmpresa;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.SituacaoDisponibilidadeModulo;
import br.app.criati.shared.enums.StatusCadastro;

public record AplicacaoContextoResponse(
		String codigo,
		String nome,
		String descricao,
		StatusCadastro status,
		String urlInicial,
		String chaveVisual,
		SituacaoDisponibilidadeModulo situacaoDisponibilidade,
		int ordemExibicao) {

	public static AplicacaoContextoResponse from(ModuloDisponivelEmpresa disponivel) {
		CodigoAplicacao modulo = disponivel.modulo();
		return new AplicacaoContextoResponse(
				modulo.name(), modulo.getNomeExibicao(), modulo.getDescricaoCurta(), StatusCadastro.ATIVO,
				modulo.getRotaInicial(), modulo.getChaveVisual(), modulo.getSituacaoDisponibilidade(),
				modulo.getOrdemExibicao());
	}
}
