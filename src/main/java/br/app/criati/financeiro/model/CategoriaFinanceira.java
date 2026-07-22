package br.app.criati.financeiro.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusCadastro;
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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "categoria_financeira")
public class CategoriaFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "descricao", length = 500)
	private String descricao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "categoria_pai_id")
	private CategoriaFinanceira categoriaPai;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoFinanceiro tipo;

	@Column(name = "ordem_exibicao", nullable = false)
	private int ordemExibicao;

	@Column(name = "permite_orcamento", nullable = false)
	private boolean permiteOrcamento;

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

	public CategoriaFinanceira(Empresa empresa, String nome, TipoFinanceiro tipo, StatusCadastro status) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.ordemExibicao = 0;
		this.permiteOrcamento = tipo == TipoFinanceiro.DESPESA;
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	public CategoriaFinanceira(Empresa empresa, String nome, String descricao, CategoriaFinanceira categoriaPai,
			TipoFinanceiro tipo, int ordemExibicao, boolean permiteOrcamento, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.descricao = descricao;
		this.categoriaPai = categoriaPai;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.ordemExibicao = ordemExibicao;
		this.permiteOrcamento = permiteOrcamento;
		this.status = StatusCadastro.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarDados(String nome, TipoFinanceiro tipo) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
	}

	public void atualizarDados(String nome, String descricao, CategoriaFinanceira categoriaPai,
			TipoFinanceiro tipo, int ordemExibicao, boolean permiteOrcamento, Usuario autor) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.descricao = descricao;
		this.categoriaPai = categoriaPai;
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.ordemExibicao = ordemExibicao;
		this.permiteOrcamento = permiteOrcamento;
		registrarAlteracao(autor);
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
