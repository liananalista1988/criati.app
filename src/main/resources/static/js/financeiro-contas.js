/* Criati Financeiro - gestao de contas financeiras da residencia. */
(function (window, document) {
	"use strict";

	var ROTULOS = {
		CAIXA: "Caixa (legado)", CONTA_CORRENTE: "Conta corrente", CONTA_PAGAMENTO: "Conta de pagamento",
		POUPANCA: "Poupança", DINHEIRO: "Dinheiro", CARTEIRA: "Carteira",
		INVESTIMENTO: "Investimento (legado)", OUTRA: "Outra"
	};
	var contaEmEdicaoId = null;

	function el(id) { return document.getElementById(id); }
	function hoje() { return new Date().toISOString().slice(0, 10); }
	function td(texto) { var celula = document.createElement("td"); celula.textContent = texto || "—"; return celula; }

	function iniciar() {
		if (!el("conta-form")) { return; }
		if (el("conta-form").dataset.inicializado === "true") { return; }
		el("conta-form").dataset.inicializado = "true";
		el("conta-data-saldo").max = hoje();
		el("contas-nova").addEventListener("click", function () { abrirConta(null); });
		el("conta-cancelar").addEventListener("click", fecharConta);
		el("conta-form").addEventListener("submit", salvarConta);
		el("conta-tipo").addEventListener("change", aplicarPadraoTipo);
		el("conta-nova-instituicao").addEventListener("click", abrirInstituicao);
		el("instituicao-cancelar").addEventListener("click", fecharInstituicao);
		el("instituicao-form").addEventListener("submit", salvarInstituicao);
		el("conta-novo-titular").addEventListener("click", abrirTitular);
		el("titular-cancelar").addEventListener("click", fecharTitular);
		el("titular-form").addEventListener("submit", salvarTitular);
		["contas-filtro-titular", "contas-filtro-instituicao", "contas-filtro-tipo", "contas-filtro-status"]
			.forEach(function (id) { el(id).addEventListener("change", carregarContas); });
		el("contas-busca").addEventListener("input", carregarContas);
		Promise.all([carregarTitulares(), carregarInstituicoes()]).then(function () { carregarContas(); carregarResumo(); });
	}

	function carregarTitulares(selecionado) {
		return window.FinanceiroApi.contas.titulares().then(function (resposta) {
			preencherSelects("titular", resposta.data || []);
			if (selecionado) { el("conta-titular").value = selecionado; }
		});
	}

	function carregarInstituicoes(selecionada) {
		return window.FinanceiroApi.contas.instituicoes().then(function (resposta) {
			preencherSelects("instituicao", resposta.data || []);
			if (selecionada) { el("conta-instituicao").value = selecionada; }
		});
	}

	function preencherSelects(tipo, itens) {
		var ids = tipo === "titular" ? ["contas-filtro-titular", "conta-titular"] : ["contas-filtro-instituicao", "conta-instituicao"];
		ids.forEach(function (id) {
			var select = el(id); var valor = select.value; var primeiro = select.options[0].cloneNode(true);
			select.innerHTML = ""; select.appendChild(primeiro);
			itens.forEach(function (item) { var opcao = document.createElement("option"); opcao.value = item.id; opcao.textContent = item.nome; select.appendChild(opcao); });
			select.value = valor;
		});
	}

	function carregarContas() {
		el("contas-carregando").hidden = false; el("contas-erro").hidden = true; el("contas-vazio").hidden = true; el("contas-tabela-wrap").hidden = true;
		window.FinanceiroApi.contas.listar({
			busca: el("contas-busca").value, titularId: el("contas-filtro-titular").value,
			instituicaoId: el("contas-filtro-instituicao").value, tipo: el("contas-filtro-tipo").value,
			status: el("contas-filtro-status").value
		}).then(function (resposta) {
			el("contas-carregando").hidden = true; var contas = resposta.data || [];
			if (!contas.length) { el("contas-vazio").hidden = false; return; }
			el("contas-tabela-wrap").hidden = false; renderizar(contas);
		}).catch(function () { el("contas-carregando").hidden = true; el("contas-erro").hidden = false; });
	}

	function carregarResumo() {
		window.FinanceiroApi.contas.resumo().then(function (resposta) {
			var resumo = resposta.data;
			el("contas-resumo-quantidade").textContent = resumo.quantidadeContasAtivas;
			window.FinanceiroFormatacao.renderMoeda(
				el("contas-resumo-saldo"),
				resumo.saldoInicialConsolidado,
				"SALDO"
			);
			el("contas-resumo-titulares").textContent = (resumo.saldosPorTitular || []).map(function (item) {
				return item.titularNome + ": " + window.FinanceiroFormatacao.moeda(item.saldoInicial);
			}).join(" · ") || "Sem contas ativas";
		}).catch(function () { el("contas-resumo").hidden = true; });
	}

	function renderizar(contas) {
		var corpo = el("contas-tbody"); corpo.innerHTML = "";
		contas.forEach(function (conta) {
			var linha = document.createElement("tr"); linha.appendChild(td(conta.nome)); linha.appendChild(td(conta.titularNome));
			linha.appendChild(td(conta.instituicaoNome)); linha.appendChild(td(ROTULOS[conta.tipo] || conta.tipo));
			var saldo = td(window.FinanceiroFormatacao.moeda(conta.saldoInicial));
			window.FinanceiroFormatacao.aplicarSemantica(saldo, conta.saldoInicial, "SALDO");
			linha.appendChild(saldo); linha.appendChild(td(conta.dataSaldoInicial));
			linha.appendChild(td(conta.permiteConciliacao ? "Permitida" : "Não permitida"));
			var status = td(""); var badge = document.createElement("span"); badge.className = "criati-badge criati-badge-" + conta.status.toLowerCase(); badge.textContent = conta.status === "ATIVO" ? "Ativa" : "Inativa"; status.appendChild(badge); linha.appendChild(status);
			var acoes = td(""); acoes.className = "criati-table-acoes";
			var detalhes = botao("Detalhes", function () { window.FinanceiroApi.contas.buscar(conta.id).then(function (resposta) { abrirConta(resposta.data); }); });
			var editar = botao("Editar", function () { abrirConta(conta); });
			var alternar = botao(conta.status === "ATIVO" ? "Desativar" : "Reativar", function () { alternarStatus(conta); });
			acoes.appendChild(detalhes); acoes.appendChild(editar); acoes.appendChild(alternar); linha.appendChild(acoes); corpo.appendChild(linha);
		});
	}

	function botao(rotulo, acao) { var item = document.createElement("button"); item.type = "button"; item.className = "criati-btn criati-btn-ghost"; item.textContent = rotulo; item.addEventListener("click", acao); return item; }

	function abrirConta(conta) {
		contaEmEdicaoId = conta ? conta.id : null; el("conta-modal-titulo").textContent = conta ? "Detalhes e edição da conta" : "Nova conta";
		el("conta-nome").value = conta ? conta.nome : ""; el("conta-titular").value = conta && conta.titularId ? conta.titularId : "";
		el("conta-tipo").value = conta ? conta.tipo : "CONTA_CORRENTE"; el("conta-instituicao").value = conta && conta.instituicaoId ? conta.instituicaoId : "";
		el("conta-moeda").value = "BRL"; el("conta-saldo").value = conta ? conta.saldoInicial : "0.00"; el("conta-data-saldo").value = conta ? conta.dataSaldoInicial : hoje();
		var dinheiro = el("conta-tipo").value === "DINHEIRO" || el("conta-tipo").value === "CARTEIRA";
		el("conta-conciliacao").disabled = dinheiro;
		el("conta-conciliacao").checked = conta ? conta.permiteConciliacao : !dinheiro;
		el("conta-modal").hidden = false; el("conta-nome").focus();
	}

	function fecharConta() { el("conta-modal").hidden = true; el("conta-form").reset(); contaEmEdicaoId = null; }
	function aplicarPadraoTipo() { var tipo = el("conta-tipo").value; var dinheiro = tipo === "DINHEIRO" || tipo === "CARTEIRA"; el("conta-conciliacao").checked = !dinheiro; el("conta-conciliacao").disabled = dinheiro; if (dinheiro) { el("conta-instituicao").value = ""; } }

	function salvarConta(evento) {
		evento.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("conta-form"))) { return; }
		var botaoSalvar = el("conta-salvar"); var dados = {
			nome: el("conta-nome").value, titularId: el("conta-titular").value, instituicaoId: el("conta-instituicao").value || null,
			tipo: el("conta-tipo").value, moeda: "BRL", saldoInicial: el("conta-saldo").value,
			dataSaldoInicial: el("conta-data-saldo").value, permiteConciliacao: el("conta-conciliacao").checked
		};
		window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		var chamada = contaEmEdicaoId ? window.FinanceiroApi.contas.editar(contaEmEdicaoId, dados) : window.FinanceiroApi.contas.criar(dados);
		chamada.then(function (resposta) {
			var mensagem = resposta.data.possivelDuplicidade ? "Conta salva. Há outra conta ativa com o mesmo nome." : "Conta salva com sucesso.";
			window.CriatiUI.showToast("sucesso", mensagem); fecharConta(); carregarContas(); carregarResumo();
		}).catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Não foi possível salvar a conta."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botaoSalvar, false); });
	}

	function alternarStatus(conta) {
		if (!window.confirm((conta.status === "ATIVO" ? "Desativar " : "Reativar ") + conta.nome + "?")) { return; }
		var chamada = conta.status === "ATIVO" ? window.FinanceiroApi.contas.inativar(conta.id) : window.FinanceiroApi.contas.reativar(conta.id);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Conta atualizada com sucesso."); carregarContas(); carregarResumo(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Não foi possível atualizar a conta."); });
	}

	function abrirTitular() { el("titular-modal").hidden = false; el("titular-nome").focus(); }
	function fecharTitular() { el("titular-modal").hidden = true; el("titular-form").reset(); }
	function salvarTitular(evento) {
		evento.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("titular-form"))) { return; }
		var botaoSalvar = el("titular-salvar"); window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		window.FinanceiroApi.pessoas.criar({ nome: el("titular-nome").value, apelido: el("titular-apelido").value || null })
			.then(function (resposta) { fecharTitular(); return carregarTitulares(resposta.data.id); })
			.then(function () { window.CriatiUI.showToast("sucesso", "Titular cadastrado com sucesso."); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Não foi possível cadastrar o titular."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botaoSalvar, false); });
	}

	function abrirInstituicao() { el("instituicao-modal").hidden = false; el("instituicao-nome").focus(); }
	function fecharInstituicao() { el("instituicao-modal").hidden = true; el("instituicao-form").reset(); }
	function salvarInstituicao(evento) {
		evento.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("instituicao-form"))) { return; }
		var botaoSalvar = el("instituicao-salvar"); window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		window.FinanceiroApi.contas.criarInstituicao({ nome: el("instituicao-nome").value, codigo: el("instituicao-codigo").value })
			.then(function (resposta) { fecharInstituicao(); return carregarInstituicoes(resposta.data.id); })
			.then(function () { window.CriatiUI.showToast("sucesso", "Instituição cadastrada com sucesso."); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Não foi possível cadastrar a instituição."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botaoSalvar, false); });
	}

	window.FinanceiroContas = { iniciar: iniciar };
})(window, document);
