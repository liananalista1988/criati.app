package br.app.criati.financeiro.web;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.UUID;
import jakarta.validation.constraints.*;
public record CompraCartaoRequest(@NotNull UUID cartaoId,@NotNull UUID pessoaResponsavelId,@NotNull UUID categoriaId,UUID parteFinanceiraId,@NotBlank @Size(max=200) String descricao,@NotNull LocalDate dataCompra,@NotNull @DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal valorTotal,@NotNull @Min(1) Integer quantidadeParcelas,@Size(max=500) String observacao){}
