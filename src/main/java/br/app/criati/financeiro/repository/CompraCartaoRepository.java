package br.app.criati.financeiro.repository;
import java.util.List; import java.util.Optional; import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import br.app.criati.financeiro.model.CompraCartao;
public interface CompraCartaoRepository extends JpaRepository<CompraCartao,UUID>{
 List<CompraCartao> findAllByEmpresaIdOrderByDataCompraDescCriadoEmDesc(UUID empresaId);
 Optional<CompraCartao> findByIdAndEmpresaId(UUID id,UUID empresaId);
 // LES-F3-005: somarComprometido/somarComprometidoTerceiros foram substituidas
 // por ParcelaCompraCartaoRepository#somarNaoQuitado/somarNaoQuitadoTerceiros -
 // limite comprometido agora e calculado por parcela, considerando o status da
 // fatura (pagamento integral libera limite; a soma por compra inteira nao
 // conseguia expressar isso).
}
