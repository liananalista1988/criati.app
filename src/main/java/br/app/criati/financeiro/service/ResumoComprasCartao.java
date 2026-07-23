package br.app.criati.financeiro.service;
import java.math.BigDecimal; import java.util.List; import java.util.UUID;
public record ResumoComprasCartao(BigDecimal totalComprado,BigDecimal limiteTotalConsolidado,BigDecimal limiteComprometido,BigDecimal limiteDisponivel,long comprasAtivas,long comprasParceladas,long comprasCanceladas,List<ValorPorPessoa> porTitular,List<ValorPorCartao> porCartao){
 public record ValorPorPessoa(UUID pessoaId,String nome,BigDecimal valor){} public record ValorPorCartao(UUID cartaoId,String nome,BigDecimal valor){}
}
