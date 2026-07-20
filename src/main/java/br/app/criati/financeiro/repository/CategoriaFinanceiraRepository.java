package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, UUID> {

	List<CategoriaFinanceira> findAllByEmpresaId(UUID empresaId);

	Optional<CategoriaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByEmpresaIdAndTipoAndNomeIgnoreCaseAndStatus(
			UUID empresaId, TipoFinanceiro tipo, String nome, StatusCadastro status);
}
