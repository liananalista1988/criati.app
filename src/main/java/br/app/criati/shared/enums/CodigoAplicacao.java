package br.app.criati.shared.enums;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum CodigoAplicacao {
	FINANCEIRO("Gerenciador Financeiro", "Controle financeiro, receitas, despesas, contas e resultados",
			"financeiro", "/app/financeiro", SituacaoDisponibilidadeModulo.OPERACIONAL, 10, true),
	CLINICA("Gestão de Clínica", "Pacientes, profissionais, agenda e atendimentos",
			"clinica", "/app/clinica", SituacaoDisponibilidadeModulo.DEMONSTRACAO, 20, true),
	TAREFAS_PROCESSOS("Tarefas e Processos", "Organize processos, responsáveis, prazos e tarefas",
			"trabalho", "/app/trabalho/processos", SituacaoDisponibilidadeModulo.OPERACIONAL, 30, true),
	ESTOQUE("Estoque", "Controle de itens, movimentações e disponibilidade",
			"estoque", null, SituacaoDisponibilidadeModulo.INDISPONIVEL, 40, false);

	private final String nomeExibicao;
	private final String descricaoCurta;
	private final String chaveVisual;
	private final String rotaInicial;
	private final SituacaoDisponibilidadeModulo situacaoDisponibilidade;
	private final int ordemExibicao;
	private final boolean persistidoNoCatalogoAtual;

	CodigoAplicacao(String nomeExibicao, String descricaoCurta, String chaveVisual, String rotaInicial,
			SituacaoDisponibilidadeModulo situacaoDisponibilidade, int ordemExibicao,
			boolean persistidoNoCatalogoAtual) {
		this.nomeExibicao = nomeExibicao;
		this.descricaoCurta = descricaoCurta;
		this.chaveVisual = chaveVisual;
		this.rotaInicial = rotaInicial;
		this.situacaoDisponibilidade = situacaoDisponibilidade;
		this.ordemExibicao = ordemExibicao;
		this.persistidoNoCatalogoAtual = persistidoNoCatalogoAtual;
	}

	public String getNomeExibicao() { return nomeExibicao; }
	public String getDescricaoCurta() { return descricaoCurta; }
	public String getChaveVisual() { return chaveVisual; }
	public String getRotaInicial() { return rotaInicial; }
	public SituacaoDisponibilidadeModulo getSituacaoDisponibilidade() { return situacaoDisponibilidade; }
	public int getOrdemExibicao() { return ordemExibicao; }
	public boolean isPersistidoNoCatalogoAtual() { return persistidoNoCatalogoAtual; }

	public boolean estaDisponivelTecnicamente() {
		return situacaoDisponibilidade != SituacaoDisponibilidadeModulo.INDISPONIVEL && rotaInicial != null;
	}

	public static Optional<CodigoAplicacao> porCodigo(String codigo) {
		if (codigo == null) return Optional.empty();
		return Arrays.stream(values()).filter(item -> item.name().equals(codigo)).findFirst();
	}

	public static List<CodigoAplicacao> catalogoOrdenado() {
		return Arrays.stream(values()).sorted(Comparator.comparingInt(CodigoAplicacao::getOrdemExibicao)).toList();
	}
}
