package br.app.criati.aplicacao.web;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.StatusCadastro;

public record AplicacaoContextoResponse(
		String codigo,
		String nome,
		String descricao,
		StatusCadastro status,
		String urlInicial) {

	public static AplicacaoContextoResponse from(Aplicacao aplicacao) {
		return new AplicacaoContextoResponse(
				aplicacao.getCodigo(),
				aplicacao.getNome(),
				aplicacao.getDescricao(),
				aplicacao.getStatus(),
				urlInicial(aplicacao.getCodigo()));
	}

	// Mapeamento explicito codigo -> rota inicial: os unicos dois codigos que
	// existem hoje no catalogo (ver V4__criar_estrutura_aplicacoes.sql). Um
	// codigo futuro sem rota conhecida cai no catalogo de aplicacoes, nunca em
	// uma URL quebrada.
	private static String urlInicial(String codigo) {
		if (CodigoAplicacao.FINANCEIRO.name().equals(codigo)) {
			return "/app/financeiro";
		}
		if (CodigoAplicacao.CLINICA.name().equals(codigo)) {
			return "/app/clinica";
		}
		return "/app/aplicacoes";
	}
}
