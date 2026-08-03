/* Criati Trabalho - listagem de processos, com indicadores do painel local
   do modulo (esta e a pagina inicial do modulo Trabalho). */
(function (window, document) {
	"use strict";
	var pagina = 0;
	var tamanho = 20;
	var totalPaginas = 0;
	var submetendoFiltro = false;

	function el(id) { return document.getElementById(id); }

	function iniciar() {
		if (!el("trabalho-processos-corpo")) return;
		["trabalho-processos-situacao", "trabalho-processos-prioridade", "trabalho-processos-status"].forEach(function (id) {
			el(id).addEventListener("change", function () { pagina = 0; carregar(); });
		});
		el("trabalho-processos-atrasado").addEventListener("change", function () { pagina = 0; carregar(); });
		var buscaTimeout;
		el("trabalho-processos-busca").addEventListener("input", function () {
			window.clearTimeout(buscaTimeout);
			buscaTimeout = window.setTimeout(function () { pagina = 0; carregar(); }, 300);
		});
		el("trabalho-processos-anterior").addEventListener("click", function () {
			if (pagina > 0) { pagina -= 1; carregar(); }
		});
		el("trabalho-processos-proxima").addEventListener("click", function () {
			if (pagina + 1 < totalPaginas) { pagina += 1; carregar(); }
		});
		carregarIndicadores();
		carregar();
	}

	function filtros() {
		return {
			busca: el("trabalho-processos-busca").value,
			situacao: el("trabalho-processos-situacao").value,
			prioridade: el("trabalho-processos-prioridade").value,
			status: el("trabalho-processos-status").value,
			atrasado: el("trabalho-processos-atrasado").checked ? "true" : "",
			pagina: pagina,
			tamanho: tamanho
		};
	}

	function carregar() {
		if (submetendoFiltro) return;
		submetendoFiltro = true;
		el("trabalho-processos-carregando").hidden = false;
		el("trabalho-processos-erro").hidden = true;
		el("trabalho-processos-vazio").hidden = true;
		el("trabalho-processos-conteudo").hidden = true;
		window.TrabalhoApi.processos.listar(filtros())
			.then(function (resposta) {
				var dados = resposta.data || { itens: [], totalElementos: 0, totalPaginas: 0 };
				el("trabalho-processos-carregando").hidden = true;
				totalPaginas = dados.totalPaginas || 0;
				if (!dados.itens || !dados.itens.length) {
					el("trabalho-processos-vazio").hidden = false;
					atualizarPaginacao(dados);
					return;
				}
				el("trabalho-processos-conteudo").hidden = false;
				renderLinhas(dados.itens);
				atualizarPaginacao(dados);
			})
			.catch(function () {
				el("trabalho-processos-carregando").hidden = true;
				el("trabalho-processos-erro").hidden = false;
			})
			.finally(function () { submetendoFiltro = false; });
	}

	function atualizarPaginacao(dados) {
		var totalElementos = dados.totalElementos || 0;
		el("trabalho-processos-pagina-info").textContent = totalElementos === 0 ? "0 resultados" :
			"Página " + (pagina + 1) + " de " + Math.max(dados.totalPaginas || 1, 1) + " · " + totalElementos + " processo(s)";
		el("trabalho-processos-anterior").disabled = pagina === 0;
		el("trabalho-processos-proxima").disabled = pagina + 1 >= (dados.totalPaginas || 0);
	}

	function renderLinhas(itens) {
		var corpo = el("trabalho-processos-corpo");
		corpo.innerHTML = "";
		itens.forEach(function (processo) {
			var linha = document.createElement("tr");

			var celulaTitulo = document.createElement("td"); celulaTitulo.dataset.label = "Título";
			var linkTitulo = document.createElement("a");
			linkTitulo.href = "/app/trabalho/processos/" + processo.id;
			linkTitulo.textContent = processo.titulo;
			celulaTitulo.appendChild(linkTitulo);
			linha.appendChild(celulaTitulo);

			var celulaResp = document.createElement("td"); celulaResp.dataset.label = "Responsável";
			celulaResp.textContent = processo.responsavelNome || "Sem responsável";
			linha.appendChild(celulaResp);

			var celulaSituacao = document.createElement("td"); celulaSituacao.dataset.label = "Situação";
			var badgeSituacao = document.createElement("span");
			badgeSituacao.className = "criati-badge criati-badge-" + processo.situacao.toLowerCase();
			badgeSituacao.textContent = window.TrabalhoFormatacao.rotuloSituacao(processo.situacao);
			celulaSituacao.appendChild(badgeSituacao);
			if (processo.atrasado) {
				var badgeAtraso = document.createElement("span");
				badgeAtraso.className = "criati-badge criati-badge-atrasado";
				badgeAtraso.style.marginLeft = "6px";
				badgeAtraso.textContent = "Atrasado";
				celulaSituacao.appendChild(badgeAtraso);
			}
			linha.appendChild(celulaSituacao);

			var celulaPrioridade = document.createElement("td"); celulaPrioridade.dataset.label = "Prioridade";
			var badgePrioridade = document.createElement("span");
			badgePrioridade.className = "criati-badge criati-badge-prioridade";
			badgePrioridade.textContent = window.TrabalhoFormatacao.rotuloPrioridade(processo.prioridade);
			celulaPrioridade.appendChild(badgePrioridade);
			linha.appendChild(celulaPrioridade);

			var celulaAbertura = document.createElement("td"); celulaAbertura.dataset.label = "Abertura";
			celulaAbertura.textContent = window.TrabalhoFormatacao.dataBr(processo.dataAbertura);
			linha.appendChild(celulaAbertura);

			var celulaPrazo = document.createElement("td"); celulaPrazo.dataset.label = "Prazo";
			celulaPrazo.textContent = window.TrabalhoFormatacao.dataBr(processo.prazo);
			linha.appendChild(celulaPrazo);

			var celulaTarefas = document.createElement("td"); celulaTarefas.dataset.label = "Tarefas";
			celulaTarefas.textContent = processo.quantidadeTarefasConcluidas + " / " + processo.quantidadeTarefas;
			linha.appendChild(celulaTarefas);

			var celulaAcoes = document.createElement("td"); celulaAcoes.className = "criati-table-acoes"; celulaAcoes.dataset.label = "Ações";
			var detalhes = document.createElement("a");
			detalhes.className = "criati-btn criati-btn-ghost";
			detalhes.href = "/app/trabalho/processos/" + processo.id;
			detalhes.textContent = "Detalhes";
			celulaAcoes.appendChild(detalhes);
			linha.appendChild(celulaAcoes);

			corpo.appendChild(linha);
		});
	}

	function indicador(rotulo, valor, atrasado) {
		var caixa = document.createElement("div");
		caixa.className = "criati-trabalho-indicador" + (atrasado ? " is-atrasado" : "");
		var valorEl = document.createElement("div");
		valorEl.className = "criati-trabalho-indicador-valor";
		valorEl.textContent = String(valor);
		var rotuloEl = document.createElement("div");
		rotuloEl.className = "criati-trabalho-indicador-rotulo";
		rotuloEl.textContent = rotulo;
		caixa.appendChild(valorEl);
		caixa.appendChild(rotuloEl);
		return caixa;
	}

	function contar(promessa) {
		return promessa.then(function (resposta) { return (resposta.data && resposta.data.totalElementos) || 0; })
			.catch(function () { return null; });
	}

	function carregarIndicadores() {
		var container = el("trabalho-indicadores");
		if (!container) return;
		Promise.all([
			contar(window.TrabalhoApi.processos.listar({ situacao: "ABERTO", tamanho: 1 })),
			contar(window.TrabalhoApi.processos.listar({ atrasado: "true", tamanho: 1 })),
			contar(window.TrabalhoApi.tarefas.listar({ situacao: "PENDENTE", tamanho: 1 })),
			contar(window.TrabalhoApi.tarefas.listar({ minhasTarefas: "true", tamanho: 1 })),
			contar(window.TrabalhoApi.tarefas.listar({ atrasada: "true", tamanho: 1 }))
		]).then(function (valores) {
			container.innerHTML = "";
			container.appendChild(indicador("Processos abertos", valores[0] === null ? "-" : valores[0]));
			container.appendChild(indicador("Processos atrasados", valores[1] === null ? "-" : valores[1], valores[1] > 0));
			container.appendChild(indicador("Tarefas pendentes", valores[2] === null ? "-" : valores[2]));
			container.appendChild(indicador("Minhas tarefas", valores[3] === null ? "-" : valores[3]));
			container.appendChild(indicador("Tarefas atrasadas", valores[4] === null ? "-" : valores[4], valores[4] > 0));
		});
	}

	window.TrabalhoProcessos = { iniciar: iniciar };
})(window, document);
