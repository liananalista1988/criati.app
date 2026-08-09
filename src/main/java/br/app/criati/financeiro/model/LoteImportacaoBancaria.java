package br.app.criati.financeiro.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.FormatoArquivoImportacao;
import br.app.criati.shared.enums.StatusLoteImportacao;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "lote_importacao_bancaria", uniqueConstraints = @UniqueConstraint(
		name = "uq_lote_importacao_empresa_hash", columnNames = { "empresa_id", "hash_arquivo" }))
public class LoteImportacaoBancaria {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	// Nulo enquanto a importacao estiver em revisao sem conta resolvida
	// (CRIATI-IMP-FEAT-004); resolverConta() define uma unica vez.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_id")
	private ContaFinanceira conta;

	// Sugestao de autodetecao (nunca confirma nada sozinha) - preenchida so
	// quando o OFX fornece BANKID+BRANCHID+ACCTID completos e ha exatamente
	// uma conta compativel na empresa atual.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_sugerida_id", updatable = false)
	private ContaFinanceira contaSugerida;

	// Metadados do extrato (BANKID/BRANCHID/ACCTID/ACCTTYPE), armazenados uma
	// unica vez por lote - nunca repetidos por transacao.
	@Column(name = "identificacao_banco_id", length = 20, updatable = false)
	private String identificacaoBancoId;

	@Column(name = "identificacao_agencia", length = 20, updatable = false)
	private String identificacaoAgencia;

	@Column(name = "identificacao_numero_conta", length = 30, updatable = false)
	private String identificacaoNumeroConta;

	@Column(name = "identificacao_tipo_conta", length = 20, updatable = false)
	private String identificacaoTipoConta;

	@Column(name = "hash_arquivo", nullable = false, length = 64, updatable = false)
	private String hashArquivo;

	@Enumerated(EnumType.STRING)
	@Column(name = "formato", nullable = false, length = 20, updatable = false)
	private FormatoArquivoImportacao formato;

	@Column(name = "nome_original", nullable = false, length = 255, updatable = false)
	private String nomeOriginal;

	@Column(name = "tamanho_bytes", nullable = false, updatable = false)
	private long tamanhoBytes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private StatusLoteImportacao status;

	@Column(name = "quantidade_transacoes", nullable = false, updatable = false)
	private int quantidadeTransacoes;

	@Column(name = "quantidade_duplicadas_arquivo", nullable = false, updatable = false)
	private int quantidadeDuplicadasArquivo;

	@Column(name = "quantidade_possiveis_duplicadas", nullable = false, updatable = false)
	private int quantidadePossiveisDuplicadas;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false, updatable = false)
	private Usuario criadoPor;

	@Column(name = "descartado_em")
	private OffsetDateTime descartadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "descartado_por_usuario_id")
	private Usuario descartadoPor;

	public LoteImportacaoBancaria(Empresa empresa, ContaFinanceira conta, FormatoArquivoImportacao formato,
			String hashArquivo,
			String nomeOriginal, long tamanhoBytes, int quantidadeTransacoes, int quantidadeDuplicadasArquivo,
			int quantidadePossiveisDuplicadas, Usuario autor,
			ContaFinanceira contaSugerida, String identificacaoBancoId, String identificacaoAgencia,
			String identificacaoNumeroConta, String identificacaoTipoConta) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.conta = conta;
		this.hashArquivo = Objects.requireNonNull(hashArquivo, "hashArquivo nao pode ser nulo");
		this.formato = Objects.requireNonNull(formato, "formato nao pode ser nulo");
		this.nomeOriginal = Objects.requireNonNull(nomeOriginal, "nomeOriginal nao pode ser nulo");
		this.tamanhoBytes = tamanhoBytes;
		this.status = StatusLoteImportacao.PREVIA_DISPONIVEL;
		this.quantidadeTransacoes = quantidadeTransacoes;
		this.quantidadeDuplicadasArquivo = quantidadeDuplicadasArquivo;
		this.quantidadePossiveisDuplicadas = quantidadePossiveisDuplicadas;
		this.criadoEm = OffsetDateTime.now();
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.contaSugerida = contaSugerida;
		this.identificacaoBancoId = identificacaoBancoId;
		this.identificacaoAgencia = identificacaoAgencia;
		this.identificacaoNumeroConta = identificacaoNumeroConta;
		this.identificacaoTipoConta = identificacaoTipoConta;
	}

	/**
	 * Resolve a conta financeira do lote inteiro (todas as suas transacoes
	 * sao atualizadas pelo chamador na mesma transacao) - so pode ser feito
	 * uma unica vez; um lote com conta ja resolvida e imutavel quanto a isso,
	 * mesmo que a escolha final divirja da sugestao automatica (selecao
	 * manual sempre permitida, sem bloqueio - CRIATI-IMP-FEAT-004).
	 */
	public void resolverConta(ContaFinanceira conta) {
		if (this.conta != null) {
			throw new DadosInvalidosException("Lote de importacao ja possui conta financeira resolvida");
		}
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
	}

	public void descartar(Usuario autor) {
		if (status == StatusLoteImportacao.DESCARTADO) {
			throw new DadosInvalidosException("Lote de importacao ja esta descartado");
		}
		status = StatusLoteImportacao.DESCARTADO;
		descartadoEm = OffsetDateTime.now();
		descartadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}
}
