package br.app.criati.admin.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.shared.enums.AcaoAuditoriaSegurancaGlobal;
import br.app.criati.shared.enums.MotivoAuditoriaSeguranca;
import br.app.criati.shared.enums.ResultadoAuditoriaSeguranca;
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

/**
 * Registro imutavel de eventos de seguranca da redefinicao GLOBAL de senha
 * (Superadministrador redefinindo a senha de qualquer usuario da plataforma,
 * sem depender de empresa ativa ou vinculo). Deliberadamente separada de
 * {@link br.app.criati.acesso.model.RedefinicaoSenhaAuditoria} (fluxo
 * empresarial da CRIATI-SEG-001, que exige empresa e so audita sucesso): aqui
 * NAO ha empresa, e tanto sucesso quanto tentativas negadas/falhas geram um
 * registro (ver resultado/motivo). Nunca armazena senha, hash ou qualquer
 * credencial. Sem metodo de alteracao proposital - uma vez criado, o registro
 * nao e editado nem excluido pelo fluxo comum.
 *
 * <p>{@code usuarioAlvoId} nao tem chave estrangeira (diferente de
 * {@code administrador}, que sempre existe pois e o chamador autenticado):
 * uma tentativa contra um id que nao corresponde a nenhum usuario real
 * (motivo {@code USUARIO_NAO_ENCONTRADO}) tambem precisa ser auditavel, e uma
 * FK impediria justamente esse caso.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "redefinicao_senha_global_auditoria")
public class RedefinicaoSenhaGlobalAuditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "administrador_id", nullable = false, updatable = false)
	private Usuario administrador;

	@Column(name = "usuario_alvo_id", nullable = false, updatable = false)
	private UUID usuarioAlvoId;

	@Enumerated(EnumType.STRING)
	@Column(name = "acao", nullable = false, length = 50, updatable = false)
	private AcaoAuditoriaSegurancaGlobal acao;

	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, length = 20, updatable = false)
	private ResultadoAuditoriaSeguranca resultado;

	@Enumerated(EnumType.STRING)
	@Column(name = "motivo", nullable = false, length = 30, updatable = false)
	private MotivoAuditoriaSeguranca motivo;

	@Column(name = "ip_origem", length = 45, updatable = false)
	private String ipOrigem;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	public RedefinicaoSenhaGlobalAuditoria(Usuario administrador, UUID usuarioAlvoId,
			AcaoAuditoriaSegurancaGlobal acao, ResultadoAuditoriaSeguranca resultado,
			MotivoAuditoriaSeguranca motivo, String ipOrigem) {
		this.administrador = Objects.requireNonNull(administrador, "administrador nao pode ser nulo");
		this.usuarioAlvoId = Objects.requireNonNull(usuarioAlvoId, "usuarioAlvoId nao pode ser nulo");
		this.acao = Objects.requireNonNull(acao, "acao nao pode ser nula");
		this.resultado = Objects.requireNonNull(resultado, "resultado nao pode ser nulo");
		this.motivo = Objects.requireNonNull(motivo, "motivo nao pode ser nulo");
		this.ipOrigem = ipOrigem;
	}
}
