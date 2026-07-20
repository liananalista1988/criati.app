package br.app.criati.convite.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusConvite;
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
@Table(name = "convite")
public class Convite {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false)
	private Empresa empresa;

	@Column(name = "email", nullable = false, length = 180)
	private String email;

	@Enumerated(EnumType.STRING)
	@Column(name = "perfil", nullable = false, length = 30)
	private PerfilUsuario perfil;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusConvite status;

	@Column(name = "expira_em", nullable = false)
	private OffsetDateTime expiraEm;

	@Column(name = "utilizado_em")
	private OffsetDateTime utilizadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", nullable = false)
	private Usuario criadoPor;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	public Convite(
			Empresa empresa,
			String email,
			PerfilUsuario perfil,
			String tokenHash,
			OffsetDateTime expiraEm,
			Usuario criadoPor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.email = Objects.requireNonNull(email, "email nao pode ser nulo");
		this.perfil = Objects.requireNonNull(perfil, "perfil nao pode ser nulo");
		this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash nao pode ser nulo");
		this.expiraEm = Objects.requireNonNull(expiraEm, "expiraEm nao pode ser nulo");
		this.criadoPor = Objects.requireNonNull(criadoPor, "criadoPor nao pode ser nulo");
		this.status = StatusConvite.PENDENTE;
	}

	// Expiracao e tratada dinamicamente (sem job de fundo): um convite PENDENTE
	// cujo prazo ja passou e considerado expirado em qualquer leitura, sem
	// exigir uma escrita previa no banco.
	public boolean estaEfetivamenteValido(OffsetDateTime agora) {
		return status == StatusConvite.PENDENTE && expiraEm.isAfter(agora);
	}

	// Para exibicao (listagem): reflete a expiracao mesmo quando o status
	// persistido ainda esta como PENDENTE, sem exigir job de fundo.
	public StatusConvite getStatusEfetivo(OffsetDateTime agora) {
		if (status == StatusConvite.PENDENTE && !expiraEm.isAfter(agora)) {
			return StatusConvite.EXPIRADO;
		}
		return status;
	}

	public void marcarUtilizado(OffsetDateTime agora) {
		this.status = StatusConvite.UTILIZADO;
		this.utilizadoEm = agora;
	}

	public void revogar() {
		this.status = StatusConvite.REVOGADO;
	}
}
