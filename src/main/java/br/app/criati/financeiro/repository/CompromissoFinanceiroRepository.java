package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.CompromissoFinanceiro;

public interface CompromissoFinanceiroRepository extends JpaRepository<CompromissoFinanceiro, UUID> {

	List<CompromissoFinanceiro> findAllByEmpresaId(UUID empresaId);

	List<CompromissoFinanceiro> findAllByEmpresaIdAndAtivoTrue(UUID empresaId);

	Optional<CompromissoFinanceiro> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<CompromissoFinanceiro> findAllByEmpresaIdAndPessoaFinanceiraId(UUID empresaId, UUID pessoaId);

	List<CompromissoFinanceiro> findAllByEmpresaIdAndCategoriaId(UUID empresaId, UUID categoriaId);

	List<CompromissoFinanceiro> findAllByEmpresaIdAndDescricaoContainingIgnoreCase(UUID empresaId, String descricao);

	Optional<CompromissoFinanceiro> findByRecorrenciaIdAndEmpresaId(UUID recorrenciaId, UUID empresaId);

	boolean existsByRecorrenciaId(UUID recorrenciaId);

	boolean existsByCategoriaIdAndEmpresaId(UUID categoriaId, UUID empresaId);

	boolean existsByPessoaFinanceiraIdAndEmpresaId(UUID pessoaId, UUID empresaId);

	boolean existsByParteFinanceiraIdAndEmpresaId(UUID parteId, UUID empresaId);

	boolean existsByContaPadraoIdAndEmpresaId(UUID contaId, UUID empresaId);
}
