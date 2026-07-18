package br.app.criati.perfil;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "perfil", uniqueConstraints = {
		@UniqueConstraint(name = "uk_perfil_empresa_codigo", columnNames = { "empresa_id", "codigo" }) })
public class Perfil {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_perfil_empresa"))
	private Empresa empresa;

	@Column(name = "codigo", nullable = false, length = 50)
	private String codigo;

	@Column(name = "nome", nullable = false, length = 100)
	private String nome;

	@Column(name = "descricao", length = 255)
	private String descricao;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected Perfil() {
	}

	public Perfil(Empresa empresa, String codigo, String nome, String descricao) {
		this.empresa = Objects.requireNonNull(empresa);
		this.codigo = Objects.requireNonNull(codigo);
		this.nome = Objects.requireNonNull(nome);
		this.descricao = descricao;
	}

	@PrePersist
	void aoCriar() {
		criadoEm = Instant.now();
	}

	public UUID getUuid() {
		return uuid;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getNome() {
		return nome;
	}

	@Override
	public boolean equals(Object outro) {
		return this == outro || outro instanceof Perfil perfil && uuid.equals(perfil.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
