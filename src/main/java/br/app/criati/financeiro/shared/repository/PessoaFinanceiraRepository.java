package br.app.criati.financeiro.shared.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public interface PessoaFinanceiraRepository extends JpaRepository<PessoaFinanceira, UUID> {

	List<PessoaFinanceira> findAllByEmpresaIdOrderByNomeAsc(UUID empresaId);

	List<PessoaFinanceira> findAllByEmpresaIdAndStatusOrderByNomeAsc(UUID empresaId, StatusCadastro status);

	Optional<PessoaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByEmpresaIdAndUsuarioIdAndStatus(UUID empresaId, UUID usuarioId, StatusCadastro status);

	boolean existsByEmpresaIdAndUsuarioIdAndStatusAndIdNot(
			UUID empresaId, UUID usuarioId, StatusCadastro status, UUID id);
}
