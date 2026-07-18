package br.app.criati.usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

	Optional<UsuarioEmpresa> findByUsuarioUuidAndEmpresaUuid(UUID usuarioUuid, UUID empresaUuid);

	List<UsuarioEmpresa> findAllByUsuarioUuidAndStatus(UUID usuarioUuid, StatusUsuarioEmpresa status);

	boolean existsByUsuarioUuidAndEmpresaUuidAndStatus(
			UUID usuarioUuid, UUID empresaUuid, StatusUsuarioEmpresa status);
}
