package br.app.criati.empresa;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "empresa")
public class Empresa {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private UUID uuid = UUID.randomUUID();

	@Column(name = "razao_social", nullable = false, length = 150)
	private String razaoSocial;

	@Column(name = "nome_fantasia", nullable = false, length = 150)
	private String nomeFantasia;

	@Column(name = "cnpj", unique = true, length = 14)
	private String cnpj;

	@Column(name = "email", length = 150)
	private String email;

	@Column(name = "telefone", length = 20)
	private String telefone;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusEmpresa status;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Empresa() {
	}

	public Empresa(String razaoSocial, String nomeFantasia, String cnpj, StatusEmpresa status) {
		this.razaoSocial = Objects.requireNonNull(razaoSocial);
		this.nomeFantasia = Objects.requireNonNull(nomeFantasia);
		this.cnpj = cnpj;
		this.status = Objects.requireNonNull(status);
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

	public String getRazaoSocial() {
		return razaoSocial;
	}

	public String getNomeFantasia() {
		return nomeFantasia;
	}

	public String getCnpj() {
		return cnpj;
	}

	public StatusEmpresa getStatus() {
		return status;
	}

	@Override
	public boolean equals(Object outro) {
		return this == outro || outro instanceof Empresa empresa && uuid.equals(empresa.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return "Empresa{uuid=" + uuid + ", nomeFantasia='" + nomeFantasia + "'}";
	}
}
