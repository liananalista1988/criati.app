package br.app.criati.empresa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusCadastro;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

	boolean existsByCnpj(String cnpj);

	Optional<Empresa> findByCnpj(String cnpj);

	long countByStatus(StatusCadastro status);

	List<Empresa> findTop5ByOrderByCriadoEmDesc();
}
