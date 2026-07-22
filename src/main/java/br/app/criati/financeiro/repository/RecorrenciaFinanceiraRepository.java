package br.app.criati.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoFinanceiro;

public interface RecorrenciaFinanceiraRepository extends JpaRepository<RecorrenciaFinanceira, UUID> {

	List<RecorrenciaFinanceira> findAllByEmpresaId(UUID empresaId);

	Optional<RecorrenciaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndStatus(UUID empresaId, StatusRecorrencia status);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndTipo(UUID empresaId, TipoFinanceiro tipo);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndPeriodicidade(UUID empresaId, PeriodicidadeRecorrencia periodicidade);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndPessoaFinanceiraId(UUID empresaId, UUID pessoaId);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndCategoriaId(UUID empresaId, UUID categoriaId);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndDescricaoContainingIgnoreCase(UUID empresaId, String descricao);

	List<RecorrenciaFinanceira> findAllByEmpresaIdAndStatusAndGerarAutomaticamenteTrueAndProximaCompetenciaLessThanEqual(
			UUID empresaId, StatusRecorrencia status, LocalDate competencia);

	boolean existsByContaIdAndEmpresaId(UUID contaId, UUID empresaId);

	boolean existsByCategoriaIdAndEmpresaId(UUID categoriaId, UUID empresaId);

	boolean existsByPessoaFinanceiraIdAndEmpresaId(UUID pessoaId, UUID empresaId);

	boolean existsByParteFinanceiraIdAndEmpresaId(UUID parteId, UUID empresaId);
}
