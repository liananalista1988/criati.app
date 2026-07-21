package br.app.criati.empresa.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.exception.EmpresaStatusInvalidoException;
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
@Table(name = "empresa")
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "nome_fantasia", length = 150)
	private String nomeFantasia;

	@Column(name = "cnpj", nullable = false, unique = true, length = 14)
	private String cnpj;

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

	public Empresa(String nome, String nomeFantasia, String cnpj, StatusCadastro status) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.nomeFantasia = nomeFantasia;
		this.cnpj = Objects.requireNonNull(cnpj, "cnpj nao pode ser nulo");
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	// Inativar so bloqueia acesso (contexto/sessao passam a rejeitar a
	// empresa); nunca exclui dados, usuarios, vinculos, aplicacoes ou
	// lancamentos - mesmo padrao ja usado por UsuarioEmpresa/EmpresaAplicacao.
	public void ativar() {
		if (this.status == StatusCadastro.ATIVO) {
			throw new EmpresaStatusInvalidoException("Empresa ja esta ativa");
		}
		this.status = StatusCadastro.ATIVO;
	}

	public void inativar() {
		if (this.status == StatusCadastro.INATIVO) {
			throw new EmpresaStatusInvalidoException("Empresa ja esta inativa");
		}
		this.status = StatusCadastro.INATIVO;
	}
}
