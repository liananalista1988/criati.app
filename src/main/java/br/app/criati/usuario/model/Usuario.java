package br.app.criati.usuario.model;

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
@Table(name = "usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "email", nullable = false, unique = true, length = 180)
	private String email;

	@Column(name = "senha", nullable = false, length = 255)
	private String senha;

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

	@Column(name = "super_administrador", nullable = false)
	private boolean superAdministrador;

	public Usuario(String nome, String email, String senha, StatusCadastro status) {
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.email = Objects.requireNonNull(email, "email nao pode ser nulo");
		this.senha = Objects.requireNonNull(senha, "senha nao pode ser nula");
		this.status = Objects.requireNonNull(status, "status nao pode ser nulo");
	}

	// Superadministrador e global e nunca decorre do cadastro comum (construtor
	// acima); esta e a unica via de criacao, usada apenas pelo bootstrap.
	public static Usuario criarSuperAdministrador(String nome, String email, String senhaCodificada) {
		Usuario usuario = new Usuario(nome, email, senhaCodificada, StatusCadastro.ATIVO);
		usuario.superAdministrador = true;
		return usuario;
	}

	// Recebe sempre o hash ja codificado pelo PasswordEncoder do chamador
	// (CRIATI-SEG-001) — esta entidade nunca codifica senha por conta propria,
	// mesmo padrao ja usado no cadastro (CadastrarUsuarioService) e no aceite
	// de convite (AceitarConviteService).
	public void redefinirSenha(String senhaCodificada) {
		this.senha = Objects.requireNonNull(senhaCodificada, "senhaCodificada nao pode ser nula");
	}
}
