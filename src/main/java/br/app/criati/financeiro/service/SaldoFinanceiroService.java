package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.TipoFinanceiro;

/**
 * Fonte unica de verdade para o calculo de saldo: nunca persistido, sempre
 * derivado por consulta (saldoInicial + receitas PAGAS - despesas PAGAS).
 * Usado tanto pela tela de contas (saldo atual de uma conta) quanto pelo
 * dashboard (saldo consolidado de todas as contas), evitando duas formulas
 * divergentes para o mesmo numero.
 */
@Service
public class SaldoFinanceiroService {

	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;

	public SaldoFinanceiroService(LancamentoFinanceiroRepository lancamentoFinanceiroRepository) {
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
	}

	@Transactional(readOnly = true)
	public BigDecimal calcularSaldoAtual(ContaFinanceira conta) {
		List<LancamentoFinanceiro> lancamentos = lancamentoFinanceiroRepository
				.findAllByContaIdAndEmpresaId(conta.getId(), conta.getEmpresa().getId());
		return calcularSaldoAtual(conta, lancamentos);
	}

	BigDecimal calcularSaldoAtual(ContaFinanceira conta, List<LancamentoFinanceiro> lancamentosDaConta) {
		BigDecimal receitasPagas = somar(lancamentosDaConta, TipoFinanceiro.RECEITA);
		BigDecimal despesasPagas = somar(lancamentosDaConta, TipoFinanceiro.DESPESA);
		return conta.getSaldoInicial().add(receitasPagas).subtract(despesasPagas);
	}

	private BigDecimal somar(List<LancamentoFinanceiro> lancamentos, TipoFinanceiro tipo) {
		return lancamentos.stream()
				.filter(l -> l.compoeSaldoRealizado() && l.getTipo() == tipo)
				.map(LancamentoFinanceiro::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
