package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.RessarcimentoParcelaCartao;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.RessarcimentoParcelaCartaoRepository;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;

@Service
public class DashboardFinanceiroService {

	private static final int LIMITE_ULTIMOS_LANCAMENTOS = 10;

	private final ContaFinanceiraRepository contaFinanceiraRepository;
	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
	private final RessarcimentoParcelaCartaoRepository ressarcimentoParcelaCartaoRepository;
	private final SaldoFinanceiroService saldoFinanceiroService;
	private final ValorAReceberParcelaCartaoService valorAReceberParcelaCartaoService;

	public DashboardFinanceiroService(
			ContaFinanceiraRepository contaFinanceiraRepository,
			LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
			RessarcimentoParcelaCartaoRepository ressarcimentoParcelaCartaoRepository,
			SaldoFinanceiroService saldoFinanceiroService,
			ValorAReceberParcelaCartaoService valorAReceberParcelaCartaoService) {
		this.contaFinanceiraRepository = contaFinanceiraRepository;
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
		this.ressarcimentoParcelaCartaoRepository = ressarcimentoParcelaCartaoRepository;
		this.saldoFinanceiroService = saldoFinanceiroService;
		this.valorAReceberParcelaCartaoService = valorAReceberParcelaCartaoService;
	}

	@Transactional(readOnly = true)
	public DashboardFinanceiro gerar(ContextoEmpresaAtual contexto, YearMonth competencia) {
		YearMonth competenciaEfetiva = competencia != null ? competencia : YearMonth.now();
		UUID empresaId = contexto.empresaId();

		List<ContaFinanceira> contas = contaFinanceiraRepository.findAllByEmpresaId(empresaId);
		List<LancamentoFinanceiro> lancamentos = lancamentoFinanceiroRepository.findAllByEmpresaId(empresaId);

		List<LancamentoFinanceiro> doMes = lancamentos.stream()
				.filter(l -> YearMonth.from(l.getDataCompetencia()).equals(competenciaEfetiva))
				.toList();

		BigDecimal saldoInicialConsolidado = contas.stream()
				.map(ContaFinanceira::getSaldoInicial)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal receitasPagas = somarLiquidados(doMes, TipoFinanceiro.RECEITA);
		BigDecimal despesasPagas = somarLiquidados(doMes, TipoFinanceiro.DESPESA);
		BigDecimal resultadoMes = receitasPagas.subtract(despesasPagas);

		// Pendentes representam o que falta receber/pagar no total (nao
		// restritos a competencia selecionada): um titulo pendente de um mes
		// anterior ainda esta em aberto e deve aparecer aqui.
		BigDecimal totalPendenteReceber = somar(lancamentos, TipoFinanceiro.RECEITA, StatusLancamentoFinanceiro.PENDENTE);
		BigDecimal totalPendentePagar = somar(lancamentos, TipoFinanceiro.DESPESA, StatusLancamentoFinanceiro.PENDENTE);

		// CRIATI-FIN-015: indicador separado, nunca somado a totalPendenteReceber
		// (que so soma LancamentoFinanceiro) — reaproveita o resumo ja isolado de
		// ValorAReceberParcelaCartaoService, mesma fonte usada pela tela dedicada
		// de compras para terceiros, evitando calcular esse valor duas vezes.
		BigDecimal totalPendenteReceberTerceiros = valorAReceberParcelaCartaoService.resumir(contexto).saldoAReceber();

		Map<UUID, List<LancamentoFinanceiro>> porConta = new LinkedHashMap<>();
		for (LancamentoFinanceiro lancamento : lancamentos) {
			porConta.computeIfAbsent(lancamento.getConta().getId(), k -> new java.util.ArrayList<>()).add(lancamento);
		}
		List<RessarcimentoParcelaCartao> ressarcimentos = ressarcimentoParcelaCartaoRepository
				.findAllByEmpresaId(empresaId);
		Map<UUID, List<RessarcimentoParcelaCartao>> ressarcimentosPorConta = new LinkedHashMap<>();
		for (RessarcimentoParcelaCartao ressarcimento : ressarcimentos) {
			ressarcimentosPorConta.computeIfAbsent(ressarcimento.getConta().getId(), k -> new java.util.ArrayList<>())
					.add(ressarcimento);
		}
		BigDecimal saldoAtualConsolidado = contas.stream()
				.map(conta -> saldoFinanceiroService.calcularSaldoAtual(conta,
						porConta.getOrDefault(conta.getId(), List.of()),
						ressarcimentosPorConta.getOrDefault(conta.getId(), List.of())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		long quantidadeContasAtivas = contas.stream().filter(ContaFinanceira::estaAtiva).count();
		long quantidadeLancamentosPeriodo = doMes.stream()
				.filter(l -> l.getStatus() != StatusLancamentoFinanceiro.CANCELADO)
				.count();

		List<ResumoCategoriaFinanceira> resumoPorCategoria = resumoPorCategoria(doMes);

		List<LancamentoFinanceiro> ultimosLancamentos = lancamentos.stream()
				.sorted(Comparator.comparing(LancamentoFinanceiro::getCriadoEm).reversed())
				.limit(LIMITE_ULTIMOS_LANCAMENTOS)
				.toList();

		return new DashboardFinanceiro(
				competenciaEfetiva,
				saldoInicialConsolidado,
				receitasPagas,
				despesasPagas,
				resultadoMes,
				totalPendenteReceber,
				totalPendentePagar,
				totalPendenteReceberTerceiros,
				saldoAtualConsolidado,
				quantidadeContasAtivas,
				quantidadeLancamentosPeriodo,
				resumoPorCategoria,
				ultimosLancamentos);
	}

	private BigDecimal somar(
			List<LancamentoFinanceiro> lancamentos, TipoFinanceiro tipo, StatusLancamentoFinanceiro status) {
		return lancamentos.stream()
				.filter(l -> l.getTipo() == tipo && l.getStatus() == status)
				.map(LancamentoFinanceiro::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private List<ResumoCategoriaFinanceira> resumoPorCategoria(List<LancamentoFinanceiro> doMes) {
		Map<UUID, ResumoAcumulado> acumulado = new LinkedHashMap<>();
		for (LancamentoFinanceiro lancamento : doMes) {
			if (!lancamento.compoeSaldoRealizado()) {
				continue;
			}
			CategoriaFinanceira categoria = lancamento.getCategoria();
			ResumoAcumulado atual = acumulado.computeIfAbsent(
					categoria.getId(), k -> new ResumoAcumulado(categoria.getNome(), categoria.getTipo()));
			atual.total = atual.total.add(lancamento.getValor());
		}
		return acumulado.entrySet().stream()
				.map(entry -> new ResumoCategoriaFinanceira(
						entry.getKey(), entry.getValue().nome, entry.getValue().tipo, entry.getValue().total))
				.toList();
	}

	private BigDecimal somarLiquidados(List<LancamentoFinanceiro> lancamentos, TipoFinanceiro tipo) {
		return lancamentos.stream().filter(l -> l.getTipo() == tipo && l.compoeSaldoRealizado())
				.map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static final class ResumoAcumulado {
		private final String nome;
		private final TipoFinanceiro tipo;
		private BigDecimal total = BigDecimal.ZERO;

		private ResumoAcumulado(String nome, TipoFinanceiro tipo) {
			this.nome = nome;
			this.tipo = tipo;
		}
	}
}
