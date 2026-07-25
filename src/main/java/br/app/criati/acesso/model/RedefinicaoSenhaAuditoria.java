package br.app.criati.acesso.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.AcaoAuditoriaSeguranca;
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
 * Registro imutavel de eventos de seguranca sensiveis (CRIATI-SEG-001: hoje
 * so a redefinicao administrativa de senha). Nunca armazena senha, hash ou
 * qualquer credencial — apenas quem fez, em quem, quando e qual acao. Sem
 * metodo de alteracao proposital: uma vez criado, o registro nao e editado
 * nem excluido pelo fluxo comum (mesmo espirito de imutabilidade ja usado em
 * RessarcimentoParcelaCartao, aqui reforcado por nao existir nenhum setter).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "redefinicao_senha_auditoria")
public class RedefinicaoSenhaAuditoria {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "administrador_id", nullable = false, updatable = false)
	private Usuario administrador;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_afetado_id", nullable = false, updatable = false)
	private Usuario usuarioAfetado;

	@Enumerated(EnumType.STRING)
	@Column(name = "acao", nullable = false, length = 40, updatable = false)
	private AcaoAuditoriaSeguranca acao;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	public RedefinicaoSenhaAuditoria(
			Empresa empresa, Usuario administrador, Usuario usuarioAfetado, AcaoAuditoriaSeguranca acao) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.administrador = Objects.requireNonNull(administrador, "administrador nao pode ser nulo");
		this.usuarioAfetado = Objects.requireNonNull(usuarioAfetado, "usuarioAfetado nao pode ser nulo");
		this.acao = Objects.requireNonNull(acao, "acao nao pode ser nula");
	}
}
