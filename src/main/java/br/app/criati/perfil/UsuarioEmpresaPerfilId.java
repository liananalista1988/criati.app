package br.app.criati.perfil;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UsuarioEmpresaPerfilId implements Serializable {

	@Column(name = "usuario_empresa_id", nullable = false)
	private UUID usuarioEmpresaId;

	@Column(name = "perfil_id", nullable = false)
	private UUID perfilId;

	protected UsuarioEmpresaPerfilId() {
	}

	public UsuarioEmpresaPerfilId(UUID usuarioEmpresaId, UUID perfilId) {
		this.usuarioEmpresaId = Objects.requireNonNull(usuarioEmpresaId);
		this.perfilId = Objects.requireNonNull(perfilId);
	}

	@Override
	public boolean equals(Object outro) {
		if (this == outro) {
			return true;
		}
		if (!(outro instanceof UsuarioEmpresaPerfilId id)) {
			return false;
		}
		return usuarioEmpresaId.equals(id.usuarioEmpresaId) && perfilId.equals(id.perfilId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(usuarioEmpresaId, perfilId);
	}
}
