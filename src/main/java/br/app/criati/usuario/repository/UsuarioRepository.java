package br.app.criati.usuario.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

	boolean existsByEmailIgnoreCase(String email);

	Optional<Usuario> findByEmailIgnoreCase(String email);

	boolean existsBySuperAdministradorTrue();

	long countByStatus(StatusCadastro status);
}
