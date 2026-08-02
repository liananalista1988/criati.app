package br.app.criati.financeiro.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Reconhece e classifica movimentacoes ja importadas de um extrato bancario,
 * comparando o padrao normalizado contra a descricao da transacao
 * (RegraClassificacaoImportacaoService). Nunca gera lancamento, pagamento de
 * fatura ou compromisso sozinha - so pre-preenche (AUTOMATICA) ou sugere
 * (SUGESTAO) dados que o usuario ainda precisa confirmar explicitamente em
 * ConfirmacaoImportacaoBancariaService.confirmar. Distinta de
 * RecorrenciaFinanceira, que gera compromissos financeiros futuros: esta
 * entidade nunca cria nada por conta propria, apenas reconhece o que ja foi
 * importado (CRIATI-IMP-002A).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "regra_classificacao_importacao")
public class RegraClassificacaoImportacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_id")
	private ContaFinanceira conta;

	@Column(name = "descricao_referencia", nullable = false, length = 500)
	private String descricaoReferencia;

	@Column(name = "padrao_normalizado", nullable = false, length = 300)
	private String padraoNormalizado;

	@Enumerated(EnumType.STRING)
	@Column(name = "estrategia_comparacao", nullable = false, length = 20)
	private EstrategiaComparacaoRegraImportacao estrategiaComparacao;

	@Column(name = "prioridade", nullable = false)
	private int prioridade;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoFinanceiro tipo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaFinanceira categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pessoa_financeira_id")
	private PessoaFinanceira pessoaFinanceira;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", length = 30)
	private FormaPagamentoLancamento formaPagamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "encaminhamento_sugerido", length = 30)
	private EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido;

	@Enumerated(EnumType.STRING)
	@Column(name = "nivel_confianca", nullable = false, length = 20)
	private NivelConfiancaRegraImportacao nivelConfianca;

	@Enumerated(EnumType.STRING)
	@Column(name = "aplicacao", nullable = false, length = 20)
	private AplicacaoRegraClassificacaoImportacao aplicacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

	@Column(name = "quantidade_utilizacoes", nullable = false)
	private int quantidadeUtilizacoes;

	@Column(name = "ultima_utilizacao_em")
	private OffsetDateTime ultimaUtilizacaoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id")
	private Usuario atualizadoPor;

	public RegraClassificacaoImportacao(
			Empresa empresa,
			ContaFinanceira conta,
			String descricaoReferencia,
			String padraoNormalizado,
			EstrategiaComparacaoRegraImportacao estrategiaComparacao,
			int prioridade,
			TipoFinanceiro tipo,
			CategoriaFinanceira categoria,
			PessoaFinanceira pessoaFinanceira,
			FormaPagamentoLancamento formaPagamento,
			EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
			NivelConfiancaRegraImportacao nivelConfianca,
			AplicacaoRegraClassificacaoImportacao aplicacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.conta = conta;
		this.descricaoReferencia = Objects.requireNonNull(descricaoReferencia, "descricaoReferencia nao pode ser nula");
		this.padraoNormalizado = Objects.requireNonNull(padraoNormalizado, "padraoNormalizado nao pode ser nulo");
		this.estrategiaComparacao = Objects.requireNonNull(estrategiaComparacao, "estrategiaComparacao nao pode ser nula");
		this.prioridade = prioridade;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = pessoaFinanceira;
		this.formaPagamento = formaPagamento;
		this.encaminhamentoSugerido = encaminhamentoSugerido;
		this.nivelConfianca = Objects.requireNonNull(nivelConfianca, "nivelConfianca nao pode ser nula");
		this.aplicacao = Objects.requireNonNull(aplicacao, "aplicacao nao pode ser nula");
		this.status = StatusCadastro.ATIVO;
		this.quantidadeUtilizacoes = 0;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}

	public void atualizarDados(
			ContaFinanceira conta,
			String descricaoReferencia,
			String padraoNormalizado,
			EstrategiaComparacaoRegraImportacao estrategiaComparacao,
			int prioridade,
			TipoFinanceiro tipo,
			CategoriaFinanceira categoria,
			PessoaFinanceira pessoaFinanceira,
			FormaPagamentoLancamento formaPagamento,
			EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
			NivelConfiancaRegraImportacao nivelConfianca,
			AplicacaoRegraClassificacaoImportacao aplicacao,
			Usuario autor) {
		this.conta = conta;
		this.descricaoReferencia = Objects.requireNonNull(descricaoReferencia, "descricaoReferencia nao pode ser nula");
		this.padraoNormalizado = Objects.requireNonNull(padraoNormalizado, "padraoNormalizado nao pode ser nulo");
		this.estrategiaComparacao = Objects.requireNonNull(estrategiaComparacao, "estrategiaComparacao nao pode ser nula");
		this.prioridade = prioridade;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = pessoaFinanceira;
		this.formaPagamento = formaPagamento;
		this.encaminhamentoSugerido = encaminhamentoSugerido;
		this.nivelConfianca = Objects.requireNonNull(nivelConfianca, "nivelConfianca nao pode ser nula");
		this.aplicacao = Objects.requireNonNull(aplicacao, "aplicacao nao pode ser nula");
		registrarAlteracao(autor);
	}

	public void desativar(Usuario autor) {
		this.status = StatusCadastro.INATIVO;
		registrarAlteracao(autor);
	}

	public void reativar(Usuario autor) {
		this.status = StatusCadastro.ATIVO;
		registrarAlteracao(autor);
	}

	public boolean estaAtiva() {
		return status == StatusCadastro.ATIVO;
	}

	/**
	 * So deve ser chamado quando a classificacao proposta por esta regra foi
	 * efetivamente aceita numa confirmacao (nunca numa simples sugestao
	 * mostrada e ainda nao confirmada) - ver ajuste 7 de CRIATI-IMP-002A.
	 */
	public void registrarUso(Usuario autor) {
		this.quantidadeUtilizacoes = this.quantidadeUtilizacoes + 1;
		this.ultimaUtilizacaoEm = OffsetDateTime.now();
		registrarAlteracao(autor);
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
