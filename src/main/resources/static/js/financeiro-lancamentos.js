/* Criati Financeiro - lançamentos básicos, saldo derivado e filtros tenant-aware. */
(function (window, document) {
	"use strict";
	var STATUS_LABEL = { PENDENTE: "Pendente", LIQUIDADO: "Liquidado", PAGO: "Pago (legado)", CANCELADO: "Cancelado" };
	var lancamentoEmEdicao = null, lancamentoParaLiquidar = null;
	var contas = [], categorias = [], pessoas = [], partes = [];
	var paginaAtual = 0, totalPaginas = 0, ordenarPor = "competencia", direcao = "desc";
	var podeEscrever = false, podeDesliquidar = false;
	var PARAMETROS_FILTRO = ["competenciaInicial", "competenciaFinal", "descricao", "categoriaId", "contaId",
		"pessoaId", "tipo", "situacao", "valorMinimo", "valorMaximo", "liquidacaoInicial", "liquidacaoFinal"];
	var modalLancamento, modalLiquidacao;
	function el(id) { return document.getElementById(id); }
	function hoje() { return new Date().toISOString().slice(0, 10); }

	function iniciar() {
		if (!el("financeiro-lancamento-form")) return;
		if (el("financeiro-lancamento-form").dataset.inicializado === "true") return;
		el("financeiro-lancamento-form").dataset.inicializado = "true";
		var conteudo = document.querySelector("main.criati-content");
		podeEscrever = conteudo && conteudo.dataset.podeEscrever === "true";
		podeDesliquidar = conteudo && conteudo.dataset.podeDesliquidar === "true";
		modalLancamento = window.CriatiUI.criarModal(el("financeiro-lancamento-modal"));
		modalLiquidacao = window.CriatiUI.criarModal(el("financeiro-pagar-modal"));
		if (el("financeiro-lancamentos-nova-receita")) el("financeiro-lancamentos-nova-receita").addEventListener("click", function () { abrirCriacao("RECEITA"); });
		if (el("financeiro-lancamentos-nova-despesa")) el("financeiro-lancamentos-nova-despesa").addEventListener("click", function () { abrirCriacao("DESPESA"); });
		el("financeiro-lancamento-cancelar").addEventListener("click", fecharFormulario);
		el("financeiro-lancamento-form").addEventListener("submit", salvar);
		el("financeiro-pagar-cancelar").addEventListener("click", fecharLiquidacao);
		el("financeiro-pagar-form").addEventListener("submit", confirmarLiquidacao);
		el("financeiro-lancamentos-filtros").addEventListener("submit", function (evento) {
			evento.preventDefault();
			paginaAtual = 0; carregar();
		});
		el("financeiro-lancamentos-filtros").addEventListener("change", function () { paginaAtual = 0; carregar(); });
		el("financeiro-lancamentos-limpar-filtros").addEventListener("click", limparFiltros);
		document.querySelectorAll("[data-limpar-filtro]").forEach(function (botao) {
			botao.addEventListener("click", function () { limparFiltro(botao.dataset.limparFiltro); });
		});
		document.querySelectorAll(".financeiro-ordenacao").forEach(function (botao) {
			botao.addEventListener("click", function () {
				direcao = ordenarPor === botao.dataset.ordenar && direcao === "asc" ? "desc" : "asc";
				ordenarPor = botao.dataset.ordenar; paginaAtual = 0; carregar();
			});
		});
		el("financeiro-lancamentos-anterior").addEventListener("click", function () { if (paginaAtual > 0) { paginaAtual--; carregar(); } });
		el("financeiro-lancamentos-proxima").addEventListener("click", function () { if (paginaAtual + 1 < totalPaginas) { paginaAtual++; carregar(); } });
		lerEstadoDaUrl();
		carregarAuxiliares().then(function () { aplicarEstadoDaUrl(); carregar(); });
	}

	function limparFiltros() {
		el("financeiro-lancamentos-filtros").reset();
		paginaAtual = 0; carregar();
	}
	function limparFiltro(grupo) {
		var campos = {
			competencia: ["financeiro-filtro-competencia-inicial", "financeiro-filtro-competencia-final"],
			descricao: ["financeiro-filtro-descricao"], categoria: ["financeiro-filtro-categoria"],
			conta: ["financeiro-filtro-conta"], pessoa: ["financeiro-filtro-pessoa"], tipo: ["financeiro-filtro-tipo"],
			valor: ["financeiro-filtro-valor-minimo", "financeiro-filtro-valor-maximo"], situacao: ["financeiro-filtro-situacao"],
			liquidacao: ["financeiro-filtro-liquidacao-inicial", "financeiro-filtro-liquidacao-final"]
		};
		(campos[grupo] || []).forEach(function (id) { el(id).value = ""; });
		paginaAtual = 0; carregar();
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
				preencherCategoriasAgrupadas(el("financeiro-filtro-categoria"));
				preencher(el("financeiro-filtro-pessoa"), pessoas, "Todas as pessoas");
				preencher(el("financeiro-filtro-parte"), partes, "Todos os contatos");
				preencher(el("financeiro-lancamento-conta"), contas, null);
				preencher(el("financeiro-lancamento-pessoa"), pessoas, null);
				preencher(el("financeiro-lancamento-parte"), partes, "Sem contato");
			});
	}
	function preencherCategoriasAgrupadas(select) {
		select.innerHTML = "";
		var todas = document.createElement("option"); todas.value = ""; todas.textContent = "Todas"; select.appendChild(todas);
		[{ tipo: "RECEITA", rotulo: "Receitas" }, { tipo: "DESPESA", rotulo: "Despesas" }].forEach(function (grupo) {
			var optgroup = document.createElement("optgroup"); optgroup.label = grupo.rotulo;
			optgroup.className = grupo.tipo === "RECEITA" ? "is-receita" : "is-despesa";
			categorias.filter(function (categoria) { return categoria.tipo === grupo.tipo; }).forEach(function (categoria) {
				var opcao = document.createElement("option"); opcao.value = categoria.id;
				opcao.textContent = (categoria.nivel === 2 ? "↳ " : "") + categoria.nome; optgroup.appendChild(opcao);
			});
			if (optgroup.children.length) select.appendChild(optgroup);
		});
	}

	function filtros() {
		var situacao = el("financeiro-filtro-situacao").value;
		return { competenciaInicial: el("financeiro-filtro-competencia-inicial").value,
			competenciaFinal: el("financeiro-filtro-competencia-final").value,
			liquidacaoInicial: el("financeiro-filtro-liquidacao-inicial").value,
			liquidacaoFinal: el("financeiro-filtro-liquidacao-final").value,
			tipo: el("financeiro-filtro-tipo").value, status: situacao === "VENCIDO" ? "" : situacao,
			contaId: el("financeiro-filtro-conta").value, categoriaId: el("financeiro-filtro-categoria").value,
			pessoaId: el("financeiro-filtro-pessoa").value, vencido: situacao === "VENCIDO" ? "true" : "",
			descricao: el("financeiro-filtro-descricao").value,
			valorMinimo: el("financeiro-filtro-valor-minimo").value, valorMaximo: el("financeiro-filtro-valor-maximo").value,
			pagina: paginaAtual, tamanho: 25, ordenarPor: ordenarPor, direcao: direcao };
	}
	function carregar() {
		el("financeiro-lancamentos-carregando").hidden = false; el("financeiro-lancamentos-erro").hidden = true;
		el("financeiro-lancamentos-vazio").hidden = true;
		var f = filtros(); var competencia = (f.competenciaInicial || hoje()).slice(0, 7);
		salvarEstadoNaUrl(f); atualizarOrdenacao(); atualizarPeriodo(f); atualizarFiltrosAtivos(f);
		Promise.all([window.FinanceiroApi.lancamentos.listarPagina(f), window.FinanceiroApi.lancamentos.resumo({
			competencia: competencia, pessoaId: f.pessoaId, contaId: f.contaId })]).then(function (r) {
			el("financeiro-lancamentos-carregando").hidden = true; renderResumo(r[1].data || {});
			var resultado = r[0].data || {}, itens = resultado.itens || [];
			totalPaginas = resultado.totalPaginas || 0;
			el("resumo-quantidade-lancamentos").textContent = String(resultado.totalElementos || 0);
			renderTabela(itens); atualizarPaginacao();
			if (!itens.length) el("financeiro-lancamentos-vazio").hidden = false;
		}).catch(function () { el("financeiro-lancamentos-carregando").hidden = true; el("financeiro-lancamentos-erro").hidden = false; });
	}
	function atualizarPaginacao() {
		var nav = el("financeiro-lancamentos-paginacao"); nav.hidden = totalPaginas <= 1;
		el("financeiro-lancamentos-anterior").disabled = paginaAtual === 0;
		el("financeiro-lancamentos-proxima").disabled = paginaAtual + 1 >= totalPaginas;
		el("financeiro-lancamentos-pagina").textContent = totalPaginas ? "Página " + (paginaAtual + 1) + " de " + totalPaginas : "Nenhum resultado";
	}
	function atualizarOrdenacao() {
		document.querySelectorAll(".financeiro-ordenacao").forEach(function (botao) {
			var ativa = botao.dataset.ordenar === ordenarPor;
			botao.setAttribute("aria-sort", ativa ? (direcao === "asc" ? "ascending" : "descending") : "none");
			botao.classList.toggle("is-active", ativa);
		});
	}
	function atualizarPeriodo(f) {
		var inicio = f.competenciaInicial ? window.FinanceiroFormatacao.dataBr(f.competenciaInicial) : "início";
		var fim = f.competenciaFinal ? window.FinanceiroFormatacao.dataBr(f.competenciaFinal) : "hoje";
		el("financeiro-lancamentos-periodo").textContent = !f.competenciaInicial && !f.competenciaFinal ? "Todos os períodos" : inicio + " a " + fim;
	}
	function atualizarFiltrosAtivos(f) {
		var rotulos = [], situacao = el("financeiro-filtro-situacao").value;
		if (f.competenciaInicial || f.competenciaFinal) rotulos.push("Competência");
		if (f.descricao) rotulos.push("Descrição");
		if (f.categoriaId) rotulos.push("Categoria");
		if (f.contaId) rotulos.push("Conta");
		if (f.pessoaId) rotulos.push("Pessoa");
		if (f.tipo) rotulos.push("Tipo");
		if (f.valorMinimo || f.valorMaximo) rotulos.push("Valor");
		if (situacao) rotulos.push("Situação");
		if (f.liquidacaoInicial || f.liquidacaoFinal) rotulos.push("Recebimento/pagamento");
		el("financeiro-lancamentos-filtros-ativos").textContent = rotulos.length
				? "Filtros ativos: " + rotulos.join(", ") + "." : "Nenhum filtro ativo.";
	}
	var estadoUrl = null;
	function lerEstadoDaUrl() {
		var url = new URL(window.location.href); estadoUrl = url.searchParams;
		paginaAtual = Math.max(0, parseInt(estadoUrl.get("pagina") || "0", 10) || 0);
		ordenarPor = estadoUrl.get("ordenarPor") || "competencia"; direcao = estadoUrl.get("direcao") === "asc" ? "asc" : "desc";
	}
	function aplicarEstadoDaUrl() {
		var mapa = { competenciaInicial: "financeiro-filtro-competencia-inicial", competenciaFinal: "financeiro-filtro-competencia-final",
			descricao: "financeiro-filtro-descricao", categoriaId: "financeiro-filtro-categoria", contaId: "financeiro-filtro-conta",
			pessoaId: "financeiro-filtro-pessoa", tipo: "financeiro-filtro-tipo", situacao: "financeiro-filtro-situacao",
			valorMinimo: "financeiro-filtro-valor-minimo", valorMaximo: "financeiro-filtro-valor-maximo",
			liquidacaoInicial: "financeiro-filtro-liquidacao-inicial", liquidacaoFinal: "financeiro-filtro-liquidacao-final" };
		Object.keys(mapa).forEach(function (parametro) { if (estadoUrl.has(parametro)) el(mapa[parametro]).value = estadoUrl.get(parametro); });
	}
	function salvarEstadoNaUrl(f) {
		var url = new URL(window.location.href), situacao = el("financeiro-filtro-situacao").value;
		PARAMETROS_FILTRO.forEach(function (nome) { url.searchParams.delete(nome); });
		var valores = Object.assign({}, f, { situacao: situacao }); delete valores.status; delete valores.vencido; delete valores.tamanho;
		Object.keys(valores).forEach(function (nome) { var valor = valores[nome]; if (valor !== "" && valor !== null && valor !== undefined && !(nome === "pagina" && valor === 0)) url.searchParams.set(nome, valor); else url.searchParams.delete(nome); });
		window.history.replaceState(null, "", url.pathname + (url.searchParams.toString() ? "?" + url.searchParams.toString() : ""));
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
		if (podeEscrever && l.status === "PENDENTE") { td.appendChild(botao("Editar", function () { abrirEdicao(l); })); td.appendChild(botao("Liquidar", function () { abrirLiquidacao(l); }, "criati-btn criati-btn-primary")); }
		if (podeEscrever && (l.status === "LIQUIDADO" || l.status === "PAGO")) td.appendChild(botao("Editar", function () { abrirEdicao(l); }));
		if (podeDesliquidar && (l.status === "LIQUIDADO" || l.status === "PAGO")) td.appendChild(botao("Desliquidar", function () { desliquidar(l); }));
		if (podeEscrever && l.status !== "CANCELADO") td.appendChild(botao("Cancelar", function () { cancelar(l); }, "criati-btn criati-btn-danger")); return td; }
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
