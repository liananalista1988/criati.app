package br.app.criati.acesso.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

	boolean existsByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	List<UsuarioEmpresa> findAllByUsuarioId(UUID usuarioId);

	List<UsuarioEmpresa> findAllByEmpresaId(UUID empresaId);

	List<UsuarioEmpresa> findAllByUsuarioIdAndStatus(UUID usuarioId, StatusCadastro status);

	Optional<UsuarioEmpresa> findByIdAndEmpresaId(UUID id, UUID empresaId);

	long countByEmpresaIdAndPerfilAndStatus(UUID empresaId, PerfilUsuario perfil, StatusCadastro status);

	long countByEmpresaIdAndStatus(UUID empresaId, StatusCadastro status);

	long countByStatus(StatusCadastro status);
}
