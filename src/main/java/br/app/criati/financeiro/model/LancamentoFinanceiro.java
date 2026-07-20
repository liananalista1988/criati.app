package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lancamento_financeiro")
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

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusLancamentoFinanceiro status;

	@Column(name = "observacao", length = 500)
	private String observacao;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

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
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
		this.observacao = observacao;
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

	public void pagar(LocalDate dataPagamento) {
		this.status = StatusLancamentoFinanceiro.PAGO;
		this.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento nao pode ser nula");
	}

	public void reabrir() {
		this.status = StatusLancamentoFinanceiro.PENDENTE;
		this.dataPagamento = null;
	}

	public void cancelar() {
		this.status = StatusLancamentoFinanceiro.CANCELADO;
	}

	public boolean compoeSaldoRealizado() {
		return status == StatusLancamentoFinanceiro.PAGO;
	}
}
