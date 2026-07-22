package br.app.criati.financeiro.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.PagamentoOcorrenciaCompromisso;
import br.app.criati.shared.enums.StatusPagamentoOcorrencia;

public interface PagamentoOcorrenciaCompromissoRepository extends JpaRepository<PagamentoOcorrenciaCompromisso, UUID> {

	List<PagamentoOcorrenciaCompromisso> findAllByEmpresaIdAndOcorrenciaIdOrderByDataPagamentoDesc(
			UUID empresaId, UUID ocorrenciaId);

	Optional<PagamentoOcorrenciaCompromisso> findByIdAndEmpresaId(UUID id, UUID empresaId);

	Optional<PagamentoOcorrenciaCompromisso> findByIdAndOcorrenciaIdAndEmpresaId(
			UUID id, UUID ocorrenciaId, UUID empresaId);

	boolean existsByLancamentoFinanceiroId(UUID lancamentoFinanceiroId);

	boolean existsByContaIdAndEmpresaId(UUID contaId, UUID empresaId);

	@Query("""
			select coalesce(sum(p.valor), 0) from PagamentoOcorrenciaCompromisso p
			where p.ocorrencia.id = :ocorrenciaId and p.status = :status
			""")
	BigDecimal somarPorOcorrenciaEStatus(@Param("ocorrenciaId") UUID ocorrenciaId,
			@Param("status") StatusPagamentoOcorrencia status);
}
