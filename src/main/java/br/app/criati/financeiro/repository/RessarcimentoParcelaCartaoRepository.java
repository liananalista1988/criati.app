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

	/**
	 * Usado por SaldoFinanceiroService para reconhecer, sem passar por
	 * LancamentoFinanceiro, o impacto de caixa dos ressarcimentos (ATIVOs) de
	 * compras para terceiros sobre o saldo de uma conta especifica — ver
	 * CRIATI-FIN-013A.
	 */
	List<RessarcimentoParcelaCartao> findAllByEmpresaIdAndContaId(UUID empresaId, UUID contaId);

	/**
	 * Variante em lote de {@link #findAllByEmpresaIdAndContaId}, usada por
	 * DashboardFinanceiroService para computar o saldo consolidado de todas as
	 * contas com uma unica consulta (evita N+1 por conta).
	 */
	List<RessarcimentoParcelaCartao> findAllByEmpresaId(UUID empresaId);
}
