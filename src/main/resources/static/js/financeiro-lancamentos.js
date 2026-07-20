/* Criati Financeiro - tela de lancamentos: listar com filtros, criar receita/despesa,
   editar, pagar, reabrir, cancelar. Todo texto vindo da API usa textContent (nunca
   innerHTML com conteudo nao confiavel). */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { PENDENTE: "Pendente", PAGO: "Pago", CANCELADO: "Cancelado" };
	var TIPO_LABEL = { RECEITA: "Receita", DESPESA: "Despesa" };

	var lancamentoEmEdicaoId = null;
	var lancamentoParaPagarId = null;
	var contasCache = [];
	var categoriasCache = [];

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var form = el("financeiro-lancamento-form");
		if (!form) {
			return;
		}

		el("financeiro-lancamentos-nova-receita").addEventListener("click", function () {
			abrirModalCriacao("RECEITA");
		});
		el("financeiro-lancamentos-nova-despesa").addEventListener("click", function () {
			abrirModalCriacao("DESPESA");
		});
		el("financeiro-lancamento-cancelar").addEventListener("click", fecharModal);
		form.addEventListener("submit", salvar);

		el("financeiro-pagar-cancelar").addEventListener("click", fecharModalPagar);
		el("financeiro-pagar-form").addEventListener("submit", confirmarPagamento);

		["financeiro-filtro-data-inicial", "financeiro-filtro-data-final", "financeiro-filtro-tipo",
			"financeiro-filtro-status", "financeiro-filtro-conta", "financeiro-filtro-categoria"]
			.forEach(function (id) {
				el(id).addEventListener("change", carregarLista);
			});
		el("financeiro-filtro-busca").addEventListener("input", debounce(carregarLista, 350));

		carregarSelects().then(carregarLista);
	}

	function debounce(fn, atraso) {
		var timer;
		return function () {
			window.clearTimeout(timer);
			timer = window.setTimeout(fn, atraso);
		};
	}

	function carregarSelects() {
		return Promise.all([
			window.FinanceiroApi.contas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.categorias.listar({ status: "ATIVO" })
		]).then(function (respostas) {
			contasCache = respostas[0].data || [];
			categoriasCache = respostas[1].data || [];
			preencherSelect(el("financeiro-filtro-conta"), contasCache, "Todas as contas");
			preencherSelect(el("financeiro-filtro-categoria"), categoriasCache, "Todas as categorias");
			preencherSelect(el("financeiro-lancamento-conta"), contasCache, null);
		});
	}

	function preencherSelect(select, itens, opcaoTodos) {
		select.innerHTML = "";
		if (opcaoTodos) {
			var optTodos = document.createElement("option");
			optTodos.value = "";
			optTodos.textContent = opcaoTodos;
			select.appendChild(optTodos);
		}
		itens.forEach(function (item) {
			var option = document.createElement("option");
			option.value = item.id;
			option.textContent = item.nome;
			select.appendChild(option);
		});
	}

	function preencherCategoriasPorTipo(tipo) {
		var select = el("financeiro-lancamento-categoria");
		var filtradas = categoriasCache.filter(function (categoria) {
			return categoria.tipo === tipo;
		});
		preencherSelect(select, filtradas, null);
	}

	function carregarLista() {
		var carregando = el("financeiro-lancamentos-carregando");
		var erro = el("financeiro-lancamentos-erro");
		var vazio = el("financeiro-lancamentos-vazio");
		var tabelaWrap = el("financeiro-lancamentos-tabela-wrap");

		carregando.hidden = false;
		erro.hidden = true;
		vazio.hidden = true;
		tabelaWrap.hidden = true;

		var filtros = {
			dataInicial: el("financeiro-filtro-data-inicial").value,
			dataFinal: el("financeiro-filtro-data-final").value,
			tipo: el("financeiro-filtro-tipo").value,
			status: el("financeiro-filtro-status").value,
			contaId: el("financeiro-filtro-conta").value,
			categoriaId: el("financeiro-filtro-categoria").value,
			busca: el("financeiro-filtro-busca").value
		};

		window.FinanceiroApi.lancamentos
			.listar(filtros)
			.then(function (resposta) {
				carregando.hidden = true;
				var lancamentos = resposta.data || [];
				if (lancamentos.length === 0) {
					vazio.hidden = false;
					return;
				}
				tabelaWrap.hidden = false;
				renderTabela(lancamentos);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderTabela(lancamentos) {
		var tbody = el("financeiro-lancamentos-tbody");
		tbody.innerHTML = "";
		lancamentos.forEach(function (lancamento) {
			tbody.appendChild(criarLinha(lancamento));
		});
	}

	function criarLinha(lancamento) {
		var linha = document.createElement("tr");

		linha.appendChild(criarCelula(window.FinanceiroFormatacao.dataBr(lancamento.dataCompetencia)));
		linha.appendChild(criarCelula(lancamento.descricao));
		linha.appendChild(criarCelula(lancamento.categoriaNome));
		linha.appendChild(criarCelula(lancamento.contaNome));
		linha.appendChild(criarCelula(TIPO_LABEL[lancamento.tipo] || lancamento.tipo));

		var celulaValor = document.createElement("td");
		celulaValor.className = lancamento.tipo === "RECEITA" ? "criati-valor-positivo" : "criati-valor-negativo";
		celulaValor.textContent = (lancamento.tipo === "RECEITA" ? "+ " : "- ")
			+ window.FinanceiroFormatacao.moeda(lancamento.valor);
		linha.appendChild(celulaValor);

		var celulaStatus = document.createElement("td");
		var badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + lancamento.status.toLowerCase();
		badge.textContent = STATUS_LABEL[lancamento.status] || lancamento.status;
		celulaStatus.appendChild(badge);
		linha.appendChild(celulaStatus);

		linha.appendChild(criarCelula(window.FinanceiroFormatacao.dataBr(lancamento.dataPagamento)));

		linha.appendChild(criarCelulaAcoes(lancamento));
		return linha;
	}

	function criarCelula(texto) {
		var celula = document.createElement("td");
		celula.textContent = texto;
		return celula;
	}

	function criarCelulaAcoes(lancamento) {
		var celula = document.createElement("td");
		celula.className = "criati-table-acoes";

		if (lancamento.status === "PENDENTE") {
			celula.appendChild(criarBotaoAcao("Editar", function () {
				abrirModalEdicao(lancamento);
			}));
			celula.appendChild(criarBotaoAcao("Pagar", function () {
				abrirModalPagar(lancamento);
			}));
			celula.appendChild(criarBotaoAcao("Cancelar", function () {
				cancelar(lancamento);
			}));
		} else if (lancamento.status === "PAGO") {
			celula.appendChild(criarBotaoAcao("Reabrir", function () {
				reabrir(lancamento);
			}));
			celula.appendChild(criarBotaoAcao("Cancelar", function () {
				cancelar(lancamento);
			}));
		}

		return celula;
	}

	function criarBotaoAcao(texto, aoClicar) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-ghost";
		botao.textContent = texto;
		botao.addEventListener("click", aoClicar);
		return botao;
	}

	function cancelar(lancamento) {
		window.FinanceiroApi.lancamentos
			.cancelar(lancamento.id)
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Lancamento cancelado.");
				carregarLista();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel cancelar o lancamento.");
			});
	}

	function reabrir(lancamento) {
		window.FinanceiroApi.lancamentos
			.reabrir(lancamento.id)
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Lancamento reaberto.");
				carregarLista();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel reabrir o lancamento.");
			});
	}

	function abrirModalPagar(lancamento) {
		lancamentoParaPagarId = lancamento.id;
		el("financeiro-pagar-data").value = new Date().toISOString().slice(0, 10);
		el("financeiro-pagar-modal").hidden = false;
	}

	function fecharModalPagar() {
		el("financeiro-pagar-modal").hidden = true;
		lancamentoParaPagarId = null;
	}

	function confirmarPagamento(evento) {
		evento.preventDefault();
		var botao = el("financeiro-pagar-confirmar");
		var dataPagamento = el("financeiro-pagar-data").value;

		window.CriatiUI.setButtonLoading(botao, true, "Confirmando...");
		window.FinanceiroApi.lancamentos
			.pagar(lancamentoParaPagarId, dataPagamento)
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Lancamento pago.");
				fecharModalPagar();
				carregarLista();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel confirmar o pagamento.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	function abrirModalCriacao(tipo) {
		lancamentoEmEdicaoId = null;
		el("financeiro-lancamento-modal-titulo").textContent = tipo === "RECEITA" ? "Nova receita" : "Nova despesa";
		el("financeiro-lancamento-tipo").value = tipo;
		el("financeiro-lancamento-descricao").value = "";
		el("financeiro-lancamento-valor").value = "";
		el("financeiro-lancamento-data-competencia").value = new Date().toISOString().slice(0, 10);
		el("financeiro-lancamento-observacao").value = "";
		preencherCategoriasPorTipo(tipo);
		el("financeiro-lancamento-modal").hidden = false;
	}

	function abrirModalEdicao(lancamento) {
		lancamentoEmEdicaoId = lancamento.id;
		el("financeiro-lancamento-modal-titulo").textContent = "Editar lancamento";
		el("financeiro-lancamento-tipo").value = lancamento.tipo;
		el("financeiro-lancamento-descricao").value = lancamento.descricao;
		el("financeiro-lancamento-valor").value = lancamento.valor;
		el("financeiro-lancamento-data-competencia").value = lancamento.dataCompetencia;
		el("financeiro-lancamento-observacao").value = lancamento.observacao || "";
		preencherCategoriasPorTipo(lancamento.tipo);
		el("financeiro-lancamento-conta").value = lancamento.contaId;
		el("financeiro-lancamento-categoria").value = lancamento.categoriaId;
		el("financeiro-lancamento-modal").hidden = false;
	}

	function fecharModal() {
		el("financeiro-lancamento-modal").hidden = true;
		el("financeiro-lancamento-form").reset();
		lancamentoEmEdicaoId = null;
	}

	function salvar(evento) {
		evento.preventDefault();
		var botaoSalvar = el("financeiro-lancamento-salvar");
		var tipo = el("financeiro-lancamento-tipo").value;
		var dadosComuns = {
			contaId: el("financeiro-lancamento-conta").value,
			categoriaId: el("financeiro-lancamento-categoria").value,
			descricao: el("financeiro-lancamento-descricao").value,
			valor: el("financeiro-lancamento-valor").value,
			dataCompetencia: el("financeiro-lancamento-data-competencia").value,
			observacao: el("financeiro-lancamento-observacao").value
		};

		window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		var chamada = lancamentoEmEdicaoId
			? window.FinanceiroApi.lancamentos.editar(lancamentoEmEdicaoId, dadosComuns)
			: window.FinanceiroApi.lancamentos.criar(Object.assign({ tipo: tipo }, dadosComuns));

		chamada
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Lancamento salvo com sucesso.");
				fecharModal();
				carregarLista();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel salvar o lancamento.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botaoSalvar, false);
			});
	}

	window.FinanceiroLancamentos = { iniciar: iniciar };
})(window, document);
