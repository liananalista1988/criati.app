package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusRecebimentoParcelaEmprestimo;
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

/**
 * Liquidacao (integral ou parcial) de uma ParcelaEmprestimo. Cada recebimento
 * gera exatamente um LancamentoFinanceiro de receita liquidado (origem
 * EMPRESTIMO_CONCEDIDO, referencia unica via lancamento_financeiro_id) — o
 * unico ponto onde o saldo das contas e efetivamente impactado, reaproveitando
 * a mesma estrutura de lancamentos ja usada pelo restante do modulo
 * Financeiro. Exclusao fisica nao e permitida: o estorno preserva o registro e
 * marca status ESTORNADO, mesmo padrao ja usado por
 * PagamentoOcorrenciaCompromisso.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "recebimento_parcela_emprestimo", uniqueConstraints = @UniqueConstraint(
		name = "uq_recebimento_parcela_emprestimo_lancamento", columnNames = "lancamento_financeiro_id"))
public class RecebimentoParcelaEmprestimo {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parcela_id", nullable = false, updatable = false)
	private ParcelaEmprestimo parcela;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false, updatable = false)
	private ContaFinanceira conta;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valor;

	@Column(name = "data_recebimento", nullable = false, updatable = false)
	private LocalDate dataRecebimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", length = 30, updatable = false)
	private FormaPagamentoLancamento formaPagamento;

	@Column(name = "observacao", length = 500, updatable = false)
	private String observacao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lancamento_financeiro_id", nullable = false, updatable = false)
	private LancamentoFinanceiro lancamentoFinanceiro;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusRecebimentoParcelaEmprestimo status;

	@Column(name = "estornado_em")
	private OffsetDateTime estornadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "estornado_por_usuario_id")
	private Usuario estornadoPor;

	@Column(name = "motivo_estorno", length = 500)
	private String motivoEstorno;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false, updatable = false)
	private Usuario criadoPor;

	public RecebimentoParcelaEmprestimo(Empresa empresa, ParcelaEmprestimo parcela, ContaFinanceira conta,
			BigDecimal valor, LocalDate dataRecebimento, FormaPagamentoLancamento formaPagamento, String observacao,
			LancamentoFinanceiro lancamentoFinanceiro, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.parcela = Objects.requireNonNull(parcela, "parcela nao pode ser nula");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataRecebimento = Objects.requireNonNull(dataRecebimento, "dataRecebimento nao pode ser nula");
		this.formaPagamento = formaPagamento;
		this.observacao = observacao;
		this.lancamentoFinanceiro = Objects.requireNonNull(lancamentoFinanceiro, "lancamentoFinanceiro nao pode ser nulo");
		this.status = StatusRecebimentoParcelaEmprestimo.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}

	public void estornar(String motivo, Usuario autor) {
		if (this.status == StatusRecebimentoParcelaEmprestimo.ESTORNADO) {
			throw new DadosInvalidosException("Recebimento ja esta estornado");
		}
		this.status = StatusRecebimentoParcelaEmprestimo.ESTORNADO;
		this.estornadoEm = OffsetDateTime.now();
		this.estornadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.motivoEstorno = motivo;
	}

	public boolean estaAtivo() {
		return status == StatusRecebimentoParcelaEmprestimo.ATIVO;
	}
}
