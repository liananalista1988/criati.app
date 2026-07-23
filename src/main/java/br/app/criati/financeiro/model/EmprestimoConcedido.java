package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Dinheiro emprestado pela residencia a uma pessoa ja cadastrada como
 * ParteFinanceira (amigo, familiar ou outra pessoa externa a empresa/residencia
 * — o mesmo cadastro ja usado como credor em CompromissoFinanceiro, nunca uma
 * estrutura de pessoas paralela). Define a regra de cobranca do emprestimo
 * inteiro (juros/multa, ver EncargosEmprestimoService) e a forma de pagamento
 * (unico ou parcelado); cada parcela concreta e uma {@link ParcelaEmprestimo}.
 * Nao gera LancamentoFinanceiro na concessao (o valor emprestado nao e
 * consumo/despesa, mas tambem nao ha ainda uma natureza de lancamento neutra
 * de transferencia no modulo) — apenas os recebimentos das parcelas movimentam
 * o fluxo financeiro existente, via {@link RecebimentoParcelaEmprestimo}.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "emprestimo_concedido")
public class EmprestimoConcedido {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "parte_financeira_id", nullable = false, updatable = false)
	private ParteFinanceira parteFinanceira;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaFinanceira categoria;

	@Column(name = "descricao", length = 500)
	private String descricao;

	@Column(name = "valor_principal", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valorPrincipal;

	@Column(name = "data_concessao", nullable = false, updatable = false)
	private LocalDate dataConcessao;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_cobranca", nullable = false, length = 30, updatable = false)
	private TipoCobrancaEmprestimo tipoCobranca;

	@Column(name = "percentual_juros", precision = 7, scale = 4, updatable = false)
	private BigDecimal percentualJuros;

	@Column(name = "percentual_multa", precision = 7, scale = 4, updatable = false)
	private BigDecimal percentualMulta;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", nullable = false, length = 20, updatable = false)
	private FormaPagamentoEmprestimo formaPagamento;

	@Column(name = "quantidade_parcelas", nullable = false, updatable = false)
	private int quantidadeParcelas;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusEmprestimoConcedido status;

	@Column(name = "motivo_cancelamento", length = 500)
	private String motivoCancelamento;

	@Column(name = "cancelado_em")
	private OffsetDateTime canceladoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cancelado_por_usuario_id")
	private Usuario canceladoPor;

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

	@OneToMany(mappedBy = "emprestimo", cascade = CascadeType.PERSIST, orphanRemoval = false)
	@OrderBy("numero")
	private List<ParcelaEmprestimo> parcelas = new ArrayList<>();

	public EmprestimoConcedido(Empresa empresa, ParteFinanceira parteFinanceira, CategoriaFinanceira categoria,
			String descricao, BigDecimal valorPrincipal, LocalDate dataConcessao, TipoCobrancaEmprestimo tipoCobranca,
			BigDecimal percentualJuros, BigDecimal percentualMulta, FormaPagamentoEmprestimo formaPagamento,
			int quantidadeParcelas, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.parteFinanceira = Objects.requireNonNull(parteFinanceira, "parteFinanceira nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.descricao = descricao;
		this.valorPrincipal = normalizarValorPrincipal(valorPrincipal);
		this.dataConcessao = Objects.requireNonNull(dataConcessao, "dataConcessao nao pode ser nula");
		this.tipoCobranca = Objects.requireNonNull(tipoCobranca, "tipoCobranca nao pode ser nulo");
		this.percentualJuros = validarPercentualJuros(tipoCobranca, percentualJuros);
		this.percentualMulta = validarPercentualMulta(tipoCobranca, percentualMulta);
		this.formaPagamento = Objects.requireNonNull(formaPagamento, "formaPagamento nao pode ser nulo");
		this.quantidadeParcelas = validarQuantidadeParcelas(formaPagamento, quantidadeParcelas);
		this.status = StatusEmprestimoConcedido.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void adicionarParcela(ParcelaEmprestimo parcela) {
		parcelas.add(parcela);
	}

	public boolean estaAtivo() {
		return status == StatusEmprestimoConcedido.ATIVO;
	}

	public void cancelar(String motivo, Usuario autor) {
		if (status == StatusEmprestimoConcedido.CANCELADO) {
			throw new DadosInvalidosException("Emprestimo ja esta cancelado");
		}
		this.status = StatusEmprestimoConcedido.CANCELADO;
		this.motivoCancelamento = motivo;
		this.canceladoEm = OffsetDateTime.now();
		this.canceladoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		registrarAlteracao(autor);
	}

	public void marcarQuitado(Usuario autor) {
		if (status != StatusEmprestimoConcedido.CANCELADO) {
			this.status = StatusEmprestimoConcedido.QUITADO;
			registrarAlteracao(autor);
		}
	}

	public void reabrir(Usuario autor) {
		if (status == StatusEmprestimoConcedido.QUITADO) {
			this.status = StatusEmprestimoConcedido.ATIVO;
			registrarAlteracao(autor);
		}
	}

	private BigDecimal normalizarValorPrincipal(BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor principal deve ser maior que zero");
		}
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal validarPercentualJuros(TipoCobrancaEmprestimo tipo, BigDecimal percentual) {
		boolean exigeJuros = tipo == TipoCobrancaEmprestimo.COM_JUROS || tipo == TipoCobrancaEmprestimo.JUROS_MORA_ATRASO;
		if (exigeJuros) {
			if (percentual == null || percentual.signum() <= 0) {
				throw new DadosInvalidosException("Percentual de juros e obrigatorio para a cobranca configurada");
			}
			return percentual.setScale(4, RoundingMode.HALF_UP);
		}
		if (percentual != null) {
			throw new DadosInvalidosException("Percentual de juros nao se aplica a cobranca configurada");
		}
		return null;
	}

	private BigDecimal validarPercentualMulta(TipoCobrancaEmprestimo tipo, BigDecimal percentual) {
		if (tipo == TipoCobrancaEmprestimo.MULTA_ATRASO) {
			if (percentual == null || percentual.signum() <= 0) {
				throw new DadosInvalidosException("Percentual de multa e obrigatorio para a cobranca configurada");
			}
			return percentual.setScale(4, RoundingMode.HALF_UP);
		}
		if (percentual != null) {
			throw new DadosInvalidosException("Percentual de multa nao se aplica a cobranca configurada");
		}
		return null;
	}

	private int validarQuantidadeParcelas(FormaPagamentoEmprestimo forma, int quantidade) {
		if (quantidade < 1) {
			throw new DadosInvalidosException("Quantidade de parcelas deve ser ao menos uma");
		}
		if (forma == FormaPagamentoEmprestimo.UNICO && quantidade != 1) {
			throw new DadosInvalidosException("Pagamento unico exige exatamente uma parcela");
		}
		return quantidade;
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
