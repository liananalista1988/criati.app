/* Criati - painel administrativo minimo de aplicacoes por empresa (Superadministrador).
   Consome /api/admin/empresas e /api/admin/empresas/{id}/aplicacoes/**; nunca decide
   autorizacao no cliente, apenas reage ao que o backend permite (401/403/etc). */
(function (window, document) {
	"use strict";

	var VINCULO_LABEL = {
		ATIVO: "Habilitada",
		INATIVO: "Desabilitada"
	};

	function el(id) {
		return document.getElementById(id);
	}

	function carregarEmpresas() {
		var lista = el("criati-admin-empresas-lista");
		var carregando = el("criati-admin-empresas-carregando");
		var erro = el("criati-admin-empresas-erro");
		if (!lista) {
			return;
		}

		window.CriatiApi
			.get("/api/admin/empresas")
			.then(function (resposta) {
				if (carregando) {
					carregando.hidden = true;
				}
				renderEmpresas(lista, resposta.data || []);
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

	function renderEmpresas(lista, empresas) {
		lista.innerHTML = "";
		empresas.forEach(function (empresa) {
			var linha = document.createElement("div");
			linha.className = "criati-card";

			var titulo = document.createElement("p");
			titulo.className = "criati-card-value";
			titulo.textContent = empresa.nome + " (" + empresa.status + ")";

			var botao = document.createElement("button");
			botao.type = "button";
			botao.className = "criati-btn criati-btn-ghost";
			botao.textContent = "Ver aplicacoes";
			botao.addEventListener("click", function () {
				selecionarEmpresa(empresa.id, empresa.nome);
			});

			linha.appendChild(titulo);
			linha.appendChild(botao);
			lista.appendChild(linha);
		});
	}

	function selecionarEmpresa(empresaId, nomeEmpresa) {
		var painel = el("criati-admin-empresa-detalhe");
		var titulo = el("criati-admin-empresa-detalhe-titulo");
		var lista = el("criati-admin-empresa-detalhe-lista");
		if (!painel || !lista) {
			return;
		}
		painel.hidden = false;
		if (titulo) {
			titulo.textContent = "Aplicacoes de " + nomeEmpresa;
		}
		lista.innerHTML = "Carregando...";

		window.CriatiApi
			.get("/api/admin/empresas/" + empresaId + "/aplicacoes")
			.then(function (resposta) {
				renderDetalhe(lista, empresaId, resposta.data || []);
			})
			.catch(function () {
				lista.textContent = "Nao foi possivel carregar as aplicacoes desta empresa.";
			});
	}

	function renderDetalhe(lista, empresaId, aplicacoes) {
		lista.innerHTML = "";
		aplicacoes.forEach(function (aplicacao) {
			var linha = document.createElement("div");
			linha.className = "criati-card";

			var nome = document.createElement("p");
			nome.className = "criati-card-value";
			nome.textContent = aplicacao.nome;

			var situacao = document.createElement("p");
			situacao.className = "criati-card-sub";
			situacao.textContent = "Situacao: " + (VINCULO_LABEL[aplicacao.statusVinculo] || "Nao habilitada");

			var habilitada = aplicacao.statusVinculo === "ATIVO";
			var botao = document.createElement("button");
			botao.type = "button";
			botao.className = "criati-btn " + (habilitada ? "criati-btn-ghost" : "criati-btn-primary");
			botao.textContent = habilitada ? "Desabilitar" : "Habilitar";
			botao.addEventListener("click", function () {
				alternar(empresaId, aplicacao.codigo, habilitada, botao);
			});

			linha.appendChild(nome);
			linha.appendChild(situacao);
			linha.appendChild(botao);
			lista.appendChild(linha);
		});
	}

	function alternar(empresaId, codigo, habilitada, botao) {
		var acao = habilitada ? "desabilitar" : "habilitar";
		window.CriatiUI.setButtonLoading(botao, true, "Aguarde...");
		window.CriatiApi
			.post("/api/admin/empresas/" + empresaId + "/aplicacoes/" + codigo + "/" + acao)
			.then(function () {
				var lista = el("criati-admin-empresa-detalhe-lista");
				return window.CriatiApi.get("/api/admin/empresas/" + empresaId + "/aplicacoes")
					.then(function (resposta) {
						renderDetalhe(lista, empresaId, resposta.data || []);
					});
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel atualizar essa aplicacao agora.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	window.CriatiAdminAplicacoes = {
		carregarEmpresas: carregarEmpresas
	};
})(window, document);
