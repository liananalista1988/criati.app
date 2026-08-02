/* Criati Financeiro - CRUD de regras de classificacao de importacao bancaria
   (CRIATI-IMP-002A). Regras nunca criam lancamento, pagamento de fatura ou
   compromisso sozinhas - a aplicacao delas durante a revisao da importacao
   (financeiro-importacoes.js) sempre exige confirmacao explicita do usuario. */
(function (window, document) {
	"use strict";

	var TIPO_LABEL = { RECEITA: "Receita", DESPESA: "Despesa" };
	var APLICACAO_LABEL = { AUTOMATICA: "Automática", SUGESTAO: "Sugestão" };
	var regraEmEdicao = null;
	var categoriasCache = [];

	function el(id) { return document.getElementById(id); }
	function td(texto) { var celula = document.createElement("td"); celula.textContent = texto || "—"; return celula; }

	function iniciar() {
		if (!el("regra-form")) { return; }
		if (el("regra-form").dataset.inicializado === "true") { return; }
		el("regra-form").dataset.inicializado = "true";
		el("regras-nova").addEventListener("click", function () { abrir(null); });
		el("regra-cancelar").addEventListener("click", fechar);
		el("regra-form").addEventListener("submit", salvar);
		el("regra-tipo").addEventListener("change", function () { preencherCategoriasPorTipo(el("regra-tipo").value); });
		el("regras-filtro-status").addEventListener("change", carregar);
		Promise.all([carregarContas(), carregarPessoas(), carregarCategorias()]).then(carregar);
	}

	function carregarContas() {
		return window.FinanceiroApi.contas.listar({ status: "ATIVO" }).then(function (resposta) {
			var select = el("regra-conta"); var contas = resposta.data || [];
			contas.forEach(function (conta) { var opcao = document.createElement("option"); opcao.value = conta.id; opcao.textContent = conta.nome; select.appendChild(opcao); });
		});
	}

	function carregarPessoas() {
		return window.FinanceiroApi.pessoas.listar({ status: "ATIVO" }).then(function (resposta) {
			var select = el("regra-pessoa"); var pessoas = resposta.data || [];
			pessoas.forEach(function (pessoa) { var opcao = document.createElement("option"); opcao.value = pessoa.id; opcao.textContent = pessoa.nome; select.appendChild(opcao); });
		});
	}

	function carregarCategorias() {
		return window.FinanceiroApi.categorias.listar({ status: "ATIVO" }).then(function (resposta) {
			categoriasCache = resposta.data || [];
		});
	}

	function preencherCategoriasPorTipo(tipo, selecionada) {
		var select = el("regra-categoria"); select.innerHTML = "";
		categoriasCache.filter(function (c) { return c.tipo === tipo; }).forEach(function (categoria) {
			var opcao = document.createElement("option"); opcao.value = categoria.id; opcao.textContent = categoria.nome; select.appendChild(opcao);
		});
		if (selecionada) { select.value = selecionada; }
	}

	function carregar() {
		el("regras-carregando").hidden = false; el("regras-erro").hidden = true;
		el("regras-vazio").hidden = true; el("regras-tabela-wrap").hidden = true;
		window.FinanceiroApi.regrasImportacao.listar({ status: el("regras-filtro-status").value }).then(function (resposta) {
			el("regras-carregando").hidden = true;
			var regras = resposta.data || [];
			if (!regras.length) { el("regras-vazio").hidden = false; return; }
			el("regras-tabela-wrap").hidden = false; renderizar(regras);
		}).catch(function () { el("regras-carregando").hidden = true; el("regras-erro").hidden = false; });
	}

	function renderizar(regras) {
		var corpo = el("regras-tbody"); corpo.innerHTML = "";
		regras.forEach(function (regra) {
			var linha = document.createElement("tr");
			linha.appendChild(td(regra.padraoNormalizado));
			linha.appendChild(td(TIPO_LABEL[regra.tipo] || regra.tipo));
			linha.appendChild(td(regra.categoriaNome));
			linha.appendChild(td(regra.contaNome || "Qualquer conta"));
			linha.appendChild(td(APLICACAO_LABEL[regra.aplicacao] || regra.aplicacao));
			linha.appendChild(td(String(regra.quantidadeUtilizacoes)));
			var status = td(""); var badge = document.createElement("span");
			badge.className = "criati-badge criati-badge-" + regra.status.toLowerCase();
			badge.textContent = regra.status === "ATIVO" ? "Ativa" : "Inativa";
			status.appendChild(badge); linha.appendChild(status);
			var acoes = td(""); acoes.className = "criati-table-acoes";
			var detalhes = botao("Detalhes", function () { abrirDetalhes(regra); });
			var editar = botao("Editar", function () { abrir(regra); });
			var alternar = botao(regra.status === "ATIVO" ? "Inativar" : "Reativar", function () { alternarStatus(regra); });
			acoes.appendChild(detalhes); acoes.appendChild(editar); acoes.appendChild(alternar);
			linha.appendChild(acoes); corpo.appendChild(linha);
		});
	}

	function botao(rotulo, acao) {
		var item = document.createElement("button"); item.type = "button"; item.className = "criati-btn criati-btn-ghost";
		item.textContent = rotulo; item.addEventListener("click", acao); return item;
	}

	function abrirDetalhes(regra) {
		window.FinanceiroDetalhes.abrir("Detalhes da regra", [
			["Descrição de referência", regra.descricaoReferencia],
			["Padrão normalizado", regra.padraoNormalizado],
			["Estratégia", regra.estrategiaComparacao],
			["Natureza", TIPO_LABEL[regra.tipo] || regra.tipo],
			["Categoria", regra.categoriaNome],
			["Conta", regra.contaNome || "Qualquer conta"],
			["Pessoa", regra.pessoaFinanceiraNome],
			["Forma de pagamento", regra.formaPagamento],
			["Encaminhamento sugerido", regra.encaminhamentoSugerido],
			["Nível de confiança", regra.nivelConfianca],
			["Prioridade", regra.prioridade],
			["Aplicação", APLICACAO_LABEL[regra.aplicacao] || regra.aplicacao],
			["Quantidade de utilizações", regra.quantidadeUtilizacoes],
			["Última utilização", regra.ultimaUtilizacaoEm ? window.FinanceiroFormatacao.dataBr(regra.ultimaUtilizacaoEm.slice(0, 10)) : "Nunca utilizada"],
			["Status", regra.status === "ATIVO" ? "Ativa" : "Inativa"]
		]);
	}

	function abrir(regra) {
		regraEmEdicao = regra;
		el("regra-modal-titulo").textContent = regra ? "Editar regra" : "Nova regra";
		el("regra-descricao-referencia").value = regra ? regra.descricaoReferencia : "";
		el("regra-padrao").value = regra ? regra.padraoNormalizado : "";
		el("regra-estrategia").value = regra ? regra.estrategiaComparacao : "CONTEM";
		el("regra-tipo").value = regra ? regra.tipo : "DESPESA";
		preencherCategoriasPorTipo(el("regra-tipo").value, regra ? regra.categoriaId : "");
		el("regra-conta").value = regra && regra.contaId ? regra.contaId : "";
		el("regra-pessoa").value = regra && regra.pessoaFinanceiraId ? regra.pessoaFinanceiraId : "";
		el("regra-forma").value = regra && regra.formaPagamento ? regra.formaPagamento : "";
		el("regra-encaminhamento").value = regra && regra.encaminhamentoSugerido ? regra.encaminhamentoSugerido : "";
		el("regra-confianca").value = regra ? regra.nivelConfianca : "MEDIA";
		el("regra-prioridade").value = regra ? regra.prioridade : 0;
		el("regra-aplicacao").value = regra ? regra.aplicacao : "SUGESTAO";
		el("regra-modal").hidden = false; el("regra-descricao-referencia").focus();
	}

	function fechar() { el("regra-modal").hidden = true; el("regra-form").reset(); regraEmEdicao = null; }

	function salvar(evento) {
		evento.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("regra-form"))) { return; }
		// Mesma regra do backend (ajuste obrigatorio 2), verificada aqui so
		// para dar um erro claro sem round-trip - o backend e sempre quem
		// decide de fato (RegraClassificacaoImportacaoService).
		if (el("regra-aplicacao").value === "AUTOMATICA" && el("regra-estrategia").value === "CONTEM") {
			window.CriatiUI.showToast("erro", "Estratégia Contém não pode ser usada com aplicação Automática. Use Sugestão ou troque para Igual/Prefixo.");
			return;
		}
		var botao = el("regra-salvar");
		var dados = {
			contaId: el("regra-conta").value || null,
			categoriaId: el("regra-categoria").value,
			pessoaFinanceiraId: el("regra-pessoa").value || null,
			descricaoReferencia: el("regra-descricao-referencia").value,
			padrao: el("regra-padrao").value || null,
			estrategiaComparacao: el("regra-estrategia").value,
			prioridade: Number(el("regra-prioridade").value) || 0,
			tipo: el("regra-tipo").value,
			formaPagamento: el("regra-forma").value || null,
			encaminhamentoSugerido: el("regra-encaminhamento").value || null,
			nivelConfianca: el("regra-confianca").value,
			aplicacao: el("regra-aplicacao").value
		};
		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		var chamada = regraEmEdicao ? window.FinanceiroApi.regrasImportacao.editar(regraEmEdicao.id, dados)
			: window.FinanceiroApi.regrasImportacao.criar(dados);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Regra salva com sucesso."); fechar(); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível salvar a regra."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botao, false); });
	}

	function alternarStatus(regra) {
		var acao = regra.status === "ATIVO" ? "Inativar" : "Reativar";
		if (!window.confirm(acao + " esta regra? Transações já classificadas por ela mantêm o histórico.")) { return; }
		var chamada = regra.status === "ATIVO" ? window.FinanceiroApi.regrasImportacao.inativar(regra.id)
			: window.FinanceiroApi.regrasImportacao.reativar(regra.id);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Regra atualizada com sucesso."); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível atualizar a regra."); });
	}

	window.FinanceiroRegrasImportacao = { iniciar: iniciar };
})(window, document);
