/* Criati Financeiro - dashboard: cards reais, resumo por categoria (barras CSS,
   sem biblioteca externa) e ultimos lancamentos. Consome exclusivamente
   GET /api/contexto/financeiro/dashboard. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { PENDENTE: "Pendente", PAGO: "Pago", CANCELADO: "Cancelado" };

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var seletor = el("financeiro-dashboard-competencia");
		if (!seletor) {
			return;
		}
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
		el("financeiro-card-saldo-atual").textContent = window.FinanceiroFormatacao.moeda(dados.saldoAtualConsolidado);
		el("financeiro-card-receitas").textContent = window.FinanceiroFormatacao.moeda(dados.receitasPagas);
		el("financeiro-card-despesas").textContent = window.FinanceiroFormatacao.moeda(dados.despesasPagas);
		el("financeiro-card-resultado").textContent = window.FinanceiroFormatacao.moeda(dados.resultadoMes);
		el("financeiro-card-a-receber").textContent = window.FinanceiroFormatacao.moeda(dados.totalPendenteReceber);
		el("financeiro-card-a-pagar").textContent = window.FinanceiroFormatacao.moeda(dados.totalPendentePagar);
		el("financeiro-card-contas-ativas").textContent = String(dados.quantidadeContasAtivas);
		el("financeiro-card-lancamentos-periodo").textContent = String(dados.quantidadeLancamentosPeriodo);

		renderResumoCategorias(dados.resumoPorCategoria || []);
		renderUltimosLancamentos(dados.ultimosLancamentos || []);
	}

	function renderResumoCategorias(resumo) {
		var container = el("financeiro-resumo-categorias");
		var vazio = el("financeiro-resumo-categorias-vazio");
		container.innerHTML = "";

		if (resumo.length === 0) {
			vazio.hidden = false;
			return;
		}
		vazio.hidden = true;

		var maior = resumo.reduce(function (max, item) {
			return Math.max(max, Number(item.total));
		}, 0.01);

		resumo.forEach(function (item) {
			var linha = document.createElement("div");
			linha.className = "criati-resumo-categoria-linha";

			var nome = document.createElement("span");
			nome.textContent = item.categoriaNome;

			var barraFundo = document.createElement("div");
			barraFundo.className = "criati-resumo-categoria-barra-fundo";
			var barra = document.createElement("div");
			barra.className = "criati-resumo-categoria-barra" + (item.tipo === "DESPESA" ? " is-despesa" : "");
			var percentual = Math.min(100, (Number(item.total) / maior) * 100);
			barra.style.width = percentual + "%";
			barraFundo.appendChild(barra);

			var total = document.createElement("span");
			total.textContent = window.FinanceiroFormatacao.moeda(item.total);

			linha.appendChild(nome);
			linha.appendChild(barraFundo);
			linha.appendChild(total);
			container.appendChild(linha);
		});
	}

	function renderUltimosLancamentos(lancamentos) {
		var tbody = el("financeiro-ultimos-lancamentos-tbody");
		var vazio = el("financeiro-ultimos-lancamentos-vazio");
		tbody.innerHTML = "";

		if (lancamentos.length === 0) {
			vazio.hidden = false;
			return;
		}
		vazio.hidden = true;

		lancamentos.forEach(function (lancamento) {
			var linha = document.createElement("tr");

			var celulaData = document.createElement("td");
			celulaData.textContent = window.FinanceiroFormatacao.dataBr(lancamento.dataCompetencia);

			var celulaDescricao = document.createElement("td");
			celulaDescricao.textContent = lancamento.descricao;

			var celulaCategoria = document.createElement("td");
			celulaCategoria.textContent = lancamento.categoriaNome;

			var celulaValor = document.createElement("td");
			celulaValor.className = lancamento.tipo === "RECEITA" ? "criati-valor-positivo" : "criati-valor-negativo";
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
