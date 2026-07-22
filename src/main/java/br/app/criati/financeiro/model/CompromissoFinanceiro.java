package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.TipoValorCompromisso;
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
 * Regra ou origem de uma obrigacao a pagar (condominio, energia, mensalidade,
 * manutencao avulsa etc). Nunca movimenta saldo por si so: cada competencia
 * concreta e representada por uma {@link OcorrenciaCompromisso}. Quando
 * recorrente, reaproveita a {@link RecorrenciaFinanceira} ja existente (mesmo
 * motor de calculo de competencia/vencimento do modulo) em vez de introduzir um
 * segundo mecanismo de repeticao. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "compromisso_financeiro", uniqueConstraints = @UniqueConstraint(
		name = "uq_compromisso_financeiro_recorrencia", columnNames = "recorrencia_id"))
public class CompromissoFinanceiro {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

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
	@JoinColumn(name = "conta_padrao_id")
	private ContaFinanceira contaPadrao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recorrencia_id")
	private RecorrenciaFinanceira recorrencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_valor", nullable = false, length = 20)
	private TipoValorCompromisso tipoValor;

	@Column(name = "valor_padrao", precision = 19, scale = 2)
	private BigDecimal valorPadrao;

	@Column(name = "dia_vencimento_padrao")
	private Integer diaVencimentoPadrao;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento_padrao", length = 30)
	private FormaPagamentoLancamento formaPagamentoPadrao;

	@Column(name = "ativo", nullable = false)
	private boolean ativo;

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

	public CompromissoFinanceiro(Empresa empresa, String descricao, CategoriaFinanceira categoria,
			PessoaFinanceira pessoaFinanceira, ParteFinanceira parteFinanceira, ContaFinanceira contaPadrao,
			RecorrenciaFinanceira recorrencia, TipoValorCompromisso tipoValor, BigDecimal valorPadrao,
			Integer diaVencimentoPadrao, FormaPagamentoLancamento formaPagamentoPadrao, String observacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.contaPadrao = contaPadrao;
		this.recorrencia = recorrencia;
		this.tipoValor = Objects.requireNonNull(tipoValor, "tipoValor nao pode ser nulo");
		this.valorPadrao = valorPadrao;
		this.diaVencimentoPadrao = diaVencimentoPadrao;
		this.formaPagamentoPadrao = formaPagamentoPadrao;
		this.ativo = true;
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarDados(String descricao, CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira,
			ParteFinanceira parteFinanceira, ContaFinanceira contaPadrao, TipoValorCompromisso tipoValor,
			BigDecimal valorPadrao, Integer diaVencimentoPadrao, FormaPagamentoLancamento formaPagamentoPadrao,
			String observacao, Usuario autor) {
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.contaPadrao = contaPadrao;
		this.tipoValor = Objects.requireNonNull(tipoValor, "tipoValor nao pode ser nulo");
		this.valorPadrao = valorPadrao;
		this.diaVencimentoPadrao = diaVencimentoPadrao;
		this.formaPagamentoPadrao = formaPagamentoPadrao;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public void ativar(Usuario autor) {
		this.ativo = true;
		registrarAlteracao(autor);
	}

	public void desativar(Usuario autor) {
		this.ativo = false;
		registrarAlteracao(autor);
	}

	public boolean ehRecorrente() {
		return recorrencia != null;
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
