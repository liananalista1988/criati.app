package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.StatusRessarcimentoParcelaCartao;
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
 * Liquidacao (integral ou parcial) de um {@link ValorAReceberParcelaCartao}.
 * Nao gera LancamentoFinanceiro: o principal ressarcido nao e receita nem
 * despesa da residencia (o modelo hoje so tem TipoFinanceiro RECEITA/DESPESA,
 * nenhum dos dois representa corretamente a devolucao de um valor que a
 * residencia apenas adiantou para um terceiro — ver CRIATI-FIN-013A). O
 * impacto de caixa na {@link ContaFinanceira} e reconhecido diretamente por
 * {@link br.app.criati.financeiro.service.SaldoFinanceiroService}, que soma
 * os ressarcimentos ATIVOs desta conta ao saldo, sem passar por
 * receita/despesa. Exclusao fisica nao e permitida: o estorno preserva o
 * registro e marca status ESTORNADO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ressarcimento_parcela_cartao")
public class RessarcimentoParcelaCartao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "valor_a_receber_id", nullable = false, updatable = false)
	private ValorAReceberParcelaCartao valorAReceber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false, updatable = false)
	private ContaFinanceira conta;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valor;

	@Column(name = "data_ressarcimento", nullable = false, updatable = false)
	private LocalDate dataRessarcimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", length = 30, updatable = false)
	private FormaPagamentoLancamento formaPagamento;

	@Column(name = "observacao", length = 500, updatable = false)
	private String observacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusRessarcimentoParcelaCartao status;

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

	public RessarcimentoParcelaCartao(Empresa empresa, ValorAReceberParcelaCartao valorAReceber, ContaFinanceira conta,
			BigDecimal valor, LocalDate dataRessarcimento, FormaPagamentoLancamento formaPagamento, String observacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.valorAReceber = Objects.requireNonNull(valorAReceber, "valorAReceber nao pode ser nulo");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.dataRessarcimento = Objects.requireNonNull(dataRessarcimento, "dataRessarcimento nao pode ser nula");
		this.formaPagamento = formaPagamento;
		this.observacao = observacao;
		this.status = StatusRessarcimentoParcelaCartao.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}

	public void estornar(String motivo, Usuario autor) {
		if (this.status == StatusRessarcimentoParcelaCartao.ESTORNADO) {
			throw new DadosInvalidosException("Ressarcimento ja esta estornado");
		}
		this.status = StatusRessarcimentoParcelaCartao.ESTORNADO;
		this.estornadoEm = OffsetDateTime.now();
		this.estornadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.motivoEstorno = motivo;
	}

	public boolean estaAtivo() {
		return status == StatusRessarcimentoParcelaCartao.ATIVO;
	}
}
