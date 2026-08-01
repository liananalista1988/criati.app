package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import br.app.criati.financeiro.model.LoteImportacaoBancaria;

public interface LoteImportacaoBancariaRepository extends JpaRepository<LoteImportacaoBancaria, UUID> {

	List<LoteImportacaoBancaria> findAllByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

	Optional<LoteImportacaoBancaria> findByIdAndEmpresaId(UUID id, UUID empresaId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from LoteImportacaoBancaria l where l.id = :id and l.empresa.id = :empresaId")
	Optional<LoteImportacaoBancaria> findForUpdateByIdAndEmpresaId(
			@Param("id") UUID id, @Param("empresaId") UUID empresaId);

	boolean existsByEmpresaIdAndHashArquivo(UUID empresaId, String hashArquivo);
}
