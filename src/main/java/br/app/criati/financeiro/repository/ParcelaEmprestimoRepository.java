package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;

public interface ParcelaEmprestimoRepository extends JpaRepository<ParcelaEmprestimo, UUID> {

	List<ParcelaEmprestimo> findAllByEmpresaId(UUID empresaId);

	Optional<ParcelaEmprestimo> findByIdAndEmpresaId(UUID id, UUID empresaId);

	/**
	 * Mesma busca de findByIdAndEmpresaId, mas com PESSIMISTIC_WRITE: usada
	 * exclusivamente pelo fluxo de recebimento (RecebimentoParcelaEmprestimoService)
	 * para serializar leituras concorrentes do saldo pendente antes de gravar um
	 * novo recebimento, evitando que duas requisicoes simultaneas leiam o mesmo
	 * saldo ainda nao commitado e gerem dois recebimentos/lancamentos para o
	 * mesmo dinheiro. Mesmo padrao ja usado por
	 * CartaoCreditoRepository.findForUpdateByIdAndEmpresaId.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ParcelaEmprestimo> findForUpdateByIdAndEmpresaId(UUID id, UUID empresaId);

	List<ParcelaEmprestimo> findAllByEmpresaIdAndEmprestimoIdOrderByNumero(UUID empresaId, UUID emprestimoId);

	List<ParcelaEmprestimo> findAllByEmpresaIdAndStatus(UUID empresaId, StatusParcelaEmprestimo status);
}
