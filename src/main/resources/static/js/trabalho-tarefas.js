/* Criati Trabalho - listagem de tarefas. */
(function (window, document) {
	"use strict";
	var pagina = 0;
	var tamanho = 20;
	var totalPaginas = 0;
	var carregando = false;

	function el(id) { return document.getElementById(id); }

	function iniciar() {
		if (!el("trabalho-tarefas-corpo")) return;
		["trabalho-tarefas-situacao", "trabalho-tarefas-prioridade", "trabalho-tarefas-status"].forEach(function (id) {
			el(id).addEventListener("change", function () { pagina = 0; carregar(); });
		});
		el("trabalho-tarefas-minhas").addEventListener("change", function () { pagina = 0; carregar(); });
		el("trabalho-tarefas-atrasada").addEventListener("change", function () { pagina = 0; carregar(); });
		var buscaTimeout;
		el("trabalho-tarefas-busca").addEventListener("input", function () {
			window.clearTimeout(buscaTimeout);
			buscaTimeout = window.setTimeout(function () { pagina = 0; carregar(); }, 300);
		});
		el("trabalho-tarefas-anterior").addEventListener("click", function () { if (pagina > 0) { pagina -= 1; carregar(); } });
		el("trabalho-tarefas-proxima").addEventListener("click", function () { if (pagina + 1 < totalPaginas) { pagina += 1; carregar(); } });
		carregar();
	}

	function filtros() {
		return {
			busca: el("trabalho-tarefas-busca").value,
			situacao: el("trabalho-tarefas-situacao").value,
			prioridade: el("trabalho-tarefas-prioridade").value,
			status: el("trabalho-tarefas-status").value,
			minhasTarefas: el("trabalho-tarefas-minhas").checked ? "true" : "",
			atrasada: el("trabalho-tarefas-atrasada").checked ? "true" : "",
			pagina: pagina,
			tamanho: tamanho
		};
	}

	function carregar() {
		if (carregando) return;
		carregando = true;
		el("trabalho-tarefas-carregando").hidden = false;
		el("trabalho-tarefas-erro").hidden = true;
		el("trabalho-tarefas-vazio").hidden = true;
		el("trabalho-tarefas-conteudo").hidden = true;
		window.TrabalhoApi.tarefas.listar(filtros())
			.then(function (resposta) {
				var dados = resposta.data || { itens: [], totalElementos: 0, totalPaginas: 0 };
				el("trabalho-tarefas-carregando").hidden = true;
				totalPaginas = dados.totalPaginas || 0;
				if (!dados.itens || !dados.itens.length) {
					el("trabalho-tarefas-vazio").hidden = false;
					atualizarPaginacao(dados);
					return;
				}
				el("trabalho-tarefas-conteudo").hidden = false;
				renderLinhas(dados.itens);
				atualizarPaginacao(dados);
			})
			.catch(function () {
				el("trabalho-tarefas-carregando").hidden = true;
				el("trabalho-tarefas-erro").hidden = false;
			})
			.finally(function () { carregando = false; });
	}

	function atualizarPaginacao(dados) {
		var totalElementos = dados.totalElementos || 0;
		el("trabalho-tarefas-pagina-info").textContent = totalElementos === 0 ? "0 resultados" :
			"Página " + (pagina + 1) + " de " + Math.max(dados.totalPaginas || 1, 1) + " · " + totalElementos + " tarefa(s)";
		el("trabalho-tarefas-anterior").disabled = pagina === 0;
		el("trabalho-tarefas-proxima").disabled = pagina + 1 >= (dados.totalPaginas || 0);
	}

	function renderLinhas(itens) {
		var corpo = el("trabalho-tarefas-corpo");
		corpo.innerHTML = "";
		itens.forEach(function (tarefa) {
			var linha = document.createElement("tr");

			var celulaTitulo = document.createElement("td"); celulaTitulo.dataset.label = "Título";
			var link = document.createElement("a"); link.href = "/app/trabalho/tarefas/" + tarefa.id; link.textContent = tarefa.titulo;
			celulaTitulo.appendChild(link); linha.appendChild(celulaTitulo);

			var celulaProcesso = document.createElement("td"); celulaProcesso.dataset.label = "Processo";
			if (tarefa.processoId) {
				var linkProcesso = document.createElement("a");
				linkProcesso.href = "/app/trabalho/processos/" + tarefa.processoId;
				linkProcesso.textContent = tarefa.processoTitulo;
				celulaProcesso.appendChild(linkProcesso);
			} else {
				celulaProcesso.textContent = "-";
			}
			linha.appendChild(celulaProcesso);

			var celulaResp = document.createElement("td"); celulaResp.dataset.label = "Responsável";
			celulaResp.textContent = tarefa.responsavelNome || "Sem responsável";
			linha.appendChild(celulaResp);

			var celulaSituacao = document.createElement("td"); celulaSituacao.dataset.label = "Situação";
			var badgeSituacao = document.createElement("span");
			badgeSituacao.className = "criati-badge criati-badge-" + tarefa.situacao.toLowerCase();
			badgeSituacao.textContent = window.TrabalhoFormatacao.rotuloSituacao(tarefa.situacao);
			celulaSituacao.appendChild(badgeSituacao);
			if (tarefa.atrasada) {
				var badgeAtraso = document.createElement("span");
				badgeAtraso.className = "criati-badge criati-badge-atrasado";
				badgeAtraso.style.marginLeft = "6px";
				badgeAtraso.textContent = "Atrasada";
				celulaSituacao.appendChild(badgeAtraso);
			}
			linha.appendChild(celulaSituacao);

			var celulaPrioridade = document.createElement("td"); celulaPrioridade.dataset.label = "Prioridade";
			var badgePrioridade = document.createElement("span");
			badgePrioridade.className = "criati-badge criati-badge-prioridade";
			badgePrioridade.textContent = window.TrabalhoFormatacao.rotuloPrioridade(tarefa.prioridade);
			celulaPrioridade.appendChild(badgePrioridade);
			linha.appendChild(celulaPrioridade);

			var celulaPrazo = document.createElement("td"); celulaPrazo.dataset.label = "Prazo";
			celulaPrazo.textContent = window.TrabalhoFormatacao.dataBr(tarefa.prazo);
			linha.appendChild(celulaPrazo);

			var celulaAcoes = document.createElement("td"); celulaAcoes.className = "criati-table-acoes"; celulaAcoes.dataset.label = "Ações";
			var detalhes = document.createElement("a");
			detalhes.className = "criati-btn criati-btn-ghost";
			detalhes.href = "/app/trabalho/tarefas/" + tarefa.id;
			detalhes.textContent = "Detalhes";
			celulaAcoes.appendChild(detalhes);
			linha.appendChild(celulaAcoes);

			corpo.appendChild(linha);
		});
	}

	window.TrabalhoTarefas = { iniciar: iniciar };
})(window, document);
