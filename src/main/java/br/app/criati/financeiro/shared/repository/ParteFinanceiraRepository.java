package br.app.criati.financeiro.shared.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public interface ParteFinanceiraRepository extends JpaRepository<ParteFinanceira, UUID> {

	List<ParteFinanceira> findAllByEmpresaIdOrderByNomeAsc(UUID empresaId);

	List<ParteFinanceira> findAllByEmpresaIdAndStatusOrderByNomeAsc(UUID empresaId, StatusCadastro status);

	List<ParteFinanceira> findAllByEmpresaIdAndStatusAndNomeContainingIgnoreCaseOrderByNomeAsc(
			UUID empresaId, StatusCadastro status, String nome);

	Optional<ParteFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);
}
