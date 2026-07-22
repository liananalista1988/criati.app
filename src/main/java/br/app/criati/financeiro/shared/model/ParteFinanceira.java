package br.app.criati.financeiro.shared.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusCadastro;
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
@Table(name = "parte_financeira")
public class ParteFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 30)
	private TipoParteFinanceira tipo;

	@Column(name = "documento", length = 30)
	private String documento;

	@Column(name = "apelido", length = 100)
	private String apelido;

	@Column(name = "observacao", length = 500)
	private String observacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

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

	public ParteFinanceira(
			Empresa empresa,
			String nome,
			TipoParteFinanceira tipo,
			String documento,
			String apelido,
			String observacao,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.documento = documento;
		this.apelido = apelido;
		this.observacao = observacao;
		this.status = StatusCadastro.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizar(
			String nome,
			TipoParteFinanceira tipo,
			String documento,
			String apelido,
			String observacao,
			Usuario autor) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.tipo = Objects.requireNonNull(tipo, "tipo nao pode ser nulo");
		this.documento = documento;
		this.apelido = apelido;
		this.observacao = observacao;
		registrarAlteracao(autor);
	}

	public void desativar(Usuario autor) {
		this.status = StatusCadastro.INATIVO;
		registrarAlteracao(autor);
	}

	public void reativar(Usuario autor) {
		this.status = StatusCadastro.ATIVO;
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
