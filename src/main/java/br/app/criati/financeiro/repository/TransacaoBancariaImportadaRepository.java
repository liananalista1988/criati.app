package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.TransacaoBancariaImportada;
public interface TransacaoBancariaImportadaRepository extends JpaRepository<TransacaoBancariaImportada, UUID> {

	List<TransacaoBancariaImportada> findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(UUID empresaId, UUID loteId);

	boolean existsByEmpresaIdAndContaIdAndChaveDuplicidade(
			UUID empresaId, UUID contaId, String chaveDuplicidade);

	long countByEmpresaId(UUID empresaId);
}
