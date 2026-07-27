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

	public boolean estaAberta() {
		return status == StatusFaturaCartao.ABERTA;
	}

	private void exigirAberta() {
		if (!estaAberta()) {
			throw new FaturaCartaoStatusInvalidoException("Fatura fechada nao pode ser alterada");
		}
	}

	private void alterar(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor);
		this.atualizadoEm = OffsetDateTime.now();
	}

	private static BigDecimal zero() {
		return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	}
}
