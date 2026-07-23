package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.RecebimentoParcelaEmprestimo;

public interface RecebimentoParcelaEmprestimoRepository extends JpaRepository<RecebimentoParcelaEmprestimo, UUID> {

	List<RecebimentoParcelaEmprestimo> findAllByEmpresaIdAndParcelaIdOrderByDataRecebimentoDesc(UUID empresaId,
			UUID parcelaId);

	Optional<RecebimentoParcelaEmprestimo> findByIdAndParcelaIdAndEmpresaId(UUID id, UUID parcelaId, UUID empresaId);

	boolean existsByLancamentoFinanceiroId(UUID lancamentoFinanceiroId);
}
