package br.app.criati.perfil;

import java.util.Objects;

import br.app.criati.permissao.Permissao;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfil_permissao")
public class PerfilPermissao {

	@EmbeddedId
	private PerfilPermissaoId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "perfil_id", nullable = false, insertable = false, updatable = false)
	private Perfil perfil;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "permissao_id", nullable = false, insertable = false, updatable = false)
	private Permissao permissao;

	protected PerfilPermissao() {
	}

	public PerfilPermissao(Perfil perfil, Permissao permissao) {
		this.perfil = Objects.requireNonNull(perfil);
		this.permissao = Objects.requireNonNull(permissao);
		this.id = new PerfilPermissaoId(perfil.getUuid(), permissao.getUuid());
	}
}
