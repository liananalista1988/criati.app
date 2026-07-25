package br.app.criati.financeiro.service;
import java.math.BigDecimal; import java.util.List; import java.util.UUID;
// CRIATI-FIN-015: totalResidencia/totalTerceiros sao a quebra explicita de totalComprado
// (totalResidencia + totalTerceiros == totalComprado sempre) entre compras da residencia
// (parteFinanceira nula) e compras para terceiro (parteFinanceira preenchida) — nenhuma das
// duas e removida do total geral nem do limite comprometido, que continuam somando ambas.
public record ResumoComprasCartao(BigDecimal totalComprado,BigDecimal totalResidencia,BigDecimal totalTerceiros,BigDecimal limiteTotalConsolidado,BigDecimal limiteComprometido,BigDecimal limiteDisponivel,long comprasAtivas,long comprasParceladas,long comprasCanceladas,List<ValorPorPessoa> porTitular,List<ValorPorCartao> porCartao){
 public record ValorPorPessoa(UUID pessoaId,String nome,BigDecimal valor){} public record ValorPorCartao(UUID cartaoId,String nome,BigDecimal valor){}
}
