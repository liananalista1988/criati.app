package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.LancamentoFinanceiro;

public interface LancamentoFinanceiroRepository extends JpaRepository<LancamentoFinanceiro, UUID> {

	List<LancamentoFinanceiro> findAllByEmpresaId(UUID empresaId);

	Optional<LancamentoFinanceiro> findByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByContaId(UUID contaId);

	boolean existsByCategoriaId(UUID categoriaId);

	List<LancamentoFinanceiro> findAllByContaId(UUID contaId);
}
