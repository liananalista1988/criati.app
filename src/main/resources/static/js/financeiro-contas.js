/* Criati Financeiro - tela de contas: listar, criar, editar, inativar, reativar. */
(function (window, document) {
	"use strict";

	var TIPO_LABEL = {
		CAIXA: "Caixa",
		CONTA_CORRENTE: "Conta corrente",
		POUPANCA: "Poupanca",
		INVESTIMENTO: "Investimento",
		OUTRA: "Outra"
	};

	var contaEmEdicaoId = null;

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var botaoNova = el("financeiro-contas-nova");
		var filtroStatus = el("financeiro-contas-filtro-status");
		var form = el("financeiro-conta-form");
		var cancelar = el("financeiro-conta-cancelar");
		if (!form) {
			return;
		}

		botaoNova.addEventListener("click", function () {
			abrirModal(null);
		});
		cancelar.addEventListener("click", fecharModal);
		filtroStatus.addEventListener("change", carregar);
		form.addEventListener("submit", salvar);

		carregar();
	}

	function carregar() {
		var carregando = el("financeiro-contas-carregando");
		var erro = el("financeiro-contas-erro");
		var vazio = el("financeiro-contas-vazio");
		var tabelaWrap = el("financeiro-contas-tabela-wrap");

		carregando.hidden = false;
		erro.hidden = true;
		vazio.hidden = true;
		tabelaWrap.hidden = true;

		var filtros = { status: el("financeiro-contas-filtro-status").value };

		window.FinanceiroApi.contas
			.listar(filtros)
			.then(function (resposta) {
				carregando.hidden = true;
				var contas = resposta.data || [];
				if (contas.length === 0) {
					vazio.hidden = false;
					return;
				}
				tabelaWrap.hidden = false;
				renderTabela(contas);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderTabela(contas) {
		var tbody = el("financeiro-contas-tbody");
		tbody.innerHTML = "";
		contas.forEach(function (conta) {
			tbody.appendChild(criarLinha(conta));
		});
	}

	function criarLinha(conta) {
		var linha = document.createElement("tr");

		var celulaNome = document.createElement("td");
		celulaNome.textContent = conta.nome;

		var celulaTipo = document.createElement("td");
		celulaTipo.textContent = TIPO_LABEL[conta.tipo] || conta.tipo;

		var celulaSaldoInicial = document.createElement("td");
		celulaSaldoInicial.textContent = window.FinanceiroFormatacao.moeda(conta.saldoInicial);

		var celulaSaldoAtual = document.createElement("td");
		celulaSaldoAtual.textContent = window.FinanceiroFormatacao.moeda(conta.saldoAtual);

		var celulaStatus = document.createElement("td");
		var badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + conta.status.toLowerCase();
		badge.textContent = conta.status === "ATIVO" ? "Ativa" : "Inativa";
		celulaStatus.appendChild(badge);

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";

		var botaoEditar = document.createElement("button");
		botaoEditar.type = "button";
		botaoEditar.className = "criati-btn criati-btn-ghost";
		botaoEditar.textContent = "Editar";
		botaoEditar.addEventListener("click", function () {
			abrirModal(conta);
		});
		celulaAcoes.appendChild(botaoEditar);

		var botaoStatus = document.createElement("button");
		botaoStatus.type = "button";
		botaoStatus.className = "criati-btn criati-btn-ghost";
		botaoStatus.textContent = conta.status === "ATIVO" ? "Inativar" : "Reativar";
		botaoStatus.addEventListener("click", function () {
			alternarStatus(conta);
		});
		celulaAcoes.appendChild(botaoStatus);

		linha.appendChild(celulaNome);
		linha.appendChild(celulaTipo);
		linha.appendChild(celulaSaldoInicial);
		linha.appendChild(celulaSaldoAtual);
		linha.appendChild(celulaStatus);
		linha.appendChild(celulaAcoes);
		return linha;
	}

	function alternarStatus(conta) {
		var chamada = conta.status === "ATIVO"
			? window.FinanceiroApi.contas.inativar(conta.id)
			: window.FinanceiroApi.contas.reativar(conta.id);

		chamada
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Conta atualizada com sucesso.");
				carregar();
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel atualizar essa conta agora.");
			});
	}

	function abrirModal(conta) {
		contaEmEdicaoId = conta ? conta.id : null;
		el("financeiro-conta-modal-titulo").textContent = conta ? "Editar conta" : "Nova conta";
		el("financeiro-conta-nome").value = conta ? conta.nome : "";
		el("financeiro-conta-tipo").value = conta ? conta.tipo : "CAIXA";
		el("financeiro-conta-saldo").value = conta ? conta.saldoInicial : "0.00";
		el("financeiro-conta-modal").hidden = false;
	}

	function fecharModal() {
		el("financeiro-conta-modal").hidden = true;
		el("financeiro-conta-form").reset();
		contaEmEdicaoId = null;
	}

	function salvar(evento) {
		evento.preventDefault();
		var botaoSalvar = el("financeiro-conta-salvar");
		var dados = {
			nome: el("financeiro-conta-nome").value,
			tipo: el("financeiro-conta-tipo").value,
			saldoInicial: el("financeiro-conta-saldo").value
		};

		window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		var chamada = contaEmEdicaoId
			? window.FinanceiroApi.contas.editar(contaEmEdicaoId, dados)
			: window.FinanceiroApi.contas.criar(dados);

		chamada
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Conta salva com sucesso.");
				fecharModal();
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel salvar a conta.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botaoSalvar, false);
			});
	}

	window.FinanceiroContas = { iniciar: iniciar };
})(window, document);
