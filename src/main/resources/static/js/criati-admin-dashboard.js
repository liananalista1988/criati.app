/* Criati - visao geral do painel global (/app/admin). Consome exclusivamente
   GET /api/admin/dashboard; numeros sempre vindos do backend, nunca calculados
   ou estimados no cliente. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var conteudo = el("criati-admin-dashboard-conteudo");
		if (!conteudo) {
			return;
		}

		var retry = el("criati-admin-dashboard-retry");
		if (retry) {
			retry.addEventListener("click", carregar);
		}

		carregar();
	}

	function carregar() {
		var carregando = el("criati-admin-dashboard-carregando");
		var erro = el("criati-admin-dashboard-erro");
		var conteudo = el("criati-admin-dashboard-conteudo");

		carregando.hidden = false;
		erro.hidden = true;
		conteudo.hidden = true;

		window.CriatiApi
			.get("/api/admin/dashboard")
			.then(function (resposta) {
				carregando.hidden = true;
				conteudo.hidden = false;
				renderDashboard(resposta.data);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderDashboard(dados) {
		el("criati-card-total-empresas").textContent = dados.totalEmpresas;
		el("criati-card-empresas-sub").textContent =
			dados.empresasAtivas + " ativas · " + dados.empresasInativas + " inativas";

		el("criati-card-total-usuarios").textContent = dados.totalUsuarios;
		el("criati-card-usuarios-sub").textContent = dados.usuariosAtivos + " ativos";

		el("criati-card-vinculos-ativos").textContent = dados.totalVinculosAtivos;
		el("criati-card-convites-pendentes").textContent = dados.convitesPendentes;
		el("criati-card-empresas-financeiro").textContent = dados.empresasComFinanceiroHabilitado;

		renderRecentes(dados.empresasRecentes || []);
	}

	function renderRecentes(empresas) {
		var vazio = el("criati-admin-dashboard-recentes-vazio");
		var wrap = el("criati-admin-dashboard-recentes-wrap");
		var tbody = el("criati-admin-dashboard-recentes-tbody");

		tbody.innerHTML = "";
		if (empresas.length === 0) {
			vazio.hidden = false;
			wrap.hidden = true;
			return;
		}
		vazio.hidden = true;
		wrap.hidden = false;

		empresas.forEach(function (empresa) {
			var linha = document.createElement("tr");

			var celulaNome = document.createElement("td");
			var link = document.createElement("a");
			link.href = "/app/admin/empresas/" + empresa.id;
			link.textContent = empresa.nome;
			celulaNome.appendChild(link);

			var celulaCnpj = document.createElement("td");
			celulaCnpj.textContent = empresa.cnpj;

			var celulaStatus = document.createElement("td");
			var badge = document.createElement("span");
			badge.className = "criati-badge criati-badge-" + empresa.status.toLowerCase();
			badge.textContent = STATUS_LABEL[empresa.status] || empresa.status;
			celulaStatus.appendChild(badge);

			linha.appendChild(celulaNome);
			linha.appendChild(celulaCnpj);
			linha.appendChild(celulaStatus);
			tbody.appendChild(linha);
		});
	}

	window.CriatiAdminDashboard = { iniciar: iniciar };
})(window, document);
