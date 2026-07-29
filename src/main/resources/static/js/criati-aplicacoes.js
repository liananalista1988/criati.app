/* Criati - aplicacoes habilitadas para a empresa ativa: menu lateral dinamico,
   cards do dashboard e pagina /app/aplicacoes. Consome exclusivamente
   GET /api/contexto/aplicacoes; nunca decide autorizacao, apenas exibe o que
   o backend retorna. */
(function (window, document) {
	"use strict";

	function carregarSidebar() {
		var placeholder = document.getElementById("criati-nav-apps-dinamicas");
		if (!placeholder) {
			return;
		}
		window.CriatiApi
			.get("/api/contexto/aplicacoes", { redirectOn401: false })
			.then(function (resposta) {
				renderSidebar(placeholder, resposta.data || []);
			})
			.catch(function () {
				// Sem contexto de empresa ativa (ex.: Superadministrador sem empresa
				// selecionada): nenhuma aplicacao dinamica aparece no menu.
			});
	}

	function renderSidebar(placeholder, aplicacoes) {
		var parent = placeholder.parentNode;
		aplicacoes.forEach(function (aplicacao) {
			if (document.querySelector("[data-menu-financeiro]")
					&& aplicacao.urlInicial.indexOf("/app/financeiro") === 0) {
				return;
			}
			var link = document.createElement("a");
			link.className = "criati-nav-link";
			link.href = aplicacao.urlInicial;
			link.setAttribute("aria-label", aplicacao.nome);
			link.setAttribute("data-tooltip", aplicacao.nome);
			if (window.location.pathname === aplicacao.urlInicial
					|| window.location.pathname.indexOf(aplicacao.urlInicial + "/") === 0) {
				link.classList.add("is-active");
				link.setAttribute("aria-current", "page");
			}

			// Icone generico (mesmo glifo de "Aplicacoes"): o catalogo nao define
			// um icone por aplicacao, e criar um novo ativo esta fora do escopo.
			link.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">'
				+ '<rect x="4" y="4" width="16" height="4" rx="1"></rect>'
				+ '<rect x="4" y="10" width="16" height="4" rx="1"></rect>'
				+ '<rect x="4" y="16" width="16" height="4" rx="1"></rect>'
				+ "</svg>";

			var label = document.createElement("span");
			label.className = "criati-nav-label";
			label.textContent = aplicacao.nome;
			link.appendChild(label);

			var item = document.createElement("li");
			item.appendChild(link);
			parent.insertBefore(item, placeholder);
		});
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
		card.className = "criati-card";

		var nome = document.createElement("p");
		nome.className = "criati-card-value";
		nome.textContent = aplicacao.nome;

		var descricao = document.createElement("p");
		descricao.className = "criati-card-sub";
		descricao.textContent = aplicacao.descricao || "";

		var botao = document.createElement("a");
		botao.className = "criati-btn criati-btn-primary";
		botao.href = aplicacao.urlInicial;
		botao.textContent = "Abrir";
		botao.style.marginTop = "12px";
		botao.style.display = "inline-block";

		card.appendChild(nome);
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
