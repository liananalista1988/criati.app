package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;
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
 * Obrigacao concreta de uma competencia especifica (ex.: energia de 08/2026).
 * Distinta do {@link CompromissoFinanceiro} (a regra) e do pagamento
 * ({@link PagamentoOcorrenciaCompromisso}, que registra a liquidacao integral
 * ou parcial). Uma ocorrencia avulsa pode existir sem compromisso permanente.
 * Quando gerada por uma {@link RecorrenciaFinanceira}, a identidade
 * (recorrencia_id + competencia) segue o mesmo padrao de idempotencia ja usado
 * por LancamentoFinanceiro (ver IMPLEMENTACAO-F2-006.md), sem duplicar o motor
 * de calculo de competencia/vencimento.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "ocorrencia_compromisso", uniqueConstraints = {
		@UniqueConstraint(name = "uq_ocorrencia_compromisso_recorrencia_competencia",
				columnNames = {"recorrencia_id", "competencia"}),
		@UniqueConstraint(name = "uq_ocorrencia_compromisso_compromisso_competencia",
				columnNames = {"compromisso_id", "competencia"})
})
public class OcorrenciaCompromisso {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "compromisso_id")
	private CompromissoFinanceiro compromisso;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorrencia_id")
	private RecorrenciaFinanceira recorrencia;

	@Column(name = "competencia", nullable = false)
	private LocalDate competencia;

	@Column(name = "descricao", nullable = false, length = 200)
	private String descricao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaFinanceira categoria;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pessoa_financeira_id", nullable = false)
	private PessoaFinanceira pessoaFinanceira;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parte_financeira_id")
	private ParteFinanceira parteFinanceira;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_prevista_id")
	private ContaFinanceira contaPrevista;

	@Column(name = "valor_previsto", precision = 19, scale = 2)
	private BigDecimal valorPrevisto;

	@Column(name = "valor_principal", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorPrincipal;

	@Column(name = "vencimento", nullable = false)
	private LocalDate vencimento;

	@Column(name = "data_recebimento_cobranca")
	private LocalDate dataRecebimentoCobranca;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusOcorrenciaCompromisso status;

	@Column(name = "juros", nullable = false, precision = 19, scale = 2)
	private BigDecimal juros;

	@Column(name = "multa", nullable = false, precision = 19, scale = 2)
	private BigDecimal multa;

	@Column(name = "desconto", nullable = false, precision = 19, scale = 2)
	private BigDecimal desconto;

	@Column(name = "valor_pago", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorPago;

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

	public OcorrenciaCompromisso(Empresa empresa, CompromissoFinanceiro compromisso, RecorrenciaFinanceira recorrencia,
			LocalDate competencia, String descricao, CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira,
			ParteFinanceira parteFinanceira, ContaFinanceira contaPrevista, BigDecimal valorPrevisto,
			BigDecimal valorPrincipal, LocalDate vencimento, LocalDate dataRecebimentoCobranca, BigDecimal juros,
			BigDecimal multa, BigDecimal desconto, String observacao, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.compromisso = compromisso;
		this.recorrencia = recorrencia;
		this.competencia = Objects.requireNonNull(competencia, "competencia nao pode ser nula");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.contaPrevista = contaPrevista;
		this.valorPrevisto = valorPrevisto;
		this.vencimento = Objects.requireNonNull(vencimento, "vencimento nao pode ser nulo");
		this.dataRecebimentoCobranca = dataRecebimentoCobranca;
		this.juros = normalizar(juros);
		this.multa = normalizar(multa);
		this.desconto = normalizar(desconto);
		this.valorPago = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		definirValorPrincipal(valorPrincipal);
		this.status = StatusOcorrenciaCompromisso.PENDENTE;
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public YearMonth getCompetencia() {
		return YearMonth.from(competencia);
	}

	public BigDecimal getValorTotal() {
		return valorPrincipal.add(juros).add(multa).subtract(desconto).setScale(2, RoundingMode.HALF_UP);
	}

	public BigDecimal getSaldoPendente() {
		BigDecimal saldo = getValorTotal().subtract(valorPago);
		return saldo.signum() < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : saldo;
	}

	public boolean estaVencida(LocalDate hoje) {
		Objects.requireNonNull(hoje, "hoje nao pode ser nulo");
		return status != StatusOcorrenciaCompromisso.CANCELADA && getSaldoPendente().signum() > 0
				&& vencimento.isBefore(hoje);
	}

	public long diasEmAtraso(LocalDate hoje) {
		if (!estaVencida(hoje)) {
			return 0;
		}
		return ChronoUnit.DAYS.between(vencimento, hoje);
	}

	public void atualizarValores(BigDecimal valorPrevisto, BigDecimal valorPrincipal, BigDecimal juros,
			BigDecimal multa, BigDecimal desconto, Usuario autor) {
		exigirNaoCancelada();
		BigDecimal jurosNormalizado = normalizar(juros);
		BigDecimal multaNormalizada = normalizar(multa);
		BigDecimal descontoNormalizado = normalizar(desconto);
		BigDecimal principalNormalizado = normalizarObrigatorio(valorPrincipal, "valorPrincipal");
		BigDecimal novoTotal = principalNormalizado.add(jurosNormalizado).add(multaNormalizada)
				.subtract(descontoNormalizado).setScale(2, RoundingMode.HALF_UP);
		if (descontoNormalizado.compareTo(principalNormalizado.add(jurosNormalizado).add(multaNormalizada)) > 0) {
			throw new DadosInvalidosException("Desconto nao pode superar principal, juros e multa somados");
		}
		if (novoTotal.signum() < 0) {
			throw new DadosInvalidosException("Valor total nao pode ficar negativo");
		}
		if (novoTotal.compareTo(valorPago) < 0) {
			throw new DadosInvalidosException("Novo valor total nao pode ser menor que o valor ja pago");
		}
		this.valorPrevisto = valorPrevisto;
		this.valorPrincipal = principalNormalizado;
		this.juros = jurosNormalizado;
		this.multa = multaNormalizada;
		this.desconto = descontoNormalizado;
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void atualizarDadosGerais(String descricao, CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira,
			ParteFinanceira parteFinanceira, ContaFinanceira contaPrevista, String observacao, Usuario autor) {
		exigirNaoCancelada();
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.contaPrevista = contaPrevista;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public void atualizarVencimento(LocalDate vencimento, Usuario autor) {
		exigirNaoCancelada();
		this.vencimento = Objects.requireNonNull(vencimento, "vencimento nao pode ser nulo");
		registrarAlteracao(autor);
	}

	public void registrarDataRecebimentoCobranca(LocalDate data, Usuario autor) {
		this.dataRecebimentoCobranca = data;
		registrarAlteracao(autor);
	}

	public void registrarPagamento(BigDecimal valor, Usuario autor) {
		exigirNaoCancelada();
		Objects.requireNonNull(valor, "valor nao pode ser nulo");
		if (valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do pagamento deve ser maior que zero");
		}
		if (valor.compareTo(getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do pagamento nao pode exceder o saldo pendente");
		}
		this.valorPago = this.valorPago.add(valor).setScale(2, RoundingMode.HALF_UP);
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void estornarPagamento(BigDecimal valor, Usuario autor) {
		Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.valorPago = this.valorPago.subtract(valor).setScale(2, RoundingMode.HALF_UP);
		if (this.valorPago.signum() < 0) {
			this.valorPago = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (this.status == StatusOcorrenciaCompromisso.CANCELADA) {
			registrarAlteracao(autor);
			return;
		}
		recalcularStatus();
		registrarAlteracao(autor);
	}

	public void cancelar(Usuario autor) {
		this.status = StatusOcorrenciaCompromisso.CANCELADA;
		registrarAlteracao(autor);
	}

	private void recalcularStatus() {
		if (this.status == StatusOcorrenciaCompromisso.CANCELADA) {
			return;
		}
		if (valorPago.signum() <= 0) {
			this.status = StatusOcorrenciaCompromisso.PENDENTE;
		} else if (getSaldoPendente().signum() <= 0) {
			this.status = StatusOcorrenciaCompromisso.PAGA;
		} else {
			this.status = StatusOcorrenciaCompromisso.PARCIALMENTE_PAGA;
		}
	}

	private void definirValorPrincipal(BigDecimal valorPrincipal) {
		BigDecimal normalizado = normalizarObrigatorio(valorPrincipal, "valorPrincipal");
		if (desconto.compareTo(normalizado.add(juros).add(multa)) > 0) {
			throw new DadosInvalidosException("Desconto nao pode superar principal, juros e multa somados");
		}
		this.valorPrincipal = normalizado;
	}

	private void exigirNaoCancelada() {
		if (this.status == StatusOcorrenciaCompromisso.CANCELADA) {
			throw new DadosInvalidosException("Ocorrencia cancelada nao pode ser alterada");
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

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
