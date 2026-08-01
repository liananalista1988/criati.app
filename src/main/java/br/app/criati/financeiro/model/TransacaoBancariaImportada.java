package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "transacao_bancaria_importada")
public class TransacaoBancariaImportada {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lote_id", nullable = false, updatable = false)
	private LoteImportacaoBancaria lote;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "conta_id", nullable = false, updatable = false)
	private ContaFinanceira conta;

	@Column(name = "sequencia", nullable = false, updatable = false)
	private int sequencia;

	@Column(name = "data_transacao", nullable = false, updatable = false)
	private LocalDate dataTransacao;

	@Column(name = "valor", nullable = false, precision = 19, scale = 2, updatable = false)
	private BigDecimal valor;

	@Column(name = "tipo_bancario", nullable = false, length = 40, updatable = false)
	private String tipoBancario;

	@Column(name = "descricao", length = 500, updatable = false)
	private String descricao;

	@Column(name = "identificador_bancario", length = 150, updatable = false)
	private String identificadorBancario;

	@Column(name = "documento", length = 100, updatable = false)
	private String documento;

	@Column(name = "chave_duplicidade", nullable = false, length = 64, updatable = false)
	private String chaveDuplicidade;

	@Column(name = "duplicada_no_arquivo", nullable = false, updatable = false)
	private boolean duplicadaNoArquivo;

	@Column(name = "possivelmente_ja_importada", nullable = false, updatable = false)
	private boolean possivelmenteJaImportada;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao", nullable = false, length = 20)
	private SituacaoTransacaoImportada situacao;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lancamento_financeiro_id", unique = true)
	private LancamentoFinanceiro lancamentoFinanceiro;

	@Column(name = "confirmada_em")
	private OffsetDateTime confirmadaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "confirmada_por_usuario_id")
	private Usuario confirmadaPor;

	@Column(name = "ignorada_em")
	private OffsetDateTime ignoradaEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ignorada_por_usuario_id")
	private Usuario ignoradaPor;

	public TransacaoBancariaImportada(Empresa empresa, LoteImportacaoBancaria lote, ContaFinanceira conta,
			int sequencia, LocalDate dataTransacao, BigDecimal valor, String tipoBancario, String descricao,
			String identificadorBancario, String documento, String chaveDuplicidade, boolean duplicadaNoArquivo,
			boolean possivelmenteJaImportada) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.lote = Objects.requireNonNull(lote, "lote nao pode ser nulo");
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
		this.sequencia = sequencia;
		this.dataTransacao = Objects.requireNonNull(dataTransacao, "dataTransacao nao pode ser nula");
		this.valor = Objects.requireNonNull(valor, "valor nao pode ser nulo");
		this.tipoBancario = Objects.requireNonNull(tipoBancario, "tipoBancario nao pode ser nulo");
		this.descricao = descricao;
		this.identificadorBancario = identificadorBancario;
		this.documento = documento;
		this.chaveDuplicidade = Objects.requireNonNull(chaveDuplicidade, "chaveDuplicidade nao pode ser nula");
		this.duplicadaNoArquivo = duplicadaNoArquivo;
		this.possivelmenteJaImportada = possivelmenteJaImportada;
		this.criadoEm = OffsetDateTime.now();
		this.situacao = SituacaoTransacaoImportada.PENDENTE;
	}

	public boolean estaSinalizadaComoDuplicada() {
		return duplicadaNoArquivo || possivelmenteJaImportada;
	}

	public void confirmar(LancamentoFinanceiro lancamento, Usuario autor) {
		if (situacao != SituacaoTransacaoImportada.PENDENTE) {
			throw new DadosInvalidosException("Somente transacao pendente pode ser confirmada");
		}
		this.lancamentoFinanceiro = Objects.requireNonNull(lancamento, "lancamento nao pode ser nulo");
		this.confirmadaEm = OffsetDateTime.now();
		this.confirmadaPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.situacao = SituacaoTransacaoImportada.CONFIRMADA;
	}

	public void ignorar(Usuario autor) {
		if (situacao == SituacaoTransacaoImportada.CONFIRMADA) {
			throw new DadosInvalidosException("Transacao confirmada nao pode ser ignorada");
		}
		if (situacao == SituacaoTransacaoImportada.IGNORADA) {
			return;
		}
		this.ignoradaEm = OffsetDateTime.now();
		this.ignoradaPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.situacao = SituacaoTransacaoImportada.IGNORADA;
	}
}
