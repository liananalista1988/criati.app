package br.app.criati.financeiro.web;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
import jakarta.validation.constraints.*;
// parteFinanceiraId nao e aceito aqui de proposito (CRIATI-FIN-014): o endpoint generico de
// compras nunca cria ValorAReceberParcelaCartao, entao aceitar esse campo geraria uma compra
// marcada como "para terceiro" sem nenhum jeito de ser ressarcida. Compra para terceiro so pode
// ser criada por POST /api/contexto/financeiro/compras-terceiros (CompraTerceiroController).
public record CompraCartaoRequest(@NotNull UUID cartaoId,@NotNull UUID pessoaResponsavelId,@NotNull UUID categoriaId,@Null(message="parteFinanceiraId nao e aceito neste endpoint; utilize /api/contexto/financeiro/compras-terceiros para compras destinadas a terceiros") UUID parteFinanceiraId,@NotBlank @Size(max=200) String descricao,@NotNull LocalDate dataCompra,@NotNull @DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal valorTotal,@NotNull @Min(1) Integer quantidadeParcelas,@Size(max=500) String observacao){}
