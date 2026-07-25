package br.app.criati.financeiro.repository;
import java.math.BigDecimal; import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
import br.app.criati.financeiro.model.CompraCartao; import br.app.criati.shared.enums.StatusCompraCartao;
public interface CompraCartaoRepository extends JpaRepository<CompraCartao,UUID>{
 List<CompraCartao> findAllByEmpresaIdOrderByDataCompraDescCriadoEmDesc(UUID empresaId);
 Optional<CompraCartao> findByIdAndEmpresaId(UUID id,UUID empresaId);
 @Query("select coalesce(sum(c.valorTotal),0) from CompraCartao c where c.empresa.id=:empresaId and c.cartaoPrincipal.id=:cartaoId and c.status=:status")
 BigDecimal somarComprometido(@Param("empresaId")UUID empresaId,@Param("cartaoId")UUID cartaoId,@Param("status")StatusCompraCartao status);
 // CRIATI-FIN-015: fatia de somarComprometido() que pertence a compras para terceiro
 // (parteFinanceira preenchida) — usada para separar, na resposta de resumo, quanto do
 // limite comprometido do cartao vem de compras da residencia vs. de terceiros. O
 // limite continua sendo consumido por igual (ver somarComprometido acima); esta query
 // so existe para exibir a quebra, nunca para alterar o calculo de limite disponivel.
 @Query("select coalesce(sum(c.valorTotal),0) from CompraCartao c where c.empresa.id=:empresaId and c.cartaoPrincipal.id=:cartaoId and c.status=:status and c.parteFinanceira is not null")
 BigDecimal somarComprometidoTerceiros(@Param("empresaId")UUID empresaId,@Param("cartaoId")UUID cartaoId,@Param("status")StatusCompraCartao status);
}
