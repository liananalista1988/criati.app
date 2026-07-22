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

	var recorrenciasBase = recurso(BASE + "/recorrencias");
	var recorrencias = {
		listar: recorrenciasBase.listar,
		criar: recorrenciasBase.criar,
		buscar: recorrenciasBase.buscar,
		editar: recorrenciasBase.editar,
		pausar: function (id) {
			return window.CriatiApi.post(BASE + "/recorrencias/" + id + "/pausar");
		},
		retomar: function (id) {
			return window.CriatiApi.post(BASE + "/recorrencias/" + id + "/retomar");
		},
		encerrar: function (id) {
			return window.CriatiApi.post(BASE + "/recorrencias/" + id + "/encerrar");
		},
		gerar: function (id) {
			return window.CriatiApi.post(BASE + "/recorrencias/" + id + "/gerar");
		},
		gerarCompetencia: function (id, competencia) {
			return window.CriatiApi.post(BASE + "/recorrencias/" + id + "/gerar-competencia", { competencia: competencia });
		},
		gerarAutomaticas: function () {
			return window.CriatiApi.post(BASE + "/recorrencias/gerar-automaticas");
		},
		ocorrencias: function (id) {
			return window.CriatiApi.get(BASE + "/recorrencias/" + id + "/ocorrencias");
		},
		resumo: function () {
			return window.CriatiApi.get(BASE + "/recorrencias/resumo");
		}
	};

	var compromissosBase = recurso(BASE + "/compromissos");
	var compromissos = {
		listar: compromissosBase.listar,
		criar: compromissosBase.criar,
		buscar: compromissosBase.buscar,
		editar: compromissosBase.editar,
		ativar: function (id) {
			return window.CriatiApi.post(BASE + "/compromissos/" + id + "/ativar");
		},
		desativar: function (id) {
			return window.CriatiApi.post(BASE + "/compromissos/" + id + "/desativar");
		}
	};

	var OCORRENCIAS_BASE = BASE + "/ocorrencias-compromisso";
	var ocorrenciasBase = recurso(OCORRENCIAS_BASE);
	var ocorrenciasCompromisso = {
		listar: ocorrenciasBase.listar,
		criar: ocorrenciasBase.criar,
		buscar: ocorrenciasBase.buscar,
		editar: ocorrenciasBase.editar,
		gerarPorCompromisso: function (compromissoId) {
			return window.CriatiApi.post(OCORRENCIAS_BASE + "/gerar-por-compromisso/" + compromissoId);
		},
		cancelar: function (id) {
			return window.CriatiApi.post(OCORRENCIAS_BASE + "/" + id + "/cancelar");
		},
		resumo: function (filtros) {
			return window.CriatiApi.get(OCORRENCIAS_BASE + "/resumo" + query(filtros));
		},
		calendario: function (mes) {
			return window.CriatiApi.get(OCORRENCIAS_BASE + "/calendario" + query({ mes: mes }));
		},
		pagamentos: function (id) {
			return window.CriatiApi.get(OCORRENCIAS_BASE + "/" + id + "/pagamentos");
		},
		pagarIntegral: function (id, dados) {
			return window.CriatiApi.post(OCORRENCIAS_BASE + "/" + id + "/pagar-integral", dados);
		},
		pagarParcial: function (id, dados) {
			return window.CriatiApi.post(OCORRENCIAS_BASE + "/" + id + "/pagar-parcial", dados);
		},
		estornarPagamento: function (id, pagamentoId, motivo) {
			return window.CriatiApi.post(
				OCORRENCIAS_BASE + "/" + id + "/pagamentos/" + pagamentoId + "/estornar", { motivo: motivo });
		}
	};

	var CARTOES_BASE = BASE + "/cartoes";
	var cartoesBase = recurso(CARTOES_BASE);
	var cartoes = {
		listar: cartoesBase.listar,
		criar: cartoesBase.criar,
		buscar: cartoesBase.buscar,
		editar: cartoesBase.editar,
		inativar: function (id) {
			return window.CriatiApi.post(CARTOES_BASE + "/" + id + "/inativar");
		},
		reativar: function (id) {
			return window.CriatiApi.post(CARTOES_BASE + "/" + id + "/reativar");
		},
		bloquear: function (id, motivo) {
			return window.CriatiApi.post(CARTOES_BASE + "/" + id + "/bloquear", { motivo: motivo });
		},
		desbloquear: function (id) {
			return window.CriatiApi.post(CARTOES_BASE + "/" + id + "/desbloquear");
		},
		virtuais: function (id) {
			return window.CriatiApi.get(CARTOES_BASE + "/" + id + "/virtuais");
		},
		resumo: function () {
			return window.CriatiApi.get(CARTOES_BASE + "/resumo");
		}
	};

	window.FinanceiroApi = {
		contas: contas,
		categorias: categorias,
		pessoas: pessoas,
		contatos: contatos,
		usuariosVinculaveis: function () {
			return window.CriatiApi.get(BASE + "/pessoas/usuarios-vinculaveis");
		},
		lancamentos: lancamentos,
		recorrencias: recorrencias,
		compromissos: compromissos,
		ocorrenciasCompromisso: ocorrenciasCompromisso,
		cartoes: cartoes,
		dashboard: dashboard
	};
})(window);
