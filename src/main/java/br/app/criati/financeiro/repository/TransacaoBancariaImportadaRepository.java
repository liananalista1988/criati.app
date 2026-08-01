package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
import jakarta.persistence.LockModeType;
public interface TransacaoBancariaImportadaRepository extends JpaRepository<TransacaoBancariaImportada, UUID> {

	List<TransacaoBancariaImportada> findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(UUID empresaId, UUID loteId);

	List<TransacaoBancariaImportada> findAllByEmpresaIdAndLoteIdAndSituacaoOrderBySequenciaAsc(
			UUID empresaId, UUID loteId, SituacaoTransacaoImportada situacao);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select t from TransacaoBancariaImportada t
			where t.empresa.id = :empresaId and t.lote.id = :loteId and t.id in :ids
			order by t.id
			""")
	List<TransacaoBancariaImportada> findAllForUpdate(
			@Param("empresaId") UUID empresaId, @Param("loteId") UUID loteId,
			@Param("ids") List<UUID> ids);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select t from TransacaoBancariaImportada t
			where t.empresa.id = :empresaId and t.lote.id = :loteId and t.id = :id
			""")
	java.util.Optional<TransacaoBancariaImportada> findForUpdateById(
			@Param("empresaId") UUID empresaId, @Param("loteId") UUID loteId, @Param("id") UUID id);

	boolean existsByEmpresaIdAndContaIdAndChaveDuplicidade(
			UUID empresaId, UUID contaId, String chaveDuplicidade);

	long countByEmpresaId(UUID empresaId);
}
