package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.SituacaoValorAReceberCompraCartao;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
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
 * Rastreio do valor a receber de uma pessoa (ParteFinanceira) por uma parcela
 * de {@link CompraCartao} feita nos cartoes da residencia para ela — uma
 * relacao 1:1 com {@link ParcelaCompraCartao}, nunca representando de novo
 * numero/valor/vencimento da parcela (essa continua sendo a unica fonte de
 * verdade para esses dados; aqui so se acrescenta o que falta: quanto ja foi
 * ressarcido, data prometida e situacao do ressarcimento). Uma
 * ParcelaCompraCartao so ganha um registro aqui quando a compra de origem foi
 * registrada via CompraTerceiroService — a mera presenca deste registro e o
 * que define, no dominio, "esta parcela pertence a uma compra para terceiro"
 * (nunca um filtro implicito em CompraCartao.parteFinanceira, que continua
 * opcional e sem esse significado reforcado). O campo {@code vencimento}
 * abaixo e uma copia deliberada e imutavel de
 * {@link ParcelaCompraCartao#getCompetencia()} no momento da criacao, apenas
 * para permitir indice proprio por vencimento sem depender de join — nunca
 * atualizado depois, sem risco de divergencia (a competencia da parcela de
 * cartao nunca muda). Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-013.md.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "valor_a_receber_parcela_cartao", uniqueConstraints = @UniqueConstraint(
		name = "uq_valor_a_receber_parcela_cartao_parcela", columnNames = "parcela_id"))
public class ValorAReceberParcelaCartao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parcela_id", nullable = false, updatable = false)
	private ParcelaCompraCartao parcela;

	@Column(name = "vencimento", nullable = false, updatable = false)
	private LocalDate vencimento;

	@Column(name = "valor_recebido", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorRecebido;

	@Column(name = "data_prometida")
	private LocalDate dataPrometida;

	@Column(name = "data_efetiva_recebimento")
	private LocalDate dataEfetivaRecebimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusValorAReceberCompraCartao status;

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

	public ValorAReceberParcelaCartao(Empresa empresa, ParcelaCompraCartao parcela, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.parcela = Objects.requireNonNull(parcela, "parcela nao pode ser nula");
		this.vencimento = Objects.requireNonNull(parcela.getCompetencia(), "parcela sem competencia");
		this.valorRecebido = zero();
		this.status = StatusValorAReceberCompraCartao.PENDENTE;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public BigDecimal getValorTotal() {
		return parcela.getValor().setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getSaldoPendente() {
		BigDecimal saldo = getValorTotal().subtract(valorRecebido);
		return saldo.signum() < 0 ? zero() : saldo;
	}

	public boolean estaAtrasada(LocalDate referencia) {
		Objects.requireNonNull(referencia, "referencia nao pode ser nula");
		return status != StatusValorAReceberCompraCartao.RESSARCIDA && status != StatusValorAReceberCompraCartao.CANCELADA
				&& getSaldoPendente().signum() > 0 && vencimento.isBefore(referencia);
	}

	public long diasEmAtraso(LocalDate referencia) {
		if (!estaAtrasada(referencia)) {
			return 0;
		}
		return ChronoUnit.DAYS.between(vencimento, referencia);
	}

	public SituacaoValorAReceberCompraCartao getSituacao(LocalDate referencia) {
		if (status == StatusValorAReceberCompraCartao.CANCELADA) {
			return SituacaoValorAReceberCompraCartao.CANCELADA;
		}
		if (status == StatusValorAReceberCompraCartao.RESSARCIDA) {
			return SituacaoValorAReceberCompraCartao.RESSARCIDA;
		}
		if (estaAtrasada(referencia)) {
			return SituacaoValorAReceberCompraCartao.ATRASADA;
		}
		return status == StatusValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA
				? SituacaoValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA
				: SituacaoValorAReceberCompraCartao.PENDENTE;
	}

	public void registrarRecebimento(BigDecimal valor, LocalDate dataRessarcimento, Usuario autor) {
		exigirNaoCancelado();
		Objects.requireNonNull(dataRessarcimento, "dataRessarcimento nao pode ser nula");
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do ressarcimento deve ser maior que zero");
		}
		if (valor.compareTo(getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do ressarcimento nao pode exceder o saldo pendente");
		}
		this.valorRecebido = this.valorRecebido.add(valor).setScale(2, RoundingMode.HALF_UP);
		if (this.dataEfetivaRecebimento == null || dataRessarcimento.isAfter(this.dataEfetivaRecebimento)) {
			this.dataEfetivaRecebimento = dataRessarcimento;
		}
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void estornarRecebimento(BigDecimal valor, Usuario autor) {
		Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.valorRecebido = this.valorRecebido.subtract(valor).setScale(2, RoundingMode.HALF_UP);
		if (this.valorRecebido.signum() < 0) {
			this.valorRecebido = zero();
		}
		if (this.valorRecebido.signum() == 0) {
			this.dataEfetivaRecebimento = null;
		}
		if (this.status == StatusValorAReceberCompraCartao.CANCELADA) {
			registrarAlteracao(autor);
			return;
		}
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void registrarDataPrometida(LocalDate dataPrometida, Usuario autor) {
		exigirNaoCancelado();
		this.dataPrometida = dataPrometida;
		registrarAlteracao(autor);
	}

	public void cancelar(Usuario autor) {
		if (this.status == StatusValorAReceberCompraCartao.RESSARCIDA) {
			throw new DadosInvalidosException("Valor a receber ja ressarcido nao pode ser cancelado");
		}
		this.status = StatusValorAReceberCompraCartao.CANCELADA;
		registrarAlteracao(autor);
	}

	private void recalcularStatus() {
		if (this.status == StatusValorAReceberCompraCartao.CANCELADA) {
			return;
		}
		if (valorRecebido.signum() <= 0) {
			this.status = StatusValorAReceberCompraCartao.PENDENTE;
		} else if (getSaldoPendente().signum() <= 0) {
			this.status = StatusValorAReceberCompraCartao.RESSARCIDA;
		} else {
			this.status = StatusValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA;
		}
	}

	private void exigirNaoCancelado() {
		if (this.status == StatusValorAReceberCompraCartao.CANCELADA) {
			throw new DadosInvalidosException("Valor a receber cancelado nao pode ser alterado");
		}
	}

	private BigDecimal zero() {
		return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
