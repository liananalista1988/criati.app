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
		listarPagina: function (filtros) {
			return window.CriatiApi.get(BASE + "/lancamentos/pagina" + query(filtros));
		},
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

	var EMPRESTIMOS_BASE = BASE + "/emprestimos-concedidos";
	var PARCELAS_EMPRESTIMO_BASE = BASE + "/parcelas-emprestimo";
	var emprestimos = {
		listar: function (filtros) {
			return window.CriatiApi.get(EMPRESTIMOS_BASE + query(filtros));
		},
		criar: function (dados) {
			return window.CriatiApi.post(EMPRESTIMOS_BASE, dados);
		},
		buscar: function (id) {
			return window.CriatiApi.get(EMPRESTIMOS_BASE + "/" + id);
		},
		parcelas: function (id) {
			return window.CriatiApi.get(EMPRESTIMOS_BASE + "/" + id + "/parcelas");
		},
		cancelar: function (id, motivo) {
			return window.CriatiApi.post(EMPRESTIMOS_BASE + "/" + id + "/cancelar", { motivo: motivo });
		}
	};
	var parcelasEmprestimo = {
		listar: function (filtros) {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + query(filtros));
		},
		buscar: function (id) {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + "/" + id);
		},
		vencidas: function () {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + "/vencidas");
		},
		proximas: function (dias) {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + "/proximas-vencimento" + query({ dias: dias }));
		},
		resumo: function () {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + "/resumo");
		},
		dataPrometida: function (id, dataPrometida) {
			return window.CriatiApi.request(PARCELAS_EMPRESTIMO_BASE + "/" + id + "/data-prometida", {
				method: "PUT",
				body: { dataPrometida: dataPrometida || null }
			});
		},
		recebimentos: function (id) {
			return window.CriatiApi.get(PARCELAS_EMPRESTIMO_BASE + "/" + id + "/recebimentos");
		},
		receberIntegral: function (id, dados) {
			return window.CriatiApi.post(PARCELAS_EMPRESTIMO_BASE + "/" + id + "/receber-integral", dados);
		},
		receberParcial: function (id, dados) {
			return window.CriatiApi.post(PARCELAS_EMPRESTIMO_BASE + "/" + id + "/receber-parcial", dados);
		}
	};

	var COMPRAS_TERCEIROS_BASE = BASE + "/compras-terceiros";
	var comprasTerceiros = {
		listar: function (filtros) {
			return window.CriatiApi.get(COMPRAS_TERCEIROS_BASE + query(filtros));
		},
		criar: function (dados) {
			return window.CriatiApi.post(COMPRAS_TERCEIROS_BASE, dados);
		},
		buscar: function (id) {
			return window.CriatiApi.get(COMPRAS_TERCEIROS_BASE + "/" + id);
		},
		cancelar: function (id, motivo) {
			return window.CriatiApi.post(COMPRAS_TERCEIROS_BASE + "/" + id + "/cancelar", { motivo: motivo });
		}
	};
	var VALORES_A_RECEBER_CARTAO_BASE = BASE + "/valores-a-receber-cartao";
	var valoresAReceberCartao = {
		listar: function (filtros) {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + query(filtros));
		},
		buscar: function (id) {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + "/" + id);
		},
		vencidas: function () {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + "/vencidas");
		},
		proximas: function (dias) {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + "/proximas-vencimento" + query({ dias: dias }));
		},
		resumo: function () {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + "/resumo");
		},
		dataPrometida: function (id, dataPrometida) {
			return window.CriatiApi.request(VALORES_A_RECEBER_CARTAO_BASE + "/" + id + "/data-prometida", {
				method: "PUT",
				body: { dataPrometida: dataPrometida || null }
			});
		},
		ressarcimentos: function (id) {
			return window.CriatiApi.get(VALORES_A_RECEBER_CARTAO_BASE + "/" + id + "/ressarcimentos");
		},
		receberIntegral: function (id, dados) {
			return window.CriatiApi.post(VALORES_A_RECEBER_CARTAO_BASE + "/" + id + "/receber-integral", dados);
		},
		receberParcial: function (id, dados) {
			return window.CriatiApi.post(VALORES_A_RECEBER_CARTAO_BASE + "/" + id + "/receber-parcial", dados);
		},
		estornar: function (id, ressarcimentoId, motivo) {
			return window.CriatiApi.post(
				VALORES_A_RECEBER_CARTAO_BASE + "/" + id + "/ressarcimentos/" + ressarcimentoId + "/estornar",
				{ motivo: motivo });
		}
	};

	var FATURAS_BASE = BASE + "/faturas";
	var faturas = {
		listar: function (filtros) {
			return window.CriatiApi.get(FATURAS_BASE + query(filtros));
		},
		buscar: function (id) {
			return window.CriatiApi.get(FATURAS_BASE + "/" + id);
		},
		abrir: function (dados) {
			return window.CriatiApi.post(FATURAS_BASE, dados);
		},
		recompor: function (id) {
			return window.CriatiApi.post(FATURAS_BASE + "/" + id + "/recompor");
		},
		fechar: function (id) {
			return window.CriatiApi.post(FATURAS_BASE + "/" + id + "/fechar");
		},
		pagamentos: function (id) {
			return window.CriatiApi.get(FATURAS_BASE + "/" + id + "/pagamentos");
		},
		registrarPagamento: function (id, dados) {
			return window.CriatiApi.post(FATURAS_BASE + "/" + id + "/pagamentos", dados);
		},
		aplicarEncargos: function (id, dados) {
			return window.CriatiApi.post(FATURAS_BASE + "/" + id + "/encargos", dados);
		}
	};

	var IMPORTACOES_BASE = BASE + "/importacoes-bancarias";
	var importacoesBancarias = {
		listar: function () {
			return window.CriatiApi.get(IMPORTACOES_BASE);
		},
		buscar: function (id) {
			return window.CriatiApi.get(IMPORTACOES_BASE + "/" + id);
		},
		// contaId e sempre opcional (CRIATI-IMP-FEAT-004/FIX-007): quando
		// informado, vai na query string (o backend le via @RequestParam,
		// required=false); quando omitido, o lote fica pendente de resolucao
		// (ver resolverConta abaixo). O corpo multipart carrega somente o
		// arquivo - nunca recalcula hash, duplicidade ou natureza financeira
		// aqui, isso e responsabilidade exclusiva do backend. formato ("OFX",
		// "CSV" ou "XLSX") so escolhe o endpoint de upload; os tres usam o
		// mesmo parser/servico/contrato de resposta no backend.
		importar: function (contaId, arquivo, formato) {
			var caminho = formato === "CSV" ? "/csv" : formato === "XLSX" ? "/xlsx" : "/ofx";
			var formData = new FormData();
			formData.append("arquivo", arquivo);
			var url = IMPORTACOES_BASE + caminho + (contaId ? "?contaId=" + encodeURIComponent(contaId) : "");
			return window.CriatiApi.upload(url, formData);
		},
		// Resolve a conta de um lote criado sem contaId; o backend recalcula a
		// duplicidade historica de todas as transacoes do lote nessa mesma
		// chamada (CRIATI-IMP-FIX-007) - so pode ser chamado uma vez por lote.
		resolverConta: function (id, contaId) {
			return window.CriatiApi.post(IMPORTACOES_BASE + "/" + id + "/conta", { contaId: contaId });
		},
		descartar: function (id) {
			return window.CriatiApi.post(IMPORTACOES_BASE + "/" + id + "/descartar");
		},
		resumo: function (id) {
			return window.CriatiApi.get(IMPORTACOES_BASE + "/" + id + "/resumo");
		},
		// comandos: [{transacaoId, categoriaId, descricaoFinal, confirmarDuplicidade}].
		// O backend e o unico responsavel por criar o LancamentoFinanceiro,
		// decidir receita/despesa pelo sinal do valor e validar duplicidade.
		confirmar: function (id, comandos) {
			return window.CriatiApi.post(IMPORTACOES_BASE + "/" + id + "/confirmacoes", { transacoes: comandos });
		},
		ignorar: function (id, transacaoId) {
			return window.CriatiApi.post(IMPORTACOES_BASE + "/" + id + "/transacoes/" + encodeURIComponent(transacaoId) + "/ignorar");
		}
	};

	var regrasImportacao = recurso(BASE + "/regras-importacao");

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
		emprestimos: emprestimos,
		parcelasEmprestimo: parcelasEmprestimo,
		comprasTerceiros: comprasTerceiros,
		valoresAReceberCartao: valoresAReceberCartao,
		faturas: faturas,
		importacoesBancarias: importacoesBancarias,
		regrasImportacao: regrasImportacao,
		dashboard: dashboard
	};
})(window);
