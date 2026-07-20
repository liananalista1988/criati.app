package br.app.criati.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;

public class UsuarioPrincipal implements UserDetails {

	public static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";

	private final Usuario usuario;

	public UsuarioPrincipal(Usuario usuario) {
		this.usuario = usuario;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		// Perfis de empresa (ADMINISTRADOR/GESTOR/USUARIO) nao viram authority:
		// eles dependem da empresa ativa na sessao e sao verificados via
		// ContextoEmpresaAtual, nao pelo Spring Security. Somente o papel global
		// de Superadministrador e exposto como authority.
		if (usuario.isSuperAdministrador()) {
			return List.of(new SimpleGrantedAuthority(ROLE_SUPERADMIN));
		}
		return List.of();
	}

	@Override
	public String getPassword() {
		return usuario.getSenha();
	}

	@Override
	public String getUsername() {
		return usuario.getEmail();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return usuario.getStatus() == StatusCadastro.ATIVO;
	}
}
