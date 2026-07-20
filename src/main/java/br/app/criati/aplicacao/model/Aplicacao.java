package br.app.criati.aplicacao.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.shared.enums.StatusCadastro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "aplicacao")
public class Aplicacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "codigo", nullable = false, unique = true, length = 30, updatable = false)
	private String codigo;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "descricao", length = 500)
	private String descricao;

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

	public Aplicacao(String codigo, String nome, String descricao, StatusCadastro status) {
		this.codigo = Objects.requireNonNull(codigo, "codigo nao pode ser nulo");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.descricao = descricao;
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	public boolean estaAtiva() {
		return status == StatusCadastro.ATIVO;
	}
}
