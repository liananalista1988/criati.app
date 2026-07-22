package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
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
 * Cartao de credito: uma linha de credito concedida por uma instituicao
 * financeira, nunca uma conta bancaria (sem saldo). Esta entrega implementa
 * apenas a estrutura cadastral (titular, instituicao, bandeira, limite,
 * fechamento/vencimento, fisico/virtual) — compras, parcelas e faturas ficam
 * para tarefas futuras. Um cartao virtual (tipo VIRTUAL) sempre aponta para um
 * cartao fisico principal (tipo FISICO) e nunca possui limite/fechamento/
 * vencimento proprios: {@link #getLimiteTotalEfetivo()} e os demais metodos
 * "Efetivo" delegam ao principal, para nunca duplicar o limite concedido no
 * consolidado. Ver docs/empresas/financeiro-les/IMPLEMENTACAO-F3-001.md.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "cartao_credito")
public class CartaoCredito {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pessoa_titular_id", nullable = false)
	private PessoaFinanceira titular;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "instituicao_id", nullable = false)
	private InstituicaoFinanceira instituicao;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoCartao tipo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cartao_principal_id")
	private CartaoCredito cartaoPrincipal;

	@Enumerated(EnumType.STRING)
	@Column(name = "bandeira", nullable = false, length = 30)
	private Bandeira bandeira;

	@Column(name = "ultimos_quatro_digitos", length = 4)
	private String ultimosQuatroDigitos;

	@Column(name = "limite_total", precision = 19, scale = 2)
	private BigDecimal limiteTotal;

	@Column(name = "limite_saudavel", precision = 19, scale = 2)
	private BigDecimal limiteSaudavel;

	@Column(name = "dia_fechamento")
	private Integer diaFechamento;

	@Column(name = "dia_vencimento")
	private Integer diaVencimento;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

	@Column(name = "bloqueado", nullable = false)
	private boolean bloqueado;

	@Column(name = "motivo_bloqueio", length = 500)
	private String motivoBloqueio;

	@Column(name = "observacao", length = 500)
	private String observacao;

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

	public CartaoCredito(Empresa empresa, PessoaFinanceira titular, InstituicaoFinanceira instituicao, String nome,
			TipoCartao tipo, CartaoCredito cartaoPrincipal, Bandeira bandeira, String ultimosQuatroDigitos,
			BigDecimal limiteTotal, BigDecimal limiteSaudavel, Integer diaFechamento, Integer diaVencimento,
			String observacao, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.titular = Objects.requireNonNull(titular, "titular nao pode ser nulo");
		this.instituicao = Objects.requireNonNull(instituicao, "instituicao nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.cartaoPrincipal = cartaoPrincipal;
		this.bandeira = Objects.requireNonNull(bandeira, "bandeira nao pode ser nula");
		this.ultimosQuatroDigitos = ultimosQuatroDigitos;
		this.limiteTotal = limiteTotal;
		this.limiteSaudavel = limiteSaudavel;
		this.diaFechamento = diaFechamento;
		this.diaVencimento = diaVencimento;
		this.status = StatusCadastro.ATIVO;
		this.bloqueado = false;
		this.observacao = observacao;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarDados(PessoaFinanceira titular, InstituicaoFinanceira instituicao, String nome,
			Bandeira bandeira, String ultimosQuatroDigitos, BigDecimal limiteTotal, BigDecimal limiteSaudavel,
			Integer diaFechamento, Integer diaVencimento, String observacao, Usuario autor) {
		this.titular = Objects.requireNonNull(titular, "titular nao pode ser nulo");
		this.instituicao = Objects.requireNonNull(instituicao, "instituicao nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.bandeira = Objects.requireNonNull(bandeira, "bandeira nao pode ser nula");
		this.ultimosQuatroDigitos = ultimosQuatroDigitos;
		this.limiteTotal = limiteTotal;
		this.limiteSaudavel = limiteSaudavel;
		this.diaFechamento = diaFechamento;
		this.diaVencimento = diaVencimento;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public void inativar(Usuario autor) {
		this.status = StatusCadastro.INATIVO;
		registrarAlteracao(autor);
	}

	public void reativar(Usuario autor) {
		this.status = StatusCadastro.ATIVO;
		registrarAlteracao(autor);
	}

	public void bloquear(String motivo, Usuario autor) {
		this.bloqueado = true;
		this.motivoBloqueio = motivo;
		registrarAlteracao(autor);
	}

	public void desbloquear(Usuario autor) {
		this.bloqueado = false;
		this.motivoBloqueio = null;
		registrarAlteracao(autor);
	}

	public boolean estaAtivo() {
		return status == StatusCadastro.ATIVO;
	}

	public boolean ehVirtual() {
		return tipo == TipoCartao.VIRTUAL;
	}

	public boolean ehPrincipal() {
		return tipo == TipoCartao.FISICO;
	}

	/**
	 * Bloquear o principal impede o uso dos virtuais sem alterar silenciosamente
	 * o campo bloqueado de cada um: este metodo reflete o bloqueio efetivo
	 * (proprio OU herdado do principal), sem jamais escrever no cartao virtual.
	 */
	public boolean estaBloqueadoEfetivo() {
		return bloqueado || (cartaoPrincipal != null && cartaoPrincipal.isBloqueado());
	}

	public BigDecimal getLimiteTotalEfetivo() {
		return ehVirtual() && cartaoPrincipal != null ? cartaoPrincipal.getLimiteTotal() : limiteTotal;
	}

	public BigDecimal getLimiteSaudavelEfetivo() {
		return ehVirtual() && cartaoPrincipal != null ? cartaoPrincipal.getLimiteSaudavel() : limiteSaudavel;
	}

	public Integer getDiaFechamentoEfetivo() {
		return ehVirtual() && cartaoPrincipal != null ? cartaoPrincipal.getDiaFechamento() : diaFechamento;
	}

	public Integer getDiaVencimentoEfetivo() {
		return ehVirtual() && cartaoPrincipal != null ? cartaoPrincipal.getDiaVencimento() : diaVencimento;
	}

	/**
	 * Estrutural apenas: ainda nao existem compras nesta entrega, entao o limite
	 * comprometido e sempre zero. Ver LES-F3-001, secao "Limite comprometido".
	 */
	public BigDecimal getLimiteComprometidoEfetivo() {
		return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
	}

	public BigDecimal getLimiteDisponivelEfetivo() {
		BigDecimal total = getLimiteTotalEfetivo();
		if (total == null) {
			return null;
		}
		return total.subtract(getLimiteComprometidoEfetivo());
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
