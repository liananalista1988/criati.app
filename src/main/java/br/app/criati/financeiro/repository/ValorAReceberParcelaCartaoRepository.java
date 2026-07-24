package br.app.criati.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;

public interface ValorAReceberParcelaCartaoRepository extends JpaRepository<ValorAReceberParcelaCartao, UUID> {

	List<ValorAReceberParcelaCartao> findAllByEmpresaId(UUID empresaId);

	Optional<ValorAReceberParcelaCartao> findByIdAndEmpresaId(UUID id, UUID empresaId);

	Optional<ValorAReceberParcelaCartao> findByParcelaIdAndEmpresaId(UUID parcelaId, UUID empresaId);

	List<ValorAReceberParcelaCartao> findAllByEmpresaIdAndParcelaCompraId(UUID empresaId, UUID compraId);

	List<ValorAReceberParcelaCartao> findAllByEmpresaIdAndStatus(UUID empresaId, StatusValorAReceberCompraCartao status);

	List<ValorAReceberParcelaCartao> findAllByEmpresaIdAndVencimentoBetween(UUID empresaId, LocalDate inicio, LocalDate fim);

	boolean existsByParcelaCompraIdAndEmpresaId(UUID compraId, UUID empresaId);

	/**
	 * Mesma busca de findByIdAndEmpresaId, mas com PESSIMISTIC_WRITE: usada
	 * exclusivamente pelo fluxo de ressarcimento (RessarcimentoParcelaCartaoService)
	 * para serializar leituras concorrentes do saldo pendente antes de gravar um
	 * novo ressarcimento — mesmo padrao ja usado por
	 * ParcelaEmprestimoRepository.findForUpdateByIdAndEmpresaId e por
	 * CartaoCreditoRepository.findForUpdateByIdAndEmpresaId.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<ValorAReceberParcelaCartao> findForUpdateByIdAndEmpresaId(UUID id, UUID empresaId);
}
