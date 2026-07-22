package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lancamento_financeiro", uniqueConstraints = @UniqueConstraint(
		name = "uq_lancamento_financeiro_recorrencia_competencia",
		columnNames = {"recorrencia_id", "data_competencia"}))
public class LancamentoFinanceiro {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false)
	private ContaFinanceira conta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaFinanceira categoria;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pessoa_financeira_id")
	private PessoaFinanceira pessoaFinanceira;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parte_financeira_id")
	private ParteFinanceira parteFinanceira;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoFinanceiro tipo;

	@Column(name = "descricao", nullable = false, length = 200)
	private String descricao;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2)
	private BigDecimal valor;

	@Column(name = "data_competencia", nullable = false)
	private LocalDate dataCompetencia;

	@Column(name = "data_pagamento")
	private LocalDate dataPagamento;

	@Column(name = "data_vencimento")
	private LocalDate dataVencimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", length = 30)
	private FormaPagamentoLancamento formaPagamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "origem", nullable = false, length = 30)
	private OrigemLancamentoFinanceiro origem;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusLancamentoFinanceiro status;

	@Column(name = "observacao", length = 500)
	private String observacao;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "criado_por_usuario_id", updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id")
	private Usuario atualizadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorrencia_id")
	private RecorrenciaFinanceira recorrencia;

	public LancamentoFinanceiro(
			Empresa empresa,
			ContaFinanceira conta,
			CategoriaFinanceira categoria,
			TipoFinanceiro tipo,
			String descricao,
			BigDecimal valor,
			LocalDate dataCompetencia,
			LocalDate dataPagamento,
			StatusLancamentoFinanceiro status,
			String observacao) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataCompetencia = Objects.requireNonNull(dataCompetencia, "dataCompetencia nao pode ser nula");
		this.dataPagamento = dataPagamento;
		this.origem = OrigemLancamentoFinanceiro.MANUAL;
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
	}

	public LancamentoFinanceiro(Empresa empresa, ContaFinanceira conta, CategoriaFinanceira categoria,
			PessoaFinanceira pessoaFinanceira, ParteFinanceira parteFinanceira, TipoFinanceiro tipo,
			String descricao, BigDecimal valor, LocalDate dataCompetencia, LocalDate dataVencimento,
			LocalDate dataLiquidacao, StatusLancamentoFinanceiro status,
			FormaPagamentoLancamento formaPagamento, String observacao, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = pessoaFinanceira;
		this.parteFinanceira = parteFinanceira;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataCompetencia = Objects.requireNonNull(dataCompetencia, "dataCompetencia nao pode ser nula");
		this.dataVencimento = dataVencimento;
		this.dataPagamento = dataLiquidacao;
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
		this.formaPagamento = formaPagamento;
		this.origem = OrigemLancamentoFinanceiro.MANUAL;
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public static LancamentoFinanceiro gerarDeRecorrencia(RecorrenciaFinanceira recorrencia, LocalDate dataCompetencia,
			LocalDate dataVencimento, Usuario autor) {
		Objects.requireNonNull(recorrencia, "recorrencia nao pode ser nula");
		LancamentoFinanceiro lancamento = new LancamentoFinanceiro(recorrencia.getEmpresa(), recorrencia.getConta(),
				recorrencia.getCategoria(), recorrencia.getPessoaFinanceira(), recorrencia.getParteFinanceira(),
				recorrencia.getTipo(), recorrencia.getDescricao(), recorrencia.getValorPadrao(), dataCompetencia,
				dataVencimento, null, StatusLancamentoFinanceiro.PENDENTE, recorrencia.getFormaPagamento(), null,
				autor);
		lancamento.origem = OrigemLancamentoFinanceiro.RECORRENCIA;
		lancamento.recorrencia = recorrencia;
		return lancamento;
	}

	/**
	 * Gera o lancamento de despesa liquidado que representa o impacto financeiro
	 * unico de um pagamento (integral ou parcial) de uma ocorrencia de
	 * compromisso a pagar. Deliberadamente nao referencia recorrencia_id mesmo
	 * quando a ocorrencia se origina de uma recorrencia: mais de um pagamento
	 * parcial na mesma competencia geraria mais de um lancamento com o mesmo par
	 * (recorrencia_id, data_competencia), colidindo com
	 * uq_lancamento_financeiro_recorrencia_competencia (criada para o fluxo
	 * recorrencia -> lancamento direto da LES-F2-006, que nao se aplica aqui). A
	 * rastreabilidade fica a cargo de PagamentoOcorrenciaCompromisso e
	 * OcorrenciaCompromisso (compromisso_id/recorrencia_id/competencia).
	 */
	public static LancamentoFinanceiro gerarDeContaAPagar(Empresa empresa, ContaFinanceira conta,
			CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira, ParteFinanceira parteFinanceira,
			String descricao, BigDecimal valor, LocalDate dataCompetencia, LocalDate dataVencimento,
			LocalDate dataLiquidacao, FormaPagamentoLancamento formaPagamento, Usuario autor) {
		LancamentoFinanceiro lancamento = new LancamentoFinanceiro(empresa, conta, categoria, pessoaFinanceira,
				parteFinanceira, TipoFinanceiro.DESPESA, descricao, valor, dataCompetencia, dataVencimento,
				dataLiquidacao, StatusLancamentoFinanceiro.LIQUIDADO, formaPagamento, null, autor);
		lancamento.origem = OrigemLancamentoFinanceiro.CONTA_A_PAGAR;
		return lancamento;
	}

	public void atualizarDados(
			ContaFinanceira conta,
			CategoriaFinanceira categoria,
			String descricao,
			BigDecimal valor,
			LocalDate dataCompetencia,
			String observacao) {
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataCompetencia = Objects.requireNonNull(dataCompetencia, "dataCompetencia nao pode ser nula");
		this.observacao = observacao;
	}

	public void atualizarDados(ContaFinanceira conta, CategoriaFinanceira categoria,
			PessoaFinanceira pessoaFinanceira, ParteFinanceira parteFinanceira, TipoFinanceiro tipo,
			String descricao, BigDecimal valor, LocalDate dataCompetencia, LocalDate dataVencimento,
			LocalDate dataLiquidacao, FormaPagamentoLancamento formaPagamento, String observacao, Usuario autor) {
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = pessoaFinanceira;
		this.parteFinanceira = parteFinanceira;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataCompetencia = Objects.requireNonNull(dataCompetencia, "dataCompetencia nao pode ser nula");
		this.dataVencimento = dataVencimento;
		this.dataPagamento = dataLiquidacao;
		this.formaPagamento = formaPagamento;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public void pagar(LocalDate dataPagamento) {
		this.status = StatusLancamentoFinanceiro.PAGO;
		this.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento nao pode ser nula");
	}

	public void pagar(LocalDate dataPagamento, Usuario autor) {
		pagar(dataPagamento);
		registrarAlteracao(autor);
	}

	public void liquidar(LocalDate dataLiquidacao, FormaPagamentoLancamento formaPagamento, Usuario autor) {
		this.status = StatusLancamentoFinanceiro.LIQUIDADO;
		this.dataPagamento = Objects.requireNonNull(dataLiquidacao, "dataLiquidacao nao pode ser nula");
		this.formaPagamento = formaPagamento;
		registrarAlteracao(autor);
	}

	public void reabrir() {
		this.status = StatusLancamentoFinanceiro.PENDENTE;
		this.dataPagamento = null;
	}

	public void desliquidar(Usuario autor) {
		reabrir();
		registrarAlteracao(autor);
	}

	public void reabrir(Usuario autor) {
		desliquidar(autor);
	}

	public void cancelar() {
		this.status = StatusLancamentoFinanceiro.CANCELADO;
		this.dataPagamento = null;
	}

	public void cancelar(Usuario autor) {
		cancelar();
		registrarAlteracao(autor);
	}

	public boolean compoeSaldoRealizado() {
		return status == StatusLancamentoFinanceiro.PAGO || status == StatusLancamentoFinanceiro.LIQUIDADO;
	}

	public LocalDate getDataLiquidacao() {
		return dataPagamento;
	}

	public boolean estaVencido(LocalDate hoje) {
		return status == StatusLancamentoFinanceiro.PENDENTE && dataVencimento != null
				&& dataVencimento.isBefore(Objects.requireNonNull(hoje));
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
