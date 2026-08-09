package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "conta_financeira")
public class ContaFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pessoa_titular_id")
	private PessoaFinanceira titular;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "instituicao_id")
	private InstituicaoFinanceira instituicao;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 30)
	private TipoContaFinanceira tipo;

	@Column(name = "saldo_inicial", nullable = false, precision = 19, scale = 2)
	private BigDecimal saldoInicial;

	@Column(name = "moeda", nullable = false, length = 3)
	private String moeda;

	@Column(name = "data_saldo_inicial", nullable = false)
	private LocalDate dataSaldoInicial;

	@Column(name = "permite_conciliacao", nullable = false)
	private boolean permiteConciliacao;

	@Column(name = "agencia_bancaria", length = 20)
	private String agenciaBancaria;

	@Column(name = "numero_conta_bancaria", length = 30)
	private String numeroContaBancaria;

	@Column(name = "digito_conta_bancaria", length = 5)
	private String digitoContaBancaria;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "criado_por_usuario_id", updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id")
	private Usuario atualizadoPor;

	public ContaFinanceira(
			Empresa empresa, String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial, StatusCadastro status) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.saldoInicial = Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
		this.moeda = "BRL";
		this.dataSaldoInicial = LocalDate.now();
		this.permiteConciliacao = false;
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	public ContaFinanceira(
			Empresa empresa,
			PessoaFinanceira titular,
			InstituicaoFinanceira instituicao,
			String nome,
			TipoContaFinanceira tipo,
			BigDecimal saldoInicial,
			LocalDate dataSaldoInicial,
			boolean permiteConciliacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.titular = Objects.requireNonNull(titular, "titular nao pode ser nulo");
		this.instituicao = instituicao;
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.moeda = "BRL";
		this.saldoInicial = Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
		this.dataSaldoInicial = Objects.requireNonNull(dataSaldoInicial, "dataSaldoInicial nao pode ser nula");
		this.permiteConciliacao = permiteConciliacao;
		this.status = StatusCadastro.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarDados(String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.saldoInicial = Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
	}

	public void atualizarDados(
			PessoaFinanceira titular,
			InstituicaoFinanceira instituicao,
			String nome,
			TipoContaFinanceira tipo,
			BigDecimal saldoInicial,
			LocalDate dataSaldoInicial,
			boolean permiteConciliacao,
			Usuario autor) {
		this.titular = Objects.requireNonNull(titular, "titular nao pode ser nulo");
		this.instituicao = instituicao;
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.moeda = "BRL";
		this.saldoInicial = Objects.requireNonNull(saldoInicial, "saldoInicial nao pode ser nulo");
		this.dataSaldoInicial = Objects.requireNonNull(dataSaldoInicial, "dataSaldoInicial nao pode ser nula");
		this.permiteConciliacao = permiteConciliacao;
		registrarAlteracao(autor);
	}

	/**
	 * Identificadores bancarios sao sempre opcionais e nao dependem de haver
	 * lancamento na conta (nao sao dado financeiro, apenas metadado usado
	 * pela sugestao automatica de conta na importacao bancaria - CRIATI-IMP-FEAT-004).
	 */
	public void atualizarIdentificacaoBancaria(
			String agenciaBancaria, String numeroContaBancaria, String digitoContaBancaria) {
		this.agenciaBancaria = agenciaBancaria;
		this.numeroContaBancaria = numeroContaBancaria;
		this.digitoContaBancaria = digitoContaBancaria;
	}

	public void inativar() {
		this.status = StatusCadastro.INATIVO;
	}

	public void inativar(Usuario autor) {
		inativar();
		registrarAlteracao(autor);
	}

	public void reativar() {
		this.status = StatusCadastro.ATIVO;
	}

	public void reativar(Usuario autor) {
		reativar();
		registrarAlteracao(autor);
	}

	public boolean estaAtiva() {
		return status == StatusCadastro.ATIVO;
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
