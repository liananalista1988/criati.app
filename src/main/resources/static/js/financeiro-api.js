/* Criati Financeiro - chamadas HTTP do modulo, construidas sobre CriatiApi
   (criati-api.js). Nunca cria um cliente HTTP novo: so monta URLs e delega
   o fetch/CSRF/tratamento de erro ao cliente central ja existente. */
(function (window) {
	"use strict";

	var BASE = "/api/contexto/financeiro";

	function query(params) {
		var partes = [];
		Object.keys(params || {}).forEach(function (chave) {
			var valor = params[chave];
			if (valor !== undefined && valor !== null && valor !== "") {
				partes.push(encodeURIComponent(chave) + "=" + encodeURIComponent(valor));
			}
		});
		return partes.length ? "?" + partes.join("&") : "";
	}

	function recurso(caminhoBase) {
		function url(id, acao) {
			var caminho = caminhoBase;
			if (id) {
				caminho += "/" + id;
			}
			if (acao) {
				caminho += "/" + acao;
			}
			return caminho;
		}

		return {
			listar: function (filtros) {
				return window.CriatiApi.get(url() + query(filtros));
			},
			criar: function (dados) {
				return window.CriatiApi.post(url(), dados);
			},
			buscar: function (id) {
				return window.CriatiApi.get(url(id));
			},
			editar: function (id, dados) {
				return window.CriatiApi.request(url(id), { method: "PUT", body: dados });
			},
			inativar: function (id) {
				return window.CriatiApi.post(url(id, "inativar"));
			},
			reativar: function (id) {
				return window.CriatiApi.post(url(id, "reativar"));
			}
		};
	}

	var contas = recurso(BASE + "/contas");
	contas.resumo = function () { return window.CriatiApi.get(BASE + "/contas/resumo"); };
	contas.titulares = function () { return window.CriatiApi.get(BASE + "/contas/titulares"); };
	contas.instituicoes = function () { return window.CriatiApi.get(BASE + "/contas/instituicoes"); };
	contas.criarInstituicao = function (dados) { return window.CriatiApi.post(BASE + "/contas/instituicoes", dados); };
	var categorias = recurso(BASE + "/categorias");
	categorias.resumo = function () { return window.CriatiApi.get(BASE + "/categorias/resumo"); };
	var pessoas = recurso(BASE + "/pessoas");
	var contatos = recurso(BASE + "/contatos");
	var lancamentosBase = recurso(BASE + "/lancamentos");

	var lancamentos = {
		listar: lancamentosBase.listar,
		criar: lancamentosBase.criar,
		buscar: lancamentosBase.buscar,
		editar: lancamentosBase.editar,
		pagar: function (id, dataPagamento) {
			return window.CriatiApi.post(BASE + "/lancamentos/" + id + "/pagar", { dataPagamento: dataPagamento });
		},
		reabrir: function (id) {
			return window.CriatiApi.post(BASE + "/lancamentos/" + id + "/reabrir");
		},
		liquidar: function (id, dados) {
			return window.CriatiApi.post(BASE + "/lancamentos/" + id + "/liquidar", dados);
		},
		desliquidar: function (id) {
			return window.CriatiApi.post(BASE + "/lancamentos/" + id + "/desliquidar");
		},
		resumo: function (filtros) {
			return window.CriatiApi.get(BASE + "/lancamentos/resumo" + query(filtros));
		},
		cancelar: function (id) {
			return window.CriatiApi.post(BASE + "/lancamentos/" + id + "/cancelar");
		}
	};

	function dashboard(competencia) {
		return window.CriatiApi.get(BASE + "/dashboard" + query({ competencia: competencia }));
	}

	window.FinanceiroApi = {
		contas: contas,
		categorias: categorias,
		pessoas: pessoas,
		contatos: contatos,
		usuariosVinculaveis: function () {
			return window.CriatiApi.get(BASE + "/pessoas/usuarios-vinculaveis");
		},
		lancamentos: lancamentos,
		dashboard: dashboard
	};
})(window);
