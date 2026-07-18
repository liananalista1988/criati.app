package br.app.criati.perfil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<Perfil, UUID> {

	Optional<Perfil> findByUuidAndEmpresaUuid(UUID perfilUuid, UUID empresaUuid);

	List<Perfil> findAllByEmpresaUuid(UUID empresaUuid);
}
