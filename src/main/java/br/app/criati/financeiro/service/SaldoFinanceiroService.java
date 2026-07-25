package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.RessarcimentoParcelaCartao;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.RessarcimentoParcelaCartaoRepository;
import br.app.criati.shared.enums.TipoFinanceiro;

/**
 * Fonte unica de verdade para o calculo de saldo: nunca persistido, sempre
 * derivado por consulta (saldoInicial + receitas PAGAS - despesas PAGAS +
 * ressarcimentos ATIVOs de compras para terceiros). Usado tanto pela tela de
 * contas (saldo atual de uma conta) quanto pelo dashboard (saldo consolidado
 * de todas as contas), evitando duas formulas divergentes para o mesmo
 * numero.
 *
 * Os ressarcimentos de compras para terceiros (CRIATI-FIN-013A) entram no
 * saldo diretamente, fora da soma de receitas/despesas: o principal
 * ressarcido nao e receita nem despesa da residencia — e apenas dinheiro
 * entrando de fato na conta — por isso nunca gera LancamentoFinanceiro. Sem
 * esse termo, o saldo da conta ficaria invisivel ao dinheiro realmente
 * recebido.
 */
@Service
public class SaldoFinanceiroService {

	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
	private final RessarcimentoParcelaCartaoRepository ressarcimentoParcelaCartaoRepository;

	public SaldoFinanceiroService(LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
			RessarcimentoParcelaCartaoRepository ressarcimentoParcelaCartaoRepository) {
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
		this.ressarcimentoParcelaCartaoRepository = ressarcimentoParcelaCartaoRepository;
	}

	@Transactional(readOnly = true)
	public BigDecimal calcularSaldoAtual(ContaFinanceira conta) {
		List<LancamentoFinanceiro> lancamentos = lancamentoFinanceiroRepository
				.findAllByContaIdAndEmpresaId(conta.getId(), conta.getEmpresa().getId());
		List<RessarcimentoParcelaCartao> ressarcimentos = ressarcimentoParcelaCartaoRepository
				.findAllByEmpresaIdAndContaId(conta.getEmpresa().getId(), conta.getId());
		return calcularSaldoAtual(conta, lancamentos, ressarcimentos);
	}

	BigDecimal calcularSaldoAtual(ContaFinanceira conta, List<LancamentoFinanceiro> lancamentosDaConta,
			List<RessarcimentoParcelaCartao> ressarcimentosDaConta) {
		BigDecimal receitasPagas = somar(lancamentosDaConta, TipoFinanceiro.RECEITA);
		BigDecimal despesasPagas = somar(lancamentosDaConta, TipoFinanceiro.DESPESA);
		BigDecimal ressarcimentosAtivos = somarRessarcimentosAtivos(ressarcimentosDaConta);
		return conta.getSaldoInicial().add(receitasPagas).subtract(despesasPagas).add(ressarcimentosAtivos);
	}

	private BigDecimal somar(List<LancamentoFinanceiro> lancamentos, TipoFinanceiro tipo) {
		return lancamentos.stream()
				.filter(l -> l.compoeSaldoRealizado() && l.getTipo() == tipo)
				.map(LancamentoFinanceiro::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal somarRessarcimentosAtivos(List<RessarcimentoParcelaCartao> ressarcimentos) {
		return ressarcimentos.stream()
				.filter(RessarcimentoParcelaCartao::estaAtivo)
				.map(RessarcimentoParcelaCartao::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
