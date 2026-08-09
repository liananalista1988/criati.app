package br.app.criati.financeiro.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;

public record ContaFinanceiraResponse(
		UUID id,
		String nome,
		UUID titularId,
		String titularNome,
		UUID instituicaoId,
		String instituicaoNome,
		TipoContaFinanceira tipo,
		String moeda,
		BigDecimal saldoInicial,
		LocalDate dataSaldoInicial,
		boolean permiteConciliacao,
		String agenciaBancaria,
		String numeroContaBancaria,
		String digitoContaBancaria,
		BigDecimal saldoAtual,
		StatusCadastro status,
		boolean possivelDuplicidade,
		OffsetDateTime criadoEm,
		OffsetDateTime atualizadoEm) {

	// identificacaoBancariaCompleta: agencia/numero/digito completos so vao
	// para quem pode editar a conta (ADMINISTRADOR); os demais perfis, com
	// acesso apenas de leitura, recebem os valores mascarados - eles nunca
	// precisam do dado bruto para nada na tela, so para reconhecer a conta
	// (CRIATI-IMP-FIX-007, item 7). Mascaramento nao afeta a autodetecao
	// interna, que le os campos da entidade diretamente, nunca deste DTO.
	public static ContaFinanceiraResponse from(
			ContaFinanceira conta, BigDecimal saldoAtual, boolean possivelDuplicidade,
			boolean identificacaoBancariaCompleta) {
		return new ContaFinanceiraResponse(
				conta.getId(),
				conta.getNome(),
				conta.getTitular() == null ? null : conta.getTitular().getId(),
				conta.getTitular() == null ? null : conta.getTitular().getNome(),
				conta.getInstituicao() == null ? null : conta.getInstituicao().getId(),
				conta.getInstituicao() == null ? null : conta.getInstituicao().getNome(),
				conta.getTipo(),
				conta.getMoeda(),
				conta.getSaldoInicial(),
				conta.getDataSaldoInicial(),
				conta.isPermiteConciliacao(),
				identificador(conta.getAgenciaBancaria(), identificacaoBancariaCompleta),
				identificador(conta.getNumeroContaBancaria(), identificacaoBancariaCompleta),
				identificador(conta.getDigitoContaBancaria(), identificacaoBancariaCompleta),
				saldoAtual,
				conta.getStatus(),
				possivelDuplicidade,
				conta.getCriadoEm(),
				conta.getAtualizadoEm());
	}

	private static String identificador(String valor, boolean completo) {
		if (valor == null || completo) {
			return valor;
		}
		if (valor.length() <= 4) {
			return "•".repeat(valor.length());
		}
		return "•".repeat(valor.length() - 4) + valor.substring(valor.length() - 4);
	}
}
