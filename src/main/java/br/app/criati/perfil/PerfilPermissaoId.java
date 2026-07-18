package br.app.criati.perfil;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PerfilPermissaoId implements Serializable {

	@Column(name = "perfil_id", nullable = false)
	private UUID perfilId;

	@Column(name = "permissao_id", nullable = false)
	private UUID permissaoId;

	protected PerfilPermissaoId() {
	}

	public PerfilPermissaoId(UUID perfilId, UUID permissaoId) {
		this.perfilId = Objects.requireNonNull(perfilId);
		this.permissaoId = Objects.requireNonNull(permissaoId);
	}

	@Override
	public boolean equals(Object outro) {
		if (this == outro) {
			return true;
		}
		if (!(outro instanceof PerfilPermissaoId id)) {
			return false;
		}
		return perfilId.equals(id.perfilId) && permissaoId.equals(id.permissaoId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(perfilId, permissaoId);
	}
}
