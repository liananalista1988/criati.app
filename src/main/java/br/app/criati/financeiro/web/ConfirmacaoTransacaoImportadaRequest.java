package br.app.criati.financeiro.web;

import java.util.UUID;

import br.app.criati.financeiro.service.ConfirmacaoTransacaoImportada;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfirmacaoTransacaoImportadaRequest(
		@NotNull UUID transacaoId,
		// Exatamente um entre categoriaId, faturaId e parcelaEmprestimoId deve
		// ser informado - validado em ConfirmacaoImportacaoBancariaService, nao
		// aqui, porque a obrigatoriedade e condicional (nao expressavel com
		// anotacoes simples).
		UUID categoriaId,
		UUID faturaId,
		UUID parcelaEmprestimoId,
		UUID regraClassificacaoId,
		@NotBlank @Size(max = 200) String descricaoFinal,
		boolean confirmarDuplicidade) {

	ConfirmacaoTransacaoImportada toCommand() {
		return new ConfirmacaoTransacaoImportada(transacaoId, categoriaId, faturaId, parcelaEmprestimoId,
				regraClassificacaoId, descricaoFinal, confirmarDuplicidade);
	}

	@Override
	public String toString() {
		return "ConfirmacaoTransacaoImportadaRequest[transacaoId=" + transacaoId
				+ ", categoriaId=" + categoriaId + ", descricaoFinal=<redigida>, confirmarDuplicidade="
				+ confirmarDuplicidade + "]";
	}
}
