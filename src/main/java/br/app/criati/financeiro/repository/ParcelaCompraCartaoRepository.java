package br.app.criati.financeiro.repository;
import java.time.LocalDate; import java.util.List; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
public interface ParcelaCompraCartaoRepository extends JpaRepository<ParcelaCompraCartao,UUID>{
 List<ParcelaCompraCartao> findAllByCompraIdAndEmpresaIdOrderByNumero(UUID compraId,UUID empresaId);
 List<ParcelaCompraCartao> findAllByEmpresaIdAndCompetencia(UUID empresaId,LocalDate competencia);
}
