package br.app.criati.permissao;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissao")
public class Permissao {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@Column(name = "codigo", nullable = false, unique = true, length = 100)
	private String codigo;

	@Column(name = "descricao", nullable = false, length = 255)
	private String descricao;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	protected Permissao() {
	}

	public Permissao(String codigo, String descricao) {
		this.codigo = Objects.requireNonNull(codigo);
		this.descricao = Objects.requireNonNull(descricao);
	}

	@PrePersist
	void aoCriar() {
		criadoEm = Instant.now();
	}

	public UUID getUuid() {
		return uuid;
	}

	public String getCodigo() {
		return codigo;
	}

	@Override
	public boolean equals(Object outro) {
		return this == outro || outro instanceof Permissao permissao && uuid.equals(permissao.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}
}
