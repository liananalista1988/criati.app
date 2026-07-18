package br.app.criati.perfil;

import java.util.Objects;
import java.util.UUID;

import br.app.criati.usuario.UsuarioEmpresa;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_empresa_perfil")
public class UsuarioEmpresaPerfil {

	@EmbeddedId
	private UsuarioEmpresaPerfilId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_empresa_id", nullable = false, insertable = false, updatable = false)
	private UsuarioEmpresa usuarioEmpresa;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "perfil_id", nullable = false, insertable = false, updatable = false)
	private Perfil perfil;

	@Column(name = "empresa_id", nullable = false, updatable = false)
	private UUID empresaId;

	protected UsuarioEmpresaPerfil() {
	}

	public UsuarioEmpresaPerfil(UsuarioEmpresa usuarioEmpresa, Perfil perfil) {
		this.usuarioEmpresa = Objects.requireNonNull(usuarioEmpresa);
		this.perfil = Objects.requireNonNull(perfil);
		UUID empresaDoVinculo = usuarioEmpresa.getEmpresa().getUuid();
		if (!empresaDoVinculo.equals(perfil.getEmpresa().getUuid())) {
			throw new IllegalArgumentException("O perfil deve pertencer à empresa do vínculo");
		}
		this.id = new UsuarioEmpresaPerfilId(usuarioEmpresa.getUuid(), perfil.getUuid());
		this.empresaId = empresaDoVinculo;
	}
}
