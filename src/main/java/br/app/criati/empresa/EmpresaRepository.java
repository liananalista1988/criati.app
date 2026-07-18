package br.app.criati.empresa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

	Optional<Empresa> findByUuid(UUID uuid);

	Optional<Empresa> findByCnpj(String cnpj);
}
