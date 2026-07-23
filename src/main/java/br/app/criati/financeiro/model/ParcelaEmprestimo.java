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
import br.app.criati.shared.enums.SituacaoParcelaEmprestimo;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
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
 * Parcela concreta (numero, vencimento, valor) de um EmprestimoConcedido. Uma
 * parcela avulsa (pagamento unico) tambem e representada aqui, sempre com
 * numero=1/totalParcelas=1 — nao existe um segundo mecanismo para o caso
 * "sem parcelamento". Juros e multa sao persistidos como valores absolutos,
 * recalculados por EncargosEmprestimoService (nunca em controller) toda vez
 * que um recebimento e registrado, com base na data desse recebimento —
 * mesmo padrao de acrescimo explicito ja usado por OcorrenciaCompromisso.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "parcela_emprestimo", uniqueConstraints = @UniqueConstraint(
		name = "uk_parcela_emprestimo_numero", columnNames = {"emprestimo_id", "numero"}))
public class ParcelaEmprestimo {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "emprestimo_id", nullable = false, updatable = false)
	private EmprestimoConcedido emprestimo;

	@Column(name = "numero", nullable = false, updatable = false)
	private int numero;

	@Column(name = "total_parcelas", nullable = false, updatable = false)
	private int totalParcelas;

	@Column(name = "valor_principal", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valorPrincipal;

	@Column(name = "vencimento", nullable = false, updatable = false)
	private LocalDate vencimento;

	@Column(name = "data_prometida")
	private LocalDate dataPrometida;

	@Column(name = "data_efetiva_pagamento")
	private LocalDate dataEfetivaPagamento;

	@Column(name = "juros", nullable = false, precision = 19, scale = 2)
	private BigDecimal juros;

	@Column(name = "multa", nullable = false, precision = 19, scale = 2)
	private BigDecimal multa;

	@Column(name = "valor_recebido", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorRecebido;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusParcelaEmprestimo status;

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

	public ParcelaEmprestimo(Empresa empresa, EmprestimoConcedido emprestimo, int numero, int totalParcelas,
			BigDecimal valorPrincipal, LocalDate vencimento, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.emprestimo = Objects.requireNonNull(emprestimo, "emprestimo nao pode ser nulo");
		this.numero = numero;
		this.totalParcelas = totalParcelas;
		this.valorPrincipal = normalizarObrigatorio(valorPrincipal, "valorPrincipal");
		this.vencimento = Objects.requireNonNull(vencimento, "vencimento nao pode ser nulo");
		this.juros = zero();
		this.multa = zero();
		this.valorRecebido = zero();
		this.status = StatusParcelaEmprestimo.PENDENTE;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public BigDecimal getValorTotal() {
		return valorPrincipal.add(juros).add(multa).setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getSaldoPendente() {
		BigDecimal saldo = getValorTotal().subtract(valorRecebido);
		return saldo.signum() < 0 ? zero() : saldo;
	}

	public boolean estaAtrasada(LocalDate referencia) {
		Objects.requireNonNull(referencia, "referencia nao pode ser nula");
		return status != StatusParcelaEmprestimo.PAGO && status != StatusParcelaEmprestimo.CANCELADO
				&& getSaldoPendente().signum() > 0 && vencimento.isBefore(referencia);
	}

	public long diasEmAtraso(LocalDate referencia) {
		if (!estaAtrasada(referencia)) {
			return 0;
		}
		return ChronoUnit.DAYS.between(vencimento, referencia);
	}

	public SituacaoParcelaEmprestimo getSituacao(LocalDate referencia) {
		if (status == StatusParcelaEmprestimo.CANCELADO) {
			return SituacaoParcelaEmprestimo.CANCELADO;
		}
		if (status == StatusParcelaEmprestimo.PAGO) {
			return SituacaoParcelaEmprestimo.PAGO;
		}
		if (estaAtrasada(referencia)) {
			return SituacaoParcelaEmprestimo.ATRASADO;
		}
		return status == StatusParcelaEmprestimo.PARCIALMENTE_PAGO ? SituacaoParcelaEmprestimo.PARCIALMENTE_PAGO
				: SituacaoParcelaEmprestimo.PENDENTE;
	}

	/**
	 * Substitui os encargos vigentes pelos valores calculados por
	 * EncargosEmprestimoService para a data de referencia de um recebimento.
	 * Nunca acumula sobre chamadas anteriores (evita contar o mesmo periodo em
	 * dobro em recebimentos parciais sucessivos) — mesmo criterio de
	 * "substituir, nao somar" usado por OcorrenciaCompromisso.atualizarValores.
	 */
	public void aplicarEncargos(BigDecimal juros, BigDecimal multa, Usuario autor) {
		exigirNaoCancelada();
		BigDecimal jurosNormalizado = normalizar(juros);
		BigDecimal multaNormalizada = normalizar(multa);
		BigDecimal novoTotal = valorPrincipal.add(jurosNormalizado).add(multaNormalizada).setScale(2, RoundingMode.HALF_UP);
		if (novoTotal.compareTo(valorRecebido) < 0) {
			throw new DadosInvalidosException("Novo valor total nao pode ser menor que o valor ja recebido");
		}
		this.juros = jurosNormalizado;
		this.multa = multaNormalizada;
		registrarAlteracao(autor);
	}

	public void registrarRecebimento(BigDecimal valor, LocalDate dataRecebimento, Usuario autor) {
		exigirNaoCancelada();
		Objects.requireNonNull(dataRecebimento, "dataRecebimento nao pode ser nula");
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do recebimento deve ser maior que zero");
		}
		if (valor.compareTo(getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do recebimento nao pode exceder o saldo pendente");
		}
		this.valorRecebido = this.valorRecebido.add(valor).setScale(2, RoundingMode.HALF_UP);
		if (this.dataEfetivaPagamento == null || dataRecebimento.isAfter(this.dataEfetivaPagamento)) {
			this.dataEfetivaPagamento = dataRecebimento;
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
			this.dataEfetivaPagamento = null;
		}
		if (this.status == StatusParcelaEmprestimo.CANCELADO) {
			registrarAlteracao(autor);
			return;
		}
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void registrarDataPrometida(LocalDate dataPrometida, Usuario autor) {
		exigirNaoCancelada();
		this.dataPrometida = dataPrometida;
		registrarAlteracao(autor);
	}

	public void cancelar(Usuario autor) {
		if (this.status == StatusParcelaEmprestimo.PAGO) {
			throw new DadosInvalidosException("Parcela ja paga nao pode ser cancelada");
		}
		this.status = StatusParcelaEmprestimo.CANCELADO;
		registrarAlteracao(autor);
	}

	private void recalcularStatus() {
		if (this.status == StatusParcelaEmprestimo.CANCELADO) {
			return;
		}
		if (valorRecebido.signum() <= 0) {
			this.status = StatusParcelaEmprestimo.PENDENTE;
		} else if (getSaldoPendente().signum() <= 0) {
			this.status = StatusParcelaEmprestimo.PAGO;
		} else {
			this.status = StatusParcelaEmprestimo.PARCIALMENTE_PAGO;
		}
	}

	private void exigirNaoCancelada() {
		if (this.status == StatusParcelaEmprestimo.CANCELADO) {
			throw new DadosInvalidosException("Parcela cancelada nao pode ser alterada");
		}
	}

	private BigDecimal normalizar(BigDecimal valor) {
		BigDecimal efetivo = valor == null ? BigDecimal.ZERO : valor;
		if (efetivo.signum() < 0) {
			throw new DadosInvalidosException("Componentes monetarios nao podem ser negativos");
		}
		return efetivo.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizarObrigatorio(BigDecimal valor, String campo) {
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException(campo + " deve ser maior que zero");
		}
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal zero() {
		return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
