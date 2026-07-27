package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.shared.enums.StatusFaturaCartao;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "fatura_cartao", uniqueConstraints = @UniqueConstraint(
		name = "uq_fatura_cartao_empresa_principal_competencia",
		columnNames = {"empresa_id", "cartao_principal_id", "competencia"}))
public class FaturaCartao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cartao_principal_id", nullable = false, updatable = false)
	private CartaoCredito cartaoPrincipal;

	@Column(nullable = false, updatable = false)
	private LocalDate competencia;

	@Column(name = "periodo_inicial", nullable = false, updatable = false)
	private LocalDate periodoInicial;

	@Column(name = "periodo_final", nullable = false, updatable = false)
	private LocalDate periodoFinal;

	@Column(name = "data_fechamento", nullable = false, updatable = false)
	private LocalDate dataFechamento;

	@Column(name = "data_vencimento", nullable = false, updatable = false)
	private LocalDate dataVencimento;

	@Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorTotal;

	@Column(name = "saldo_financiado_anterior", nullable = false, precision = 19, scale = 2)
	private BigDecimal saldoFinanciadoAnterior;

	@Column(name = "juros", nullable = false, precision = 19, scale = 2)
	private BigDecimal juros;

	@Column(name = "multa", nullable = false, precision = 19, scale = 2)
	private BigDecimal multa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusFaturaCartao status;

	@Column(name = "fechado_em")
	private OffsetDateTime fechadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fechado_por_usuario_id")
	private Usuario fechadoPor;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false, updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "atualizado_por_usuario_id", nullable = false)
	private Usuario atualizadoPor;

	@Version
	@Column(nullable = false)
	private long versao;

	public FaturaCartao(Empresa empresa, CartaoCredito cartaoPrincipal, LocalDate competencia,
			LocalDate periodoInicial, LocalDate periodoFinal, LocalDate dataFechamento,
			LocalDate dataVencimento, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa);
		this.cartaoPrincipal = Objects.requireNonNull(cartaoPrincipal);
		this.competencia = Objects.requireNonNull(competencia);
		this.periodoInicial = Objects.requireNonNull(periodoInicial);
		this.periodoFinal = Objects.requireNonNull(periodoFinal);
		this.dataFechamento = Objects.requireNonNull(dataFechamento);
		this.dataVencimento = Objects.requireNonNull(dataVencimento);
		this.valorTotal = zero();
		this.saldoFinanciadoAnterior = zero();
		this.juros = zero();
		this.multa = zero();
		this.status = StatusFaturaCartao.ABERTA;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor);
		this.atualizadoPor = autor;
	}

	public void recompor(BigDecimal novoTotal, Usuario autor) {
		exigirAberta();
		if (novoTotal == null || novoTotal.signum() < 0) {
			throw new IllegalArgumentException("Total da fatura invalido");
		}
		this.valorTotal = novoTotal.setScale(2, RoundingMode.HALF_UP);
		alterar(autor);
	}

	public void fechar(Usuario autor) {
		exigirAberta();
		this.status = StatusFaturaCartao.FECHADA;
		this.fechadoEm = OffsetDateTime.now();
		this.fechadoPor = Objects.requireNonNull(autor);
		alterar(autor);
	}

	/**
	 * Congela, uma unica vez, o saldo devedor herdado da fatura anterior do
	 * mesmo cartao principal (LES-F3-005) - chamado apenas por
	 * FaturaCartaoService#abrir no momento da criacao, nunca em recompor().
	 * Nao recalculado depois: idempotencia vem do proprio abrir() ja ser
	 * idempotente (uma fatura so e criada uma vez por competencia).
	 */
	public void assumirSaldoFinanciado(BigDecimal saldo, Usuario autor) {
		exigirAberta();
		this.saldoFinanciadoAnterior = normalizarNaoNegativo(saldo, "saldoFinanciadoAnterior");
		alterar(autor);
	}

	/**
	 * Substitui os encargos vigentes (nunca acumula sobre chamadas anteriores,
	 * mesmo criterio de ParcelaEmprestimo#aplicarEncargos), aumentando
	 * explicitamente o valor devido da fatura. So permitido em fatura ja
	 * fechada e ainda nao quitada.
	 */
	public void aplicarEncargos(BigDecimal juros, BigDecimal multa, BigDecimal totalPagoAcumulado, Usuario autor) {
		exigirPagavel();
		this.juros = normalizarNaoNegativo(juros, "juros");
		this.multa = normalizarNaoNegativo(multa, "multa");
		recalcularStatus(Objects.requireNonNull(totalPagoAcumulado, "totalPagoAcumulado nao pode ser nulo"));
		alterar(autor);
	}

	/**
	 * Recalcula o status a partir do total ja pago (soma de todos os
	 * PagamentoFaturaCartao ativos desta fatura, calculada pelo service - a
	 * fatura nunca acumula o proprio valor pago como campo, para nao duplicar
	 * fonte de verdade). Pagamento parcial/minimo nunca altera parcelas
	 * individualmente: apenas este status e a fonte de verdade.
	 */
	public void registrarPagamento(BigDecimal totalPagoAcumulado, Usuario autor) {
		exigirPagavel();
		recalcularStatus(Objects.requireNonNull(totalPagoAcumulado, "totalPagoAcumulado nao pode ser nulo"));
		alterar(autor);
	}

	/**
	 * Valor devido total: soma das parcelas da competencia (valorTotal) mais o
	 * saldo financiado herdado do ciclo anterior e os encargos manuais
	 * aplicados - nunca persistido como coluna propria, sempre derivado.
	 */
	public BigDecimal getValorDevido() {
		return valorTotal.add(saldoFinanciadoAnterior).add(juros).add(multa).setScale(2, RoundingMode.HALF_UP);
	}

	public boolean estaAberta() {
		return status == StatusFaturaCartao.ABERTA;
	}

	private void recalcularStatus(BigDecimal totalPagoAcumulado) {
		BigDecimal saldoDevido = getValorDevido().subtract(totalPagoAcumulado).setScale(2, RoundingMode.HALF_UP);
		if (saldoDevido.signum() <= 0) {
			this.status = StatusFaturaCartao.PAGA;
		} else if (LocalDate.now().isAfter(dataVencimento)) {
			this.status = StatusFaturaCartao.ATRASADA;
		} else {
			this.status = StatusFaturaCartao.PARCIALMENTE_PAGA;
		}
	}

	private void exigirAberta() {
		if (!estaAberta()) {
			throw new FaturaCartaoStatusInvalidoException("Fatura fechada nao pode ser alterada");
		}
	}

	/**
	 * Fatura ABERTA nunca recebe pagamento (precisa fechar primeiro); fatura
	 * PAGA nao aceita novo pagamento/encargo (estorno, quando existir, e
	 * escopo de LES-F3-006). FECHADA, PARCIALMENTE_PAGA e ATRASADA sao os
	 * unicos estados pagaveis.
	 */
	private void exigirPagavel() {
		if (status == StatusFaturaCartao.ABERTA) {
			throw new FaturaCartaoStatusInvalidoException("Fatura aberta ainda nao pode receber pagamento");
		}
		if (status == StatusFaturaCartao.PAGA) {
			throw new FaturaCartaoStatusInvalidoException("Fatura ja esta paga");
		}
	}

	private void alterar(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor);
		this.atualizadoEm = OffsetDateTime.now();
	}

	private static BigDecimal zero() {
		return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal normalizarNaoNegativo(BigDecimal valor, String campo) {
		BigDecimal efetivo = valor == null ? BigDecimal.ZERO : valor;
		if (efetivo.signum() < 0) {
			throw new IllegalArgumentException(campo + " nao pode ser negativo");
		}
		return efetivo.setScale(2, RoundingMode.HALF_UP);
	}
}
