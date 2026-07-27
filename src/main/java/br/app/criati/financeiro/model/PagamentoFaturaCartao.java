package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;
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
 * Registro imutavel (insert-only) de cada pagamento (integral, parcial ou
 * minimo) de uma FaturaCartao - LES-F3-005. Cada pagamento gera exatamente um
 * LancamentoFinanceiro de despesa liquidado (origem FATURA, referencia unica
 * via lancamento_financeiro_id), mesmo padrao ja usado por
 * RecebimentoParcelaEmprestimo. Nunca altera ParcelaCompraCartao
 * individualmente - quem carrega os estados de pagamento e exclusivamente a
 * FaturaCartao (ver FaturaCartao#registrarPagamento). Estorno e cancelamento
 * sao escopo de LES-F3-006: nao ha campo de status aqui de proposito, todo
 * registro criado e permanente nesta entrega.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pagamento_fatura_cartao", uniqueConstraints = @UniqueConstraint(
		name = "uq_pagamento_fatura_cartao_lancamento", columnNames = "lancamento_financeiro_id"))
public class PagamentoFaturaCartao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "fatura_id", nullable = false, updatable = false)
	private FaturaCartao fatura;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_pagamento_id", nullable = false, updatable = false)
	private ContaFinanceira contaPagamento;

	@Column(name = "data_pagamento", nullable = false, updatable = false)
	private LocalDate dataPagamento;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valor;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20, updatable = false)
	private TipoPagamentoFaturaCartao tipo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lancamento_financeiro_id", nullable = false, updatable = false)
	private LancamentoFinanceiro lancamentoFinanceiro;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false, updatable = false)
	private Usuario criadoPor;

	public PagamentoFaturaCartao(Empresa empresa, FaturaCartao fatura, ContaFinanceira contaPagamento,
			LocalDate dataPagamento, BigDecimal valor, TipoPagamentoFaturaCartao tipo,
			LancamentoFinanceiro lancamentoFinanceiro, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.fatura = Objects.requireNonNull(fatura, "fatura nao pode ser nula");
		this.contaPagamento = Objects.requireNonNull(contaPagamento, "contaPagamento nao pode ser nula");
		this.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.lancamentoFinanceiro = Objects.requireNonNull(lancamentoFinanceiro, "lancamentoFinanceiro nao pode ser nulo");
		this.criadoEm = OffsetDateTime.now();
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}
}
