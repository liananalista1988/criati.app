package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;

public interface EmprestimoConcedidoRepository extends JpaRepository<EmprestimoConcedido, UUID> {

	List<EmprestimoConcedido> findAllByEmpresaId(UUID empresaId);

	Optional<EmprestimoConcedido> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<EmprestimoConcedido> findAllByEmpresaIdAndStatus(UUID empresaId, StatusEmprestimoConcedido status);

	List<EmprestimoConcedido> findAllByEmpresaIdAndParteFinanceiraId(UUID empresaId, UUID parteFinanceiraId);

	boolean existsByCategoriaIdAndEmpresaId(UUID categoriaId, UUID empresaId);

	boolean existsByParteFinanceiraIdAndEmpresaId(UUID parteFinanceiraId, UUID empresaId);
}
