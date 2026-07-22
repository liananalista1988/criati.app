package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusPagamentoOcorrencia;
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
 * Liquidacao integral ou parcial de uma {@link OcorrenciaCompromisso}. Cada
 * pagamento gera exatamente um {@link LancamentoFinanceiro} de despesa
 * liquidado (referencia unica via lancamento_financeiro_id) — o unico ponto
 * onde o saldo das contas e efetivamente impactado. Exclusao fisica nao e
 * permitida: o estorno preserva o registro e marca status ESTORNADO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pagamento_ocorrencia_compromisso", uniqueConstraints = @UniqueConstraint(
		name = "uq_pagamento_ocorrencia_lancamento", columnNames = "lancamento_financeiro_id"))
public class PagamentoOcorrenciaCompromisso {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ocorrencia_id", nullable = false, updatable = false)
	private OcorrenciaCompromisso ocorrencia;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false, updatable = false)
	private ContaFinanceira conta;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valor;

	@Column(name = "data_pagamento", nullable = false, updatable = false)
	private LocalDate dataPagamento;

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
	private StatusPagamentoOcorrencia status;

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

	public PagamentoOcorrenciaCompromisso(Empresa empresa, OcorrenciaCompromisso ocorrencia, ContaFinanceira conta,
			BigDecimal valor, LocalDate dataPagamento, FormaPagamentoLancamento formaPagamento, String observacao,
			LancamentoFinanceiro lancamentoFinanceiro, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.ocorrencia = Objects.requireNonNull(ocorrencia, "ocorrencia nao pode ser nula");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento nao pode ser nula");
		this.formaPagamento = formaPagamento;
		this.observacao = observacao;
		this.lancamentoFinanceiro = Objects.requireNonNull(lancamentoFinanceiro, "lancamentoFinanceiro nao pode ser nulo");
		this.status = StatusPagamentoOcorrencia.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}

	public void estornar(String motivo, Usuario autor) {
		if (this.status == StatusPagamentoOcorrencia.ESTORNADO) {
			throw new DadosInvalidosException("Pagamento ja esta estornado");
		}
		this.status = StatusPagamentoOcorrencia.ESTORNADO;
		this.estornadoEm = OffsetDateTime.now();
		this.estornadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.motivoEstorno = motivo;
	}

	public boolean estaAtivo() {
		return status == StatusPagamentoOcorrencia.ATIVO;
	}
}
