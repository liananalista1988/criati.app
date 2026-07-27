package br.app.criati.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.shared.enums.StatusFaturaCartao;
import jakarta.persistence.LockModeType;

public interface FaturaCartaoRepository extends JpaRepository<FaturaCartao, UUID> {

	List<FaturaCartao> findAllByEmpresaIdOrderByCompetenciaDesc(UUID empresaId);

	List<FaturaCartao> findAllByEmpresaIdAndCartaoPrincipalIdOrderByCompetenciaDesc(
			UUID empresaId, UUID cartaoPrincipalId);

	List<FaturaCartao> findAllByEmpresaIdAndStatusOrderByCompetenciaDesc(
			UUID empresaId, StatusFaturaCartao status);

	Optional<FaturaCartao> findByIdAndEmpresaId(UUID id, UUID empresaId);

	Optional<FaturaCartao> findByEmpresaIdAndCartaoPrincipalIdAndCompetencia(
			UUID empresaId, UUID cartaoPrincipalId, LocalDate competencia);

	boolean existsByEmpresaIdAndCartaoPrincipalIdAndCompetenciaAndStatus(
			UUID empresaId, UUID cartaoPrincipalId, LocalDate competencia, StatusFaturaCartao status);

	@Query("""
			select (count(f) > 0) from FaturaCartao f
			where f.empresa.id = :empresaId
			  and f.status = :status
			  and exists (
			    select p.id from ParcelaCompraCartao p
			    where p.compra.id = :compraId and p.faturaId = f.id
			  )
			""")
	boolean existeFaturaFechadaDaCompra(
			@Param("empresaId") UUID empresaId,
			@Param("compraId") UUID compraId,
			@Param("status") StatusFaturaCartao status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<FaturaCartao> findForUpdateByIdAndEmpresaId(UUID id, UUID empresaId);
}
