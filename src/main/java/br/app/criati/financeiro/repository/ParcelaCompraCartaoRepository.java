package br.app.criati.financeiro.repository;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.shared.enums.StatusCompraCartao;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.StatusParcelaCartao;
import jakarta.persistence.LockModeType;
public interface ParcelaCompraCartaoRepository extends JpaRepository<ParcelaCompraCartao,UUID>{
 List<ParcelaCompraCartao> findAllByCompraIdAndEmpresaIdOrderByNumero(UUID compraId,UUID empresaId);
 List<ParcelaCompraCartao> findAllByEmpresaIdAndCompetencia(UUID empresaId,LocalDate competencia);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("""
   select p from ParcelaCompraCartao p
   join fetch p.compra c
   where p.empresa.id = :empresaId
     and c.cartaoPrincipal.id = :cartaoPrincipalId
     and p.competencia = :competencia
     and p.status = :status
     and (p.faturaId is null or p.faturaId = :faturaId)
   order by p.id
   """)
 List<ParcelaCompraCartao> buscarElegiveisParaFatura(
   @Param("empresaId") UUID empresaId,
   @Param("cartaoPrincipalId") UUID cartaoPrincipalId,
   @Param("competencia") LocalDate competencia,
   @Param("status") StatusParcelaCartao status,
   @Param("faturaId") UUID faturaId);
 List<ParcelaCompraCartao> findAllByEmpresaIdAndFaturaIdOrderById(UUID empresaId,UUID faturaId);

	// LES-F3-005: substitui CompraCartaoRepository#somarComprometido como fonte
	// do limite comprometido - soma por PARCELA (nao mais por compra inteira),
	// excluindo apenas as parcelas cuja fatura ja esta PAGA (pagamento integral
	// libera o limite; parcial/minimo nao libera nada, mesmo que a fatura ainda
	// nao tenha sido totalmente quitada). Parcela sem fatura (faturaId nulo)
	// continua sempre comprometida.
	@Query("""
			select coalesce(sum(p.valor), 0) from ParcelaCompraCartao p
			join p.compra c
			where p.empresa.id = :empresaId
			  and c.cartaoPrincipal.id = :cartaoPrincipalId
			  and c.status = :statusCompra
			  and not exists (
			      select 1 from FaturaCartao f
			      where f.id = p.faturaId and f.status = :statusFaturaPaga
			  )
			""")
	BigDecimal somarNaoQuitado(
			@Param("empresaId") UUID empresaId,
			@Param("cartaoPrincipalId") UUID cartaoPrincipalId,
			@Param("statusCompra") StatusCompraCartao statusCompra,
			@Param("statusFaturaPaga") StatusFaturaCartao statusFaturaPaga);

	// CRIATI-FIN-015: mesma quebra informativa que somarComprometidoTerceiros
	// tinha, agora recalculada na mesma granularidade de somarNaoQuitado acima.
	@Query("""
			select coalesce(sum(p.valor), 0) from ParcelaCompraCartao p
			join p.compra c
			where p.empresa.id = :empresaId
			  and c.cartaoPrincipal.id = :cartaoPrincipalId
			  and c.status = :statusCompra
			  and c.parteFinanceira is not null
			  and not exists (
			      select 1 from FaturaCartao f
			      where f.id = p.faturaId and f.status = :statusFaturaPaga
			  )
			""")
	BigDecimal somarNaoQuitadoTerceiros(
			@Param("empresaId") UUID empresaId,
			@Param("cartaoPrincipalId") UUID cartaoPrincipalId,
			@Param("statusCompra") StatusCompraCartao statusCompra,
			@Param("statusFaturaPaga") StatusFaturaCartao statusFaturaPaga);
}
