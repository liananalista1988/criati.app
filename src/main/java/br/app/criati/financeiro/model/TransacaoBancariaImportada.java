package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.OrigemClassificacaoTransacaoImportada;
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

	// Nulo enquanto o lote nao tiver conta resolvida (CRIATI-IMP-FEAT-004);
	// atualizada em bloco quando LoteImportacaoBancaria.resolverConta e chamado.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "conta_id")
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

	// Atualizavel: recalculada quando o lote resolve a conta a posteriori
	// (LoteImportacaoBancaria/ImportacaoBancariaService.resolverConta,
	// CRIATI-IMP-FIX-007) - antes disso nao ha conta para comparar.
	@Column(name = "possivelmente_ja_importada", nullable = false)
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "regra_classificacao_id")
	private RegraClassificacaoImportacao regraClassificacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "origem_classificacao", nullable = false, length = 20)
	private OrigemClassificacaoTransacaoImportada origemClassificacao;

	public TransacaoBancariaImportada(Empresa empresa, LoteImportacaoBancaria lote, ContaFinanceira conta,
			int sequencia, LocalDate dataTransacao, BigDecimal valor, String tipoBancario, String descricao,
			String identificadorBancario, String documento, String chaveDuplicidade, boolean duplicadaNoArquivo,
			boolean possivelmenteJaImportada) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.lote = Objects.requireNonNull(lote, "lote nao pode ser nulo");
		this.conta = conta;
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
		this.origemClassificacao = OrigemClassificacaoTransacaoImportada.MANUAL;
	}

	public boolean estaSinalizadaComoDuplicada() {
		return duplicadaNoArquivo || possivelmenteJaImportada;
	}

	/**
	 * Define a conta desta transacao quando o lote resolve a conta a
	 * posteriori (CRIATI-IMP-FIX-007) - chamado exclusivamente por
	 * ImportacaoBancariaService.resolverConta, uma unica vez por transacao
	 * (o lote so permite resolver conta uma vez).
	 */
	public void resolverConta(ContaFinanceira conta) {
		this.conta = Objects.requireNonNull(conta, "conta nao pode ser nula");
	}

	/**
	 * Recalculada sempre que a conta e definida ou redefinida (hoje so ocorre
	 * uma vez, via resolverConta) - preserva a regra definitiva empresa+
	 * conta+chave, agora que a conta e conhecida (CRIATI-IMP-FIX-007).
	 */
	public void atualizarPossivelmenteJaImportada(boolean possivelmenteJaImportada) {
		this.possivelmenteJaImportada = possivelmenteJaImportada;
	}

	/**
	 * regra e origem sao opcionais: regra nula + origem MANUAL cobre tanto o
	 * historico anterior a esta funcionalidade quanto uma confirmacao em que
	 * nenhuma regra foi efetivamente usada (nunca ambos nao-nulos/MANUAL ao
	 * mesmo tempo - ver ConfirmacaoImportacaoBancariaService, que decide isso
	 * antes de chamar este metodo).
	 */
	public void confirmar(LancamentoFinanceiro lancamento, Usuario autor,
			RegraClassificacaoImportacao regra, OrigemClassificacaoTransacaoImportada origem) {
		if (situacao != SituacaoTransacaoImportada.PENDENTE) {
			throw new DadosInvalidosException("Somente transacao pendente pode ser confirmada");
		}
		this.lancamentoFinanceiro = Objects.requireNonNull(lancamento, "lancamento nao pode ser nulo");
		this.confirmadaEm = OffsetDateTime.now();
		this.confirmadaPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.situacao = SituacaoTransacaoImportada.CONFIRMADA;
		this.regraClassificacao = regra;
		this.origemClassificacao = Objects.requireNonNull(origem, "origem nao pode ser nula");
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
