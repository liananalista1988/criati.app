/* Criati Financeiro - dashboard: cards reais, resumo por categoria (barras CSS,
   sem biblioteca externa) e ultimos lancamentos. Consome exclusivamente
   GET /api/contexto/financeiro/dashboard. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = {
		PENDENTE: "Pendente",
		LIQUIDADO: "Liquidado",
		PAGO: "Pago",
		CANCELADO: "Cancelado"
	};

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var seletor = el("financeiro-dashboard-competencia");
		if (!seletor) {
			return;
		}
		if (seletor.dataset.dashboardInicializado === "true") {
			return;
		}
		seletor.dataset.dashboardInicializado = "true";
		seletor.value = window.FinanceiroFormatacao.competenciaAtual();
		seletor.addEventListener("change", carregar);
		carregar();
	}

	function carregar() {
		var carregando = el("financeiro-dashboard-carregando");
		var erro = el("financeiro-dashboard-erro");
		var conteudo = el("financeiro-dashboard-conteudo");

		carregando.hidden = false;
		erro.hidden = true;
		conteudo.hidden = true;

		var competencia = el("financeiro-dashboard-competencia").value;

		window.FinanceiroApi
			.dashboard(competencia)
			.then(function (resposta) {
				carregando.hidden = true;
				conteudo.hidden = false;
				render(resposta.data);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function render(dados) {
		renderValor("financeiro-card-saldo-atual", dados.saldoAtualConsolidado, "SALDO");
		renderValor("financeiro-card-receitas", dados.receitasPagas, "ENTRADA");
		renderValor("financeiro-card-despesas", dados.despesasPagas, "SAIDA");
		renderValor("financeiro-card-resultado", dados.resultadoMes, "SALDO");
		renderValor("financeiro-card-a-receber", dados.totalPendenteReceber, "ENTRADA");
		renderValor("financeiro-card-a-pagar", dados.totalPendentePagar, "SAIDA");
		renderValor(
			"financeiro-card-a-receber-terceiros",
			dados.totalPendenteReceberTerceiros,
			"ENTRADA"
		);
		el("financeiro-card-contas-ativas").textContent = String(dados.quantidadeContasAtivas);
		el("financeiro-card-lancamentos-periodo").textContent = String(dados.quantidadeLancamentosPeriodo);

		renderComparativo(dados.receitasPagas, dados.despesasPagas);
		renderResumoCategorias(dados.resumoPorCategoria || []);
		renderUltimosLancamentos(dados.ultimosLancamentos || []);
	}

	function renderValor(id, valor, natureza) {
		var elemento = el(id);
		elemento.textContent = window.FinanceiroFormatacao.moeda(valor);
		window.FinanceiroFormatacao.aplicarSemantica(elemento, valor, natureza);
	}

	function renderComparativo(receitas, despesas) {
		var valorReceitas = Number(receitas) || 0;
		var valorDespesas = Number(despesas) || 0;
		var maior = Math.max(valorReceitas, valorDespesas);
		var vazio = el("financeiro-grafico-vazio");
		var grafico = document.querySelector(".financeiro-dashboard-comparativo");

		var elementoReceitas = el("financeiro-grafico-receitas-valor");
		var elementoDespesas = el("financeiro-grafico-despesas-valor");
		elementoReceitas.textContent = window.FinanceiroFormatacao.moeda(valorReceitas);
		elementoDespesas.textContent = window.FinanceiroFormatacao.moeda(valorDespesas);
		window.FinanceiroFormatacao.aplicarSemantica(elementoReceitas, valorReceitas, "ENTRADA");
		window.FinanceiroFormatacao.aplicarSemantica(elementoDespesas, valorDespesas, "SAIDA");
		el("financeiro-grafico-receitas-barra").style.width =
			(maior ? (valorReceitas / maior) * 100 : 0) + "%";
		el("financeiro-grafico-despesas-barra").style.width =
			(maior ? (valorDespesas / maior) * 100 : 0) + "%";

		grafico.hidden = maior === 0;
		vazio.hidden = maior !== 0;
		grafico.setAttribute(
			"aria-label",
			"Receitas " + window.FinanceiroFormatacao.moeda(valorReceitas)
				+ "; despesas " + window.FinanceiroFormatacao.moeda(valorDespesas)
		);
	}

	function renderResumoCategorias(resumo) {
		var container = el("financeiro-resumo-categorias");
		var vazio = el("financeiro-resumo-categorias-vazio");
		var despesas = resumo.filter(function (item) {
			return item.tipo === "DESPESA";
		});
		container.innerHTML = "";

		if (despesas.length === 0) {
			vazio.hidden = false;
			return;
		}
		vazio.hidden = true;

		var maior = despesas.reduce(function (max, item) {
			return Math.max(max, Number(item.total));
		}, 0.01);

		despesas.forEach(function (item) {
			var linha = document.createElement("div");
			linha.className = "criati-resumo-categoria-linha";

			var nome = document.createElement("span");
			nome.textContent = item.categoriaNome;

			var barraFundo = document.createElement("div");
			barraFundo.className = "criati-resumo-categoria-barra-fundo";
			var barra = document.createElement("div");
			barra.className = "criati-resumo-categoria-barra is-despesa";
			var percentual = Math.min(100, (Number(item.total) / maior) * 100);
			barra.style.width = percentual + "%";
			barraFundo.appendChild(barra);

			var total = document.createElement("span");
			total.textContent = window.FinanceiroFormatacao.moeda(item.total);
			window.FinanceiroFormatacao.aplicarSemantica(
				total,
				item.total,
				"SAIDA"
			);

			linha.appendChild(nome);
			linha.appendChild(barraFundo);
			linha.appendChild(total);
			container.appendChild(linha);
		});
	}

	function renderUltimosLancamentos(lancamentos) {
		var tbody = el("financeiro-ultimos-lancamentos-tbody");
		var vazio = el("financeiro-ultimos-lancamentos-vazio");
		var tabela = el("financeiro-ultimos-lancamentos-tabela");
		tbody.innerHTML = "";

		if (lancamentos.length === 0) {
			vazio.hidden = false;
			tabela.hidden = true;
			return;
		}
		vazio.hidden = true;
		tabela.hidden = false;

		lancamentos.forEach(function (lancamento) {
			var linha = document.createElement("tr");

			var celulaData = document.createElement("td");
			celulaData.textContent = window.FinanceiroFormatacao.dataBr(lancamento.dataCompetencia);

			var celulaDescricao = document.createElement("td");
			celulaDescricao.textContent = lancamento.descricao;

			var celulaCategoria = document.createElement("td");
			celulaCategoria.textContent = lancamento.categoriaNome;

			var celulaValor = document.createElement("td");
			window.FinanceiroFormatacao.aplicarSemantica(
				celulaValor,
				lancamento.valor,
				lancamento.tipo === "RECEITA" ? "ENTRADA" : "SAIDA"
			);
			celulaValor.textContent = (lancamento.tipo === "RECEITA" ? "+ " : "- ")
				+ window.FinanceiroFormatacao.moeda(lancamento.valor);

			var celulaStatus = document.createElement("td");
			var badge = document.createElement("span");
			badge.className = "criati-badge criati-badge-" + lancamento.status.toLowerCase();
			badge.textContent = STATUS_LABEL[lancamento.status] || lancamento.status;
			celulaStatus.appendChild(badge);

			linha.appendChild(celulaData);
			linha.appendChild(celulaDescricao);
			linha.appendChild(celulaCategoria);
			linha.appendChild(celulaValor);
			linha.appendChild(celulaStatus);
			tbody.appendChild(linha);
		});
	}

	window.FinanceiroDashboard = { iniciar: iniciar };
})(window, document);
