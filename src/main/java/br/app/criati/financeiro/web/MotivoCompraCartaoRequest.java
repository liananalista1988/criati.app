package br.app.criati.financeiro.web; import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.Size;
public record MotivoCompraCartaoRequest(@NotBlank @Size(max=500) String motivo){}
