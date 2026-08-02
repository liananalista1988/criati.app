/* Criati - aplicacoes habilitadas para a empresa ativa: menu lateral dinamico,
   cards do dashboard e pagina /app/aplicacoes. Consome exclusivamente
   GET /api/contexto/aplicacoes; nunca decide autorizacao, apenas exibe o que
   o backend retorna. */
(function (window, document) {
	"use strict";

	function carregarSidebar() {
		// Compatibilidade com templates existentes. A sidebar agora e renderizada
		// pelo backend com a mesma decisao de disponibilidade usada nas rotas.
	}

	function carregarCardsDashboard() {
		var container = document.getElementById("criati-dashboard-aplicacoes");
		var vazio = document.getElementById("criati-dashboard-aplicacoes-vazio");
		if (!container) {
			return;
		}
		window.CriatiApi
			.get("/api/contexto/aplicacoes", { redirectOn401: false })
			.then(function (resposta) {
				renderCardsDashboard(container, vazio, resposta.data || []);
			})
			.catch(function () {
				renderCardsDashboard(container, vazio, []);
			});
	}

	function renderCardsDashboard(container, vazio, aplicacoes) {
		container.innerHTML = "";
		if (aplicacoes.length === 0) {
			if (vazio) {
				vazio.hidden = false;
			}
			return;
		}
		if (vazio) {
			vazio.hidden = true;
		}
		aplicacoes.forEach(function (aplicacao) {
			container.appendChild(criarCardAplicacao(aplicacao));
		});
	}

	function criarCardAplicacao(aplicacao) {
		var card = document.createElement("article");
		card.className = "criati-card criati-module-card";
		card.setAttribute("data-modulo", aplicacao.chaveVisual || "modulo");

		var cabecalho = document.createElement("div");
		cabecalho.className = "criati-module-card-header";

		var nome = document.createElement("p");
		nome.className = "criati-card-value";
		nome.textContent = aplicacao.nome;

		var situacao = document.createElement("span");
		situacao.className = "criati-module-status";
		situacao.textContent = aplicacao.situacaoDisponibilidade === "DEMONSTRACAO"
			? "Demonstração" : "Disponível";
		cabecalho.appendChild(nome);
		cabecalho.appendChild(situacao);

		var descricao = document.createElement("p");
		descricao.className = "criati-card-sub";
		descricao.textContent = aplicacao.descricao || "";

		var botao = document.createElement("a");
		botao.className = "criati-btn criati-btn-primary";
		botao.href = aplicacao.urlInicial;
		botao.textContent = "Abrir módulo";
		botao.setAttribute("aria-label", "Abrir " + aplicacao.nome);

		card.appendChild(cabecalho);
		card.appendChild(descricao);
		card.appendChild(botao);
		return card;
	}

	function carregarListaAplicacoes() {
		var carregando = document.getElementById("criati-aplicacoes-carregando");
		var lista = document.getElementById("criati-aplicacoes-lista");
		var vazio = document.getElementById("criati-aplicacoes-vazio");
		var erro = document.getElementById("criati-aplicacoes-erro");
		if (!lista) {
			return;
		}

		window.CriatiApi
			.get("/api/contexto/aplicacoes")
			.then(function (resposta) {
				lista.innerHTML = "";
				if (carregando) {
					carregando.hidden = true;
				}
				var aplicacoes = resposta.data || [];
				if (aplicacoes.length === 0) {
					if (vazio) {
						vazio.hidden = false;
					}
					return;
				}
				aplicacoes.forEach(function (aplicacao) {
					lista.appendChild(criarCardAplicacao(aplicacao));
				});
			})
			.catch(function () {
				if (carregando) {
					carregando.hidden = true;
				}
				if (erro) {
					erro.hidden = false;
				}
			});
	}

	window.CriatiAplicacoes = {
		carregarSidebar: carregarSidebar,
		carregarCardsDashboard: carregarCardsDashboard,
		carregarListaAplicacoes: carregarListaAplicacoes
	};
})(window, document);
