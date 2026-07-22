package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoFinanceiro;
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
 * Regra de geracao de lancamentos financeiros recorrentes (salario, condominio,
 * assinaturas etc). A recorrencia nunca movimenta saldo por si so: cada
 * competencia elegivel gera um LancamentoFinanceiro (origem RECORRENCIA) que
 * segue o ciclo normal de liquidacao. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-F2-006.md para a estrategia de
 * calculo de competencia/vencimento e tratamento de meses menores.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "recorrencia_financeira")
public class RecorrenciaFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoFinanceiro tipo;

	@Column(name = "descricao", nullable = false, length = 200)
	private String descricao;

	@Column(name = "valor_padrao", nullable = false, precision = 19, scale = 2)
	private BigDecimal valorPadrao;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false)
	private ContaFinanceira conta;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private CategoriaFinanceira categoria;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pessoa_financeira_id", nullable = false)
	private PessoaFinanceira pessoaFinanceira;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parte_financeira_id")
	private ParteFinanceira parteFinanceira;

	@Enumerated(EnumType.STRING)
	@Column(name = "forma_pagamento", length = 30)
	private FormaPagamentoLancamento formaPagamento;

	@Enumerated(EnumType.STRING)
	@Column(name = "periodicidade", nullable = false, length = 20)
	private PeriodicidadeRecorrencia periodicidade;

	@Column(name = "intervalo", nullable = false)
	private int intervalo;

	@Column(name = "dia_referencia", nullable = false)
	private int diaReferencia;

	@Column(name = "mes_referencia")
	private Integer mesReferencia;

	@Column(name = "data_inicial", nullable = false)
	private LocalDate dataInicial;

	@Column(name = "data_final")
	private LocalDate dataFinal;

	@Column(name = "proxima_competencia", nullable = false)
	private LocalDate proximaCompetencia;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusRecorrencia status;

	@Column(name = "gerar_automaticamente", nullable = false)
	private boolean gerarAutomaticamente;

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

	@Column(name = "pausada_em")
	private OffsetDateTime pausadaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pausada_por_usuario_id")
	private Usuario pausadaPor;

	@Column(name = "encerrada_em")
	private OffsetDateTime encerradaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "encerrada_por_usuario_id")
	private Usuario encerradaPor;

	public RecorrenciaFinanceira(Empresa empresa, TipoFinanceiro tipo, String descricao, BigDecimal valorPadrao,
			ContaFinanceira conta, CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira,
			ParteFinanceira parteFinanceira, FormaPagamentoLancamento formaPagamento,
			PeriodicidadeRecorrencia periodicidade, int intervalo, int diaReferencia, Integer mesReferencia,
			LocalDate dataInicial, LocalDate dataFinal, boolean gerarAutomaticamente, String observacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valorPadrao = Objects.requireNonNull(valorPadrao, "valorPadrao nao pode ser nulo");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.formaPagamento = formaPagamento;
		this.periodicidade = Objects.requireNonNull(periodicidade, "periodicidade nao pode ser nula");
		this.intervalo = intervalo;
		this.diaReferencia = diaReferencia;
		this.mesReferencia = mesReferencia;
		this.dataInicial = Objects.requireNonNull(dataInicial, "dataInicial nao pode ser nula");
		this.dataFinal = dataFinal;
		this.proximaCompetencia = competenciaInicial().atDay(1);
		this.status = StatusRecorrencia.ATIVA;
		this.gerarAutomaticamente = gerarAutomaticamente;
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarSerie(String descricao, BigDecimal valorPadrao, ContaFinanceira conta,
			CategoriaFinanceira categoria, PessoaFinanceira pessoaFinanceira, ParteFinanceira parteFinanceira,
			FormaPagamentoLancamento formaPagamento, int intervalo, int diaReferencia, Integer mesReferencia,
			LocalDate dataFinal, boolean gerarAutomaticamente, String observacao, Usuario autor) {
		this.descricao = Objects.requireNonNull(descricao, "descricao nao pode ser nula");
		this.valorPadrao = Objects.requireNonNull(valorPadrao, "valorPadrao nao pode ser nulo");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.categoria = Objects.requireNonNull(categoria, "categoria nao pode ser nula");
		this.pessoaFinanceira = Objects.requireNonNull(pessoaFinanceira, "pessoaFinanceira nao pode ser nula");
		this.parteFinanceira = parteFinanceira;
		this.formaPagamento = formaPagamento;
		this.intervalo = intervalo;
		this.diaReferencia = diaReferencia;
		this.mesReferencia = mesReferencia;
		this.dataFinal = dataFinal;
		this.gerarAutomaticamente = gerarAutomaticamente;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public YearMonth competenciaInicial() {
		YearMonth base = YearMonth.from(dataInicial);
		if (periodicidade != PeriodicidadeRecorrencia.ANUAL) {
			return base;
		}
		return base.getMonthValue() <= mesReferencia
				? YearMonth.of(base.getYear(), mesReferencia)
				: YearMonth.of(base.getYear() + 1, mesReferencia);
	}

	/** Aplica a regra de "ultimo dia do mes" quando o dia configurado excede o mes da competencia (ex.: dia 31 em fevereiro). */
	public LocalDate calcularVencimento(YearMonth competencia) {
		Objects.requireNonNull(competencia, "competencia nao pode ser nula");
		int dia = Math.min(diaReferencia, competencia.lengthOfMonth());
		return competencia.atDay(dia);
	}

	public YearMonth calcularProximaCompetencia(YearMonth atual) {
		Objects.requireNonNull(atual, "competencia nao pode ser nula");
		return periodicidade == PeriodicidadeRecorrencia.ANUAL ? atual.plusYears(intervalo) : atual.plusMonths(intervalo);
	}

	public boolean dentroDoPeriodo(YearMonth competencia) {
		if (competencia.isBefore(competenciaInicial())) {
			return false;
		}
		return dataFinal == null || !competencia.isAfter(YearMonth.from(dataFinal));
	}

	public YearMonth getProximaCompetencia() {
		return YearMonth.from(proximaCompetencia);
	}

	public void avancarProximaCompetencia() {
		this.proximaCompetencia = calcularProximaCompetencia(getProximaCompetencia()).atDay(1);
	}

	public boolean estaAtiva() {
		return status == StatusRecorrencia.ATIVA;
	}

	public boolean elegivelParaGeracaoAutomatica(YearMonth competenciaAtual) {
		return estaAtiva() && gerarAutomaticamente && !getProximaCompetencia().isAfter(competenciaAtual)
				&& dentroDoPeriodo(getProximaCompetencia());
	}

	public void pausar(Usuario autor) {
		this.status = StatusRecorrencia.PAUSADA;
		this.pausadaEm = OffsetDateTime.now();
		this.pausadaPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		registrarAlteracao(autor);
	}

	public void retomar(YearMonth competenciaAtual, Usuario autor) {
		this.status = StatusRecorrencia.ATIVA;
		if (getProximaCompetencia().isBefore(competenciaAtual)) {
			this.proximaCompetencia = competenciaAtual.atDay(1);
		}
		registrarAlteracao(autor);
	}

	public void encerrar(Usuario autor) {
		this.status = StatusRecorrencia.ENCERRADA;
		this.encerradaEm = OffsetDateTime.now();
		this.encerradaPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		registrarAlteracao(autor);
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
