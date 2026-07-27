package br.app.criati.financeiro.repository;
import java.time.LocalDate; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
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
}
