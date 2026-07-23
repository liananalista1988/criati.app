package br.app.criati.financeiro.web;
import java.math.BigDecimal; import java.util.List; import java.util.UUID; import br.app.criati.financeiro.service.ResumoComprasCartao;
public record ResumoComprasCartaoResponse(BigDecimal totalComprado,BigDecimal limiteTotalConsolidado,BigDecimal limiteComprometido,BigDecimal limiteDisponivel,long comprasAtivas,long comprasParceladas,long comprasCanceladas,List<ValorPorPessoa> porTitular,List<ValorPorCartao> porCartao){
 public static ResumoComprasCartaoResponse from(ResumoComprasCartao r){return new ResumoComprasCartaoResponse(r.totalComprado(),r.limiteTotalConsolidado(),r.limiteComprometido(),r.limiteDisponivel(),r.comprasAtivas(),r.comprasParceladas(),r.comprasCanceladas(),r.porTitular().stream().map(x->new ValorPorPessoa(x.pessoaId(),x.nome(),x.valor())).toList(),r.porCartao().stream().map(x->new ValorPorCartao(x.cartaoId(),x.nome(),x.valor())).toList());}
 public record ValorPorPessoa(UUID pessoaId,String nome,BigDecimal valor){} public record ValorPorCartao(UUID cartaoId,String nome,BigDecimal valor){}
}
