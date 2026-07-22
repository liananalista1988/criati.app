package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public interface ContaFinanceiraRepository extends JpaRepository<ContaFinanceira, UUID> {

	List<ContaFinanceira> findAllByEmpresaId(UUID empresaId);

	List<ContaFinanceira> findAllByEmpresaIdAndStatusOrderByNomeAsc(UUID empresaId, StatusCadastro status);

	List<ContaFinanceira> findAllByEmpresaIdAndTitularIdOrderByNomeAsc(UUID empresaId, UUID titularId);

	List<ContaFinanceira> findAllByEmpresaIdAndInstituicaoIdOrderByNomeAsc(UUID empresaId, UUID instituicaoId);

	List<ContaFinanceira> findAllByEmpresaIdAndNomeContainingIgnoreCaseOrderByNomeAsc(UUID empresaId, String nome);

	List<ContaFinanceira> findAllByEmpresaIdAndPermiteConciliacaoTrueAndStatusOrderByNomeAsc(
			UUID empresaId, StatusCadastro status);

	Optional<ContaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByEmpresaIdAndNomeIgnoreCaseAndStatus(UUID empresaId, String nome, StatusCadastro status);

	boolean existsByEmpresaIdAndNomeIgnoreCaseAndStatusAndIdNot(
			UUID empresaId, String nome, StatusCadastro status, UUID id);

	long countByEmpresaIdAndStatus(UUID empresaId, StatusCadastro status);
}
