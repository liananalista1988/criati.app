package br.app.criati.usuario;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "email", nullable = false, unique = true, length = 150)
	private String email;

	@JsonIgnore
	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Column(name = "ativo", nullable = false)
	private boolean ativo;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Usuario() {
	}

	public Usuario(String nome, String email, String senhaHash, boolean ativo) {
		this.nome = Objects.requireNonNull(nome);
		this.email = normalizarEmail(email);
		this.senhaHash = Objects.requireNonNull(senhaHash);
		this.ativo = ativo;
	}

	private static String normalizarEmail(String email) {
		return Objects.requireNonNull(email).trim().toLowerCase(Locale.ROOT);
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

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public boolean isAtivo() {
		return ativo;
	}

	@Override
	public boolean equals(Object outro) {
		return this == outro || outro instanceof Usuario usuario && uuid.equals(usuario.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return "Usuario{uuid=" + uuid + ", email='" + email + "', ativo=" + ativo + "}";
	}
}
