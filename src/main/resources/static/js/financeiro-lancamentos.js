/* Criati Financeiro - lançamentos básicos, saldo derivado e filtros tenant-aware. */
(function (window, document) {
	"use strict";
	var STATUS_LABEL = { PENDENTE: "Pendente", LIQUIDADO: "Liquidado", PAGO: "Pago (legado)", CANCELADO: "Cancelado" };
	var lancamentoEmEdicao = null, lancamentoParaLiquidar = null;
	var contas = [], categorias = [], pessoas = [], partes = [];
	var modalLancamento, modalLiquidacao;
	function el(id) { return document.getElementById(id); }
	function hoje() { return new Date().toISOString().slice(0, 10); }

	function iniciar() {
		if (!el("financeiro-lancamento-form")) return;
		if (el("financeiro-lancamento-form").dataset.inicializado === "true") return;
		el("financeiro-lancamento-form").dataset.inicializado = "true";
		modalLancamento = window.CriatiUI.criarModal(el("financeiro-lancamento-modal"));
		modalLiquidacao = window.CriatiUI.criarModal(el("financeiro-pagar-modal"));
		el("financeiro-lancamentos-nova-receita").addEventListener("click", function () { abrirCriacao("RECEITA"); });
		el("financeiro-lancamentos-nova-despesa").addEventListener("click", function () { abrirCriacao("DESPESA"); });
		el("financeiro-lancamento-cancelar").addEventListener("click", fecharFormulario);
		el("financeiro-lancamento-form").addEventListener("submit", salvar);
		el("financeiro-pagar-cancelar").addEventListener("click", fecharLiquidacao);
		el("financeiro-pagar-form").addEventListener("submit", confirmarLiquidacao);
		el("financeiro-lancamentos-filtros").addEventListener("submit", function (evento) {
			evento.preventDefault();
			carregar();
		});
		el("financeiro-lancamentos-limpar-filtros").addEventListener("click", limparFiltros);
		carregarAuxiliares().then(carregar);
	}

	function limparFiltros() {
		el("financeiro-lancamentos-filtros").reset();
		el("financeiro-lancamentos-filtros").dispatchEvent(new Event("input", { bubbles: true }));
		carregar();
	}
	function preencher(select, itens, texto) {
		select.innerHTML = "";
		if (texto !== null) { var vazio = document.createElement("option"); vazio.value = ""; vazio.textContent = texto; select.appendChild(vazio); }
		itens.forEach(function (item) { var o = document.createElement("option"); o.value = item.id; o.textContent = item.nome; select.appendChild(o); });
	}
	function carregarAuxiliares() {
		return Promise.all([window.FinanceiroApi.contas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.categorias.listar({ status: "ATIVO" }),
			window.FinanceiroApi.pessoas.listar({ status: "ATIVO" }), window.FinanceiroApi.contatos.listar({ status: "ATIVO" })])
			.then(function (r) {
				contas = r[0].data || []; categorias = r[1].data || []; pessoas = r[2].data || []; partes = r[3].data || [];
				preencher(el("financeiro-filtro-conta"), contas, "Todas as contas");
				preencher(el("financeiro-filtro-categoria"), categorias, "Todas as categorias");
				preencher(el("financeiro-filtro-pessoa"), pessoas, "Todas as pessoas");
				preencher(el("financeiro-filtro-parte"), partes, "Todos os contatos");
				preencher(el("financeiro-lancamento-conta"), contas, null);
				preencher(el("financeiro-lancamento-pessoa"), pessoas, null);
				preencher(el("financeiro-lancamento-parte"), partes, "Sem contato");
			});
	}

	function filtros() {
		return { dataInicial: el("financeiro-filtro-data-inicial").value, dataFinal: el("financeiro-filtro-data-final").value,
			tipo: el("financeiro-filtro-tipo").value, status: el("financeiro-filtro-status").value,
			contaId: el("financeiro-filtro-conta").value, categoriaId: el("financeiro-filtro-categoria").value,
			pessoaId: el("financeiro-filtro-pessoa").value, parteId: el("financeiro-filtro-parte").value,
			vencido: el("financeiro-filtro-vencido").value, busca: el("financeiro-filtro-busca").value };
	}
	function carregar() {
		el("financeiro-lancamentos-carregando").hidden = false; el("financeiro-lancamentos-erro").hidden = true;
		el("financeiro-lancamentos-vazio").hidden = true; el("financeiro-lancamentos-tabela-wrap").hidden = true;
		var f = filtros(); var competencia = (f.dataInicial || hoje()).slice(0, 7);
		Promise.all([window.FinanceiroApi.lancamentos.listar(f), window.FinanceiroApi.lancamentos.resumo({
			competencia: competencia, pessoaId: f.pessoaId, contaId: f.contaId })]).then(function (r) {
			el("financeiro-lancamentos-carregando").hidden = true; renderResumo(r[1].data || {});
			var itens = r[0].data || [];
			el("resumo-quantidade-lancamentos").textContent = String(itens.length);
			if (!itens.length) { el("financeiro-lancamentos-vazio").hidden = false; return; }
			el("financeiro-lancamentos-tabela-wrap").hidden = false; renderTabela(itens);
		}).catch(function () { el("financeiro-lancamentos-carregando").hidden = true; el("financeiro-lancamentos-erro").hidden = false; });
	}
	function renderResumo(r) {
		[["receitas-liquidadas", r.receitasLiquidadas, "ENTRADA"], ["despesas-liquidadas", r.despesasLiquidadas, "SAIDA"],
		["resultado-liquidado", r.resultadoLiquidado, "SALDO"], ["saldo-consolidado", r.saldoConsolidado, "SALDO"],
		["receitas-pendentes", r.receitasPendentes, "ENTRADA"], ["despesas-pendentes", r.despesasPendentes, "SAIDA"]]
			.forEach(function (item) {
				window.FinanceiroFormatacao.renderMoeda(el("resumo-" + item[0]), item[1] || 0, item[2]);
			});
		el("resumo-vencidos").textContent = r.quantidadeVencidos || 0;
	}
	function renderTabela(itens) { var tbody = el("financeiro-lancamentos-tbody"); tbody.innerHTML = "";
		itens.forEach(function (l) { tbody.appendChild(linha(l)); }); }
	function celula(texto, rotulo) {
		var td = document.createElement("td");
		td.textContent = texto || "—";
		if (rotulo) td.dataset.label = rotulo;
		return td;
	}
	function linha(l) {
		var tr = document.createElement("tr"); tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataCompetencia), "Competência"));
		tr.appendChild(celula(l.descricao, "Descrição")); tr.appendChild(celula(l.categoriaNome, "Categoria")); tr.appendChild(celula(l.contaNome, "Conta"));
		tr.appendChild(celula(l.pessoaFinanceiraNome, "Pessoa")); tr.appendChild(celula(l.tipo === "RECEITA" ? "Receita" : "Despesa", "Tipo"));
		var valor = celula((l.tipo === "RECEITA" ? "+ " : "- ") + window.FinanceiroFormatacao.moeda(l.valor));
		valor.dataset.label = "Valor"; valor.className = l.tipo === "RECEITA" ? "criati-valor-positivo" : "criati-valor-negativo"; tr.appendChild(valor);
		var status = document.createElement("td"), badge = document.createElement("span");
		status.dataset.label = "Status";
		// Vencido e calculado (pendente + vencimento no passado), nao um status
		// persistido: prevalece sobre a cor do status normal para que a
		// situacao fique visivelmente vermelha, nunca so o texto do badge.
		badge.className = "criati-badge criati-badge-" + (l.vencido ? "vencido" : l.status.toLowerCase());
		badge.textContent = l.vencido ? "Vencido" : (STATUS_LABEL[l.status] || l.status); status.appendChild(badge); tr.appendChild(status);
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataVencimento), "Vencimento"));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataLiquidacao), "Liquidação"));
		tr.appendChild(acoes(l)); return tr;
	}
	function botao(texto, fn, classe) { var b = document.createElement("button"); b.type = "button"; b.className = classe || "criati-btn criati-btn-ghost"; b.textContent = texto; b.addEventListener("click", fn); return b; }
	function acoes(l) { var td = document.createElement("td"); td.className = "criati-table-acoes"; td.dataset.label = "Ações";
		td.appendChild(botao("Detalhes", function () { window.FinanceiroDetalhes.abrir("Detalhes do lançamento", [
			["Descrição", l.descricao], ["Natureza", l.tipo === "RECEITA" ? "Receita" : "Despesa"],
			["Valor", window.FinanceiroFormatacao.moeda(l.valor)], ["Competência", window.FinanceiroFormatacao.dataBr(l.dataCompetencia)],
			["Vencimento", window.FinanceiroFormatacao.dataBr(l.dataVencimento)], ["Liquidação", window.FinanceiroFormatacao.dataBr(l.dataLiquidacao)],
			["Conta", l.contaNome], ["Categoria", l.categoriaNome], ["Pessoa", l.pessoaFinanceiraNome],
			["Forma de pagamento", l.formaPagamento], ["Origem", l.origem], ["Observação", l.observacao],
			["Status", l.vencido ? "Vencido" : (STATUS_LABEL[l.status] || l.status)]
		]); }));
		if (l.status === "PENDENTE") { td.appendChild(botao("Editar", function () { abrirEdicao(l); })); td.appendChild(botao("Liquidar", function () { abrirLiquidacao(l); }, "criati-btn criati-btn-primary")); }
		if (l.status === "LIQUIDADO" || l.status === "PAGO") { td.appendChild(botao("Editar", function () { abrirEdicao(l); })); td.appendChild(botao("Desliquidar", function () { desliquidar(l); })); }
		if (l.status !== "CANCELADO") td.appendChild(botao("Cancelar", function () { cancelar(l); }, "criati-btn criati-btn-danger")); return td; }
	function cancelar(l) { if (!confirm("Cancelar removerá qualquer impacto deste lançamento no saldo. Continuar?")) return;
		window.FinanceiroApi.lancamentos.cancelar(l.id).then(sucesso("Lançamento cancelado.")).catch(falha("Não foi possível cancelar.")); }
	function desliquidar(l) { if (!confirm("Desliquidar removerá o impacto no saldo. Continuar?")) return;
		window.FinanceiroApi.lancamentos.desliquidar(l.id).then(sucesso("Lançamento desliquidado.")).catch(falha("Não foi possível desliquidar.")); }
	function sucesso(msg) { return function () { window.CriatiUI.showToast("sucesso", msg); carregar(); }; }
	function falha(msg) { return function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || msg); }; }

	function abrirLiquidacao(l) { lancamentoParaLiquidar = l; el("financeiro-pagar-data-label").textContent = rotuloDataPorTipo(l.tipo); el("financeiro-pagar-data").value = hoje(); el("financeiro-pagar-forma").value = l.formaPagamento || ""; modalLiquidacao.abrir(el("financeiro-pagar-data")); }
	function fecharLiquidacao() { modalLiquidacao.fechar(); lancamentoParaLiquidar = null; }
	function confirmarLiquidacao(e) { e.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("financeiro-pagar-form"))) { return; }
		var b = el("financeiro-pagar-confirmar"); window.CriatiUI.setButtonLoading(b, true, "Confirmando...");
		window.FinanceiroApi.lancamentos.liquidar(lancamentoParaLiquidar.id, { dataLiquidacao: el("financeiro-pagar-data").value,
			formaPagamento: el("financeiro-pagar-forma").value || null }).then(function () { fecharLiquidacao(); sucesso("Lançamento liquidado.")(); })
			.catch(falha("Não foi possível liquidar.")).finally(function () { window.CriatiUI.setButtonLoading(b, false); }); }

	function preencherCategorias(tipo) { preencher(el("financeiro-lancamento-categoria"), categorias.filter(function (c) { return c.tipo === tipo; }), null); }
	function rotuloDataPorTipo(tipo) { return tipo === "RECEITA" ? "Data do recebimento" : tipo === "DESPESA" ? "Data do pagamento" : "Data do recebimento ou pagamento"; }
	function atualizarRotulosData(tipo) {
		el("financeiro-lancamento-data-liquidacao-label").textContent = rotuloDataPorTipo(tipo);
		el("financeiro-pagar-data-label").textContent = rotuloDataPorTipo(tipo);
	}
	function abrirCriacao(tipo) { lancamentoEmEdicao = null; el("financeiro-lancamento-modal-titulo").textContent = tipo === "RECEITA" ? "Nova receita" : "Nova despesa";
		el("financeiro-lancamento-tipo").value = tipo; el("financeiro-lancamento-descricao").value = ""; el("financeiro-lancamento-valor").value = "";
		el("financeiro-lancamento-data-competencia").value = hoje(); el("financeiro-lancamento-data-vencimento").value = "";
		el("financeiro-lancamento-data-liquidacao").value = ""; el("financeiro-lancamento-status").value = "PENDENTE";
		el("financeiro-lancamento-forma").value = ""; el("financeiro-lancamento-observacao").value = ""; atualizarRotulosData(tipo); preencherCategorias(tipo); modalLancamento.abrir(el("financeiro-lancamento-conta")); }
	function abrirEdicao(l) { lancamentoEmEdicao = l; el("financeiro-lancamento-modal-titulo").textContent = "Editar lançamento";
		el("financeiro-lancamento-tipo").value = l.tipo; atualizarRotulosData(l.tipo); preencherCategorias(l.tipo); el("financeiro-lancamento-conta").value = l.contaId;
		el("financeiro-lancamento-categoria").value = l.categoriaId; el("financeiro-lancamento-pessoa").value = l.pessoaFinanceiraId || "";
		el("financeiro-lancamento-parte").value = l.parteFinanceiraId || ""; el("financeiro-lancamento-descricao").value = l.descricao;
		el("financeiro-lancamento-valor").value = l.valor; el("financeiro-lancamento-data-competencia").value = l.dataCompetencia;
		el("financeiro-lancamento-data-vencimento").value = l.dataVencimento || ""; el("financeiro-lancamento-status").value = l.status === "PAGO" ? "LIQUIDADO" : l.status;
		el("financeiro-lancamento-data-liquidacao").value = l.dataLiquidacao || ""; el("financeiro-lancamento-forma").value = l.formaPagamento || "";
		el("financeiro-lancamento-observacao").value = l.observacao || ""; modalLancamento.abrir(el("financeiro-lancamento-conta")); }
	function fecharFormulario() { modalLancamento.fechar(); el("financeiro-lancamento-form").reset(); lancamentoEmEdicao = null; }
	function salvar(e) { e.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("financeiro-lancamento-form"))) { return; }
		var b = el("financeiro-lancamento-salvar"), tipo = el("financeiro-lancamento-tipo").value;
		var d = { contaId: el("financeiro-lancamento-conta").value, categoriaId: el("financeiro-lancamento-categoria").value,
			pessoaFinanceiraId: el("financeiro-lancamento-pessoa").value, parteFinanceiraId: el("financeiro-lancamento-parte").value || null,
			tipo: tipo, descricao: el("financeiro-lancamento-descricao").value, valor: el("financeiro-lancamento-valor").value,
			dataCompetencia: el("financeiro-lancamento-data-competencia").value, dataVencimento: el("financeiro-lancamento-data-vencimento").value || null,
			dataLiquidacao: el("financeiro-lancamento-data-liquidacao").value || null, formaPagamento: el("financeiro-lancamento-forma").value || null,
			observacao: el("financeiro-lancamento-observacao").value };
		if (!lancamentoEmEdicao) d.status = el("financeiro-lancamento-status").value;
		window.CriatiUI.setButtonLoading(b, true, "Salvando..."); var chamada = lancamentoEmEdicao ? window.FinanceiroApi.lancamentos.editar(lancamentoEmEdicao.id, d) : window.FinanceiroApi.lancamentos.criar(d);
		chamada.then(function () { fecharFormulario(); sucesso("Lançamento salvo.")(); }).catch(falha("Não foi possível salvar.")).finally(function () { window.CriatiUI.setButtonLoading(b, false); }); }
	window.FinanceiroLancamentos = { iniciar: iniciar };
})(window, document);
