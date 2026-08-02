package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.LancamentoFinanceiro;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, UUID>,
		JpaSpecificationExecutor<LancamentoFinanceiro> {

	@Override
	@EntityGraph(attributePaths = {"conta", "categoria", "pessoaFinanceira", "parteFinanceira", "criadoPor", "atualizadoPor"})
	Page<LancamentoFinanceiro> findAll(Specification<LancamentoFinanceiro> spec, Pageable pageable);

	List<LancamentoFinanceiro> findAllByEmpresaId(UUID empresaId);

	Optional<LancamentoFinanceiro> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByContaIdAndEmpresaId(UUID contaId, UUID empresaId);

	boolean existsByCategoriaIdAndEmpresaId(UUID categoriaId, UUID empresaId);

	List<LancamentoFinanceiro> findAllByContaIdAndEmpresaId(UUID contaId, UUID empresaId);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndDataCompetenciaBetweenOrderByDataCompetenciaDesc(
			UUID empresaId, java.time.LocalDate inicio, java.time.LocalDate fim);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndPessoaFinanceiraId(UUID empresaId, UUID pessoaFinanceiraId);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndParteFinanceiraId(UUID empresaId, UUID parteFinanceiraId);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndCategoriaId(UUID empresaId, UUID categoriaId);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndTipo(UUID empresaId,
			br.app.criati.shared.enums.TipoFinanceiro tipo);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndStatus(UUID empresaId,
			br.app.criati.shared.enums.StatusLancamentoFinanceiro status);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndDescricaoContainingIgnoreCase(UUID empresaId, String descricao);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndStatusAndDataVencimentoBefore(UUID empresaId,
			br.app.criati.shared.enums.StatusLancamentoFinanceiro status, java.time.LocalDate data);

	boolean existsByRecorrenciaIdAndDataCompetencia(UUID recorrenciaId, java.time.LocalDate dataCompetencia);

	Optional<LancamentoFinanceiro> findByRecorrenciaIdAndDataCompetencia(UUID recorrenciaId,
			java.time.LocalDate dataCompetencia);

	List<LancamentoFinanceiro> findAllByEmpresaIdAndRecorrenciaIdOrderByDataCompetenciaDesc(UUID empresaId,
			UUID recorrenciaId);

	long countByRecorrenciaId(UUID recorrenciaId);

	@Query("""
			select coalesce(sum(l.valor), 0) from LancamentoFinanceiro l
			where l.empresa.id = :empresaId and l.tipo = :tipo and l.status in :status
			""")
	java.math.BigDecimal somarPorEmpresaTipoEStatus(@Param("empresaId") UUID empresaId,
			@Param("tipo") br.app.criati.shared.enums.TipoFinanceiro tipo,
			@Param("status") java.util.Collection<br.app.criati.shared.enums.StatusLancamentoFinanceiro> status);

	@Query("""
			select coalesce(sum(case when l.tipo = br.app.criati.shared.enums.TipoFinanceiro.RECEITA
				then l.valor else -l.valor end), 0)
			from LancamentoFinanceiro l where l.empresa.id = :empresaId and l.conta.id = :contaId
			and l.status in :status
			""")
	java.math.BigDecimal calcularImpactoPorConta(@Param("empresaId") UUID empresaId,
			@Param("contaId") UUID contaId,
			@Param("status") java.util.Collection<br.app.criati.shared.enums.StatusLancamentoFinanceiro> status);
}
