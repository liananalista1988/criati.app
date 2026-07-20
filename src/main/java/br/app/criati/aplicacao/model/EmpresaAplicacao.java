package br.app.criati.aplicacao.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusCadastro;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
		name = "empresa_aplicacao",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_empresa_aplicacao",
				columnNames = { "empresa_id", "aplicacao_id" }))
public class EmpresaAplicacao {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "aplicacao_id", nullable = false, updatable = false)
	private Aplicacao aplicacao;

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

	public EmpresaAplicacao(Empresa empresa, Aplicacao aplicacao, StatusCadastro status) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.aplicacao = Objects.requireNonNull(aplicacao, "aplicacao nao pode ser nula");
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	public void habilitar() {
		this.status = StatusCadastro.ATIVO;
	}

	public void desabilitar() {
		this.status = StatusCadastro.INATIVO;
	}
}
