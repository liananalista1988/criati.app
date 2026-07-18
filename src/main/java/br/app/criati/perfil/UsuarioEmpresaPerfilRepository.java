package br.app.criati.perfil;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioEmpresaPerfilRepository
		extends JpaRepository<UsuarioEmpresaPerfil, UsuarioEmpresaPerfilId> {
}
