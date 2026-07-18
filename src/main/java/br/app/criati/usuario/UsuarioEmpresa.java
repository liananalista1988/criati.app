package br.app.criati.usuario;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.Empresa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "usuario_empresa", uniqueConstraints = {
		@UniqueConstraint(name = "uk_usuario_empresa_usuario_empresa", columnNames = { "usuario_id", "empresa_id" }) })
public class UsuarioEmpresa {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usuario_empresa_usuario"))
	private Usuario usuario;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_usuario_empresa_empresa"))
	private Empresa empresa;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusUsuarioEmpresa status;

	@Column(name = "data_entrada", nullable = false)
	private Instant dataEntrada;

	@Column(name = "data_saida")
	private Instant dataSaida;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected UsuarioEmpresa() {
	}

	public UsuarioEmpresa(Usuario usuario, Empresa empresa, StatusUsuarioEmpresa status) {
		this.usuario = Objects.requireNonNull(usuario);
		this.empresa = Objects.requireNonNull(empresa);
		this.status = Objects.requireNonNull(status);
		this.dataEntrada = Instant.now();
	}

	@PrePersist
	void aoCriar() {
		Instant agora = Instant.now();
		criadoEm = agora;
		atualizadoEm = agora;
	}

	@PreUpdate
	void aoAtualizar() {
		atualizadoEm = Instant.now();
	}

	public UUID getUuid() {
		return uuid;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public StatusUsuarioEmpresa getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object outro) {
		return this == outro || outro instanceof UsuarioEmpresa vinculo && uuid.equals(vinculo.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
