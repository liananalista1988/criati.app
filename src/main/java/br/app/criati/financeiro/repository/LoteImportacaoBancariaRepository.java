package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.LoteImportacaoBancaria;

public interface LoteImportacaoBancariaRepository extends JpaRepository<LoteImportacaoBancaria, UUID> {

	List<LoteImportacaoBancaria> findAllByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

	Optional<LoteImportacaoBancaria> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByEmpresaIdAndHashArquivo(UUID empresaId, String hashArquivo);
}
