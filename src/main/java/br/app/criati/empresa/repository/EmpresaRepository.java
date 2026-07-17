package br.app.criati.empresa.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.empresa.model.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

	boolean existsByCnpj(String cnpj);

	Optional<Empresa> findByCnpj(String cnpj);
}
