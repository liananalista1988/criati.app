package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.RessarcimentoParcelaCartao;

public interface RessarcimentoParcelaCartaoRepository extends JpaRepository<RessarcimentoParcelaCartao, UUID> {

	List<RessarcimentoParcelaCartao> findAllByEmpresaIdAndValorAReceberIdOrderByDataRessarcimentoDesc(UUID empresaId,
			UUID valorAReceberId);

	Optional<RessarcimentoParcelaCartao> findByIdAndValorAReceberIdAndEmpresaId(UUID id, UUID valorAReceberId,
			UUID empresaId);

	boolean existsByLancamentoFinanceiroId(UUID lancamentoFinanceiroId);
}
