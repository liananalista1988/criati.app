package br.app.criati.financeiro.web;

import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.LoteImportacaoBancaria;
import br.app.criati.shared.enums.FormatoArquivoImportacao;
import br.app.criati.shared.enums.StatusLoteImportacao;

public record LoteImportacaoBancariaResponse(
		UUID id,
		UUID contaId,
		String contaNome,
		UUID contaSugeridaId,
		String contaSugeridaNome,
		// Nunca expoe banco/agencia/numero/tipo brutos do OFX (CRIATI-IMP-FIX-011)
		// - sao metadados internos usados so pela autodetecao no backend; a tela
		// de revisao so precisa saber SE existe algum dado bancario no arquivo
		// para escolher a mensagem certa, nunca do valor em si. Nem ADMINISTRADOR
		// precisa deles aqui: diferente de ContaFinanceiraResponse (onde
		// ADMINISTRADOR edita a conta e por isso ve os dados dela), este campo e
		// so o extrato bruto do arquivo importado, sem uso legitimo na tela.
		boolean identificacaoBancariaPresente,
		String hashArquivo,
		FormatoArquivoImportacao formato,
		String nomeOriginal,
		long tamanhoBytes,
		StatusLoteImportacao status,
		int quantidadeTransacoes,
		int quantidadeDuplicadasArquivo,
		int quantidadePossiveisDuplicadas,
		OffsetDateTime criadoEm,
		UUID criadoPorUsuarioId,
		OffsetDateTime descartadoEm,
		UUID descartadoPorUsuarioId) {

	public static LoteImportacaoBancariaResponse from(LoteImportacaoBancaria lote) {
		boolean identificacaoPresente = lote.getIdentificacaoBancoId() != null
				|| lote.getIdentificacaoAgencia() != null
				|| lote.getIdentificacaoNumeroConta() != null
				|| lote.getIdentificacaoTipoConta() != null;
		return new LoteImportacaoBancariaResponse(lote.getId(),
				lote.getConta() == null ? null : lote.getConta().getId(),
				lote.getConta() == null ? null : lote.getConta().getNome(),
				lote.getContaSugerida() == null ? null : lote.getContaSugerida().getId(),
				lote.getContaSugerida() == null ? null : lote.getContaSugerida().getNome(),
				identificacaoPresente,
				lote.getHashArquivo(), lote.getFormato(), lote.getNomeOriginal(), lote.getTamanhoBytes(), lote.getStatus(),
				lote.getQuantidadeTransacoes(), lote.getQuantidadeDuplicadasArquivo(),
				lote.getQuantidadePossiveisDuplicadas(), lote.getCriadoEm(), lote.getCriadoPor().getId(),
				lote.getDescartadoEm(), lote.getDescartadoPor() == null ? null : lote.getDescartadoPor().getId());
	}

	@Override
	public String toString() {
		return "LoteImportacaoBancariaResponse[id=" + id + ", status=" + status
				+ ", quantidadeTransacoes=" + quantidadeTransacoes + "]";
	}
}
