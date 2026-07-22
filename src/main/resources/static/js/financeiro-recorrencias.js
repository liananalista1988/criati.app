/* Criati Financeiro - recorrencias (regras de geracao) e ocorrencias geradas. */
(function (window, document) {
	"use strict";
	var STATUS_LABEL = { ATIVA: "Ativa", PAUSADA: "Pausada", ENCERRADA: "Encerrada" };
	var PERIODICIDADE_LABEL = { MENSAL: "Mensal", ANUAL: "Anual" };
	var recorrenciaEmEdicao = null;
	var contas = [], categorias = [], pessoas = [], partes = [];
	function el(id) { return document.getElementById(id); }
	function hoje() { return new Date().toISOString().slice(0, 10); }

	function iniciar() {
		if (!el("financeiro-recorrencia-form")) return;
		el("financeiro-recorrencias-nova-receita").addEventListener("click", function () { abrirCriacao("RECEITA"); });
		el("financeiro-recorrencias-nova-despesa").addEventListener("click", function () { abrirCriacao("DESPESA"); });
		el("financeiro-recorrencias-gerar-automaticas").addEventListener("click", gerarAutomaticas);
		el("financeiro-recorrencia-cancelar").addEventListener("click", fecharFormulario);
		el("financeiro-recorrencia-form").addEventListener("submit", salvar);
		el("financeiro-recorrencia-periodicidade").addEventListener("change", alternarCampoMes);
		el("financeiro-recorrencia-ocorrencias-fechar").addEventListener("click", fecharOcorrencias);
		["status", "tipo", "periodicidade", "pessoa", "categoria"]
			.forEach(function (s) { el("financeiro-recorrencias-filtro-" + s).addEventListener("change", carregar); });
		el("financeiro-recorrencias-filtro-busca").addEventListener("input", debounce(carregar, 300));
		carregarAuxiliares().then(carregar);
	}

	function debounce(fn, atraso) { var timer; return function () { clearTimeout(timer); timer = setTimeout(fn, atraso); }; }
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
				preencher(el("financeiro-recorrencias-filtro-pessoa"), pessoas, "Todas as pessoas");
				preencher(el("financeiro-recorrencias-filtro-categoria"), categorias, "Todas as categorias");
				preencher(el("financeiro-recorrencia-conta"), contas, null);
				preencher(el("financeiro-recorrencia-pessoa"), pessoas, null);
				preencher(el("financeiro-recorrencia-parte"), partes, "Sem contato");
			});
	}

	function filtros() {
		return { status: el("financeiro-recorrencias-filtro-status").value, tipo: el("financeiro-recorrencias-filtro-tipo").value,
			periodicidade: el("financeiro-recorrencias-filtro-periodicidade").value,
			pessoaId: el("financeiro-recorrencias-filtro-pessoa").value, categoriaId: el("financeiro-recorrencias-filtro-categoria").value,
			busca: el("financeiro-recorrencias-filtro-busca").value };
	}
	function carregar() {
		el("financeiro-recorrencias-carregando").hidden = false; el("financeiro-recorrencias-erro").hidden = true;
		el("financeiro-recorrencias-vazio").hidden = true; el("financeiro-recorrencias-tabela-wrap").hidden = true;
		Promise.all([window.FinanceiroApi.recorrencias.listar(filtros()), window.FinanceiroApi.recorrencias.resumo()])
			.then(function (r) {
				el("financeiro-recorrencias-carregando").hidden = true; renderResumo(r[1].data || {});
				var itens = r[0].data || []; if (!itens.length) { el("financeiro-recorrencias-vazio").hidden = false; return; }
				el("financeiro-recorrencias-tabela-wrap").hidden = false; renderTabela(itens);
			}).catch(function () { el("financeiro-recorrencias-carregando").hidden = true; el("financeiro-recorrencias-erro").hidden = false; });
	}
	function renderResumo(r) {
		el("resumo-recorrencias-ativas").textContent = r.ativas || 0;
		el("resumo-recorrencias-pausadas").textContent = r.pausadas || 0;
		el("resumo-recorrencias-encerradas").textContent = r.encerradas || 0;
		el("resumo-recorrencias-geradas").textContent = r.ocorrenciasGeradasNaCompetencia || 0;
		el("resumo-recorrencias-receitas").textContent = window.FinanceiroFormatacao.moeda(r.receitasRecorrentesPrevistas || 0);
		el("resumo-recorrencias-despesas").textContent = window.FinanceiroFormatacao.moeda(r.despesasRecorrentesPrevistas || 0);
	}
	function renderTabela(itens) { var tbody = el("financeiro-recorrencias-tbody"); tbody.innerHTML = "";
		itens.forEach(function (r) { tbody.appendChild(linha(r)); }); }
	function celula(texto) { var td = document.createElement("td"); td.textContent = texto || "—"; return td; }
	function linha(r) {
		var tr = document.createElement("tr"); tr.appendChild(celula(r.descricao));
		tr.appendChild(celula(r.tipo === "RECEITA" ? "Receita" : "Despesa"));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(r.valorPadrao)));
		tr.appendChild(celula(PERIODICIDADE_LABEL[r.periodicidade] || r.periodicidade));
		tr.appendChild(celula(r.pessoaFinanceiraNome)); tr.appendChild(celula(r.categoriaNome));
		tr.appendChild(celula(window.FinanceiroFormatacao.competenciaLabel(r.proximaCompetencia)));
		var status = document.createElement("td"), badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + r.status.toLowerCase();
		badge.textContent = STATUS_LABEL[r.status] || r.status; status.appendChild(badge); tr.appendChild(status);
		tr.appendChild(celula(r.gerarAutomaticamente ? "Sim" : "Nao"));
		tr.appendChild(acoes(r)); return tr;
	}
	function botao(texto, fn) { var b = document.createElement("button"); b.type = "button"; b.className = "criati-btn criati-btn-ghost"; b.textContent = texto; b.addEventListener("click", fn); return b; }
	function acoes(r) {
		var td = document.createElement("td"); td.className = "criati-table-acoes";
		td.appendChild(botao("Ocorrencias", function () { abrirOcorrencias(r); }));
		if (r.status !== "ENCERRADA") td.appendChild(botao("Editar", function () { abrirEdicao(r); }));
		if (r.status === "ATIVA") { td.appendChild(botao("Gerar proxima", function () { gerarOcorrencia(r); })); td.appendChild(botao("Pausar", function () { pausar(r); })); }
		if (r.status === "PAUSADA") td.appendChild(botao("Retomar", function () { retomar(r); }));
		if (r.status !== "ENCERRADA") td.appendChild(botao("Encerrar", function () { encerrar(r); }));
		return td;
	}
	function sucesso(msg) { return function () { window.CriatiUI.showToast("sucesso", msg); carregar(); }; }
	function falha(msg) { return function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || msg); }; }

	function pausar(r) { if (!confirm("Pausar interrompe novas geracoes; lancamentos ja gerados permanecem. Continuar?")) return;
		window.FinanceiroApi.recorrencias.pausar(r.id).then(sucesso("Recorrencia pausada.")).catch(falha("Nao foi possivel pausar.")); }
	function retomar(r) { window.FinanceiroApi.recorrencias.retomar(r.id).then(sucesso("Recorrencia retomada.")).catch(falha("Nao foi possivel retomar.")); }
	function encerrar(r) { if (!confirm("Encerrar impede novas geracoes desta recorrencia. O historico e preservado. Continuar?")) return;
		window.FinanceiroApi.recorrencias.encerrar(r.id).then(sucesso("Recorrencia encerrada.")).catch(falha("Nao foi possivel encerrar.")); }
	function gerarOcorrencia(r) { window.FinanceiroApi.recorrencias.gerar(r.id).then(sucesso("Ocorrencia gerada.")).catch(falha("Nao foi possivel gerar a ocorrencia.")); }
	function gerarAutomaticas() { var b = el("financeiro-recorrencias-gerar-automaticas"); window.CriatiUI.setButtonLoading(b, true, "Gerando...");
		window.FinanceiroApi.recorrencias.gerarAutomaticas().then(function (r) {
			var qtd = (r.data && r.data.ocorrenciasGeradas) || 0;
			window.CriatiUI.showToast("sucesso", qtd + " ocorrencia(s) gerada(s)."); carregar();
		}).catch(falha("Nao foi possivel processar as geracoes automaticas.")).finally(function () { window.CriatiUI.setButtonLoading(b, false); }); }

	function abrirOcorrencias(r) {
		window.FinanceiroApi.recorrencias.ocorrencias(r.id).then(function (resp) {
			var itens = resp.data || []; var tbody = el("financeiro-recorrencia-ocorrencias-tbody"); tbody.innerHTML = "";
			el("financeiro-recorrencia-ocorrencias-vazio").hidden = !!itens.length;
			itens.forEach(function (l) {
				var tr = document.createElement("tr");
				tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataCompetencia)));
				tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataVencimento)));
				tr.appendChild(celula(window.FinanceiroFormatacao.moeda(l.valor)));
				tr.appendChild(celula(STATUS_LABEL[l.status] || l.status));
				tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(l.dataLiquidacao)));
				var link = document.createElement("td"); var a = document.createElement("a"); a.href = "/app/financeiro/lancamentos"; a.textContent = "Ver lancamentos"; link.appendChild(a); tr.appendChild(link);
				tbody.appendChild(tr);
			});
			el("financeiro-recorrencia-ocorrencias-modal").hidden = false;
		}).catch(falha("Nao foi possivel carregar as ocorrencias."));
	}
	function fecharOcorrencias() { el("financeiro-recorrencia-ocorrencias-modal").hidden = true; }

	function alternarCampoMes() {
		el("financeiro-recorrencia-mes-campo").hidden = el("financeiro-recorrencia-periodicidade").value !== "ANUAL";
	}
	function preencherCategorias(tipo) { preencher(el("financeiro-recorrencia-categoria"), categorias.filter(function (c) { return c.tipo === tipo; }), null); }
	function abrirCriacao(tipo) {
		recorrenciaEmEdicao = null; el("financeiro-recorrencia-modal-titulo").textContent = tipo === "RECEITA" ? "Nova receita recorrente" : "Nova despesa recorrente";
		el("financeiro-recorrencia-modal-aviso").hidden = true;
		el("financeiro-recorrencia-tipo").value = tipo; el("financeiro-recorrencia-descricao").value = ""; el("financeiro-recorrencia-valor").value = "";
		el("financeiro-recorrencia-periodicidade").value = "MENSAL"; el("financeiro-recorrencia-intervalo").value = "1";
		el("financeiro-recorrencia-dia").value = "1"; el("financeiro-recorrencia-mes").value = "1";
		el("financeiro-recorrencia-data-inicial").value = hoje(); el("financeiro-recorrencia-data-final").value = "";
		el("financeiro-recorrencia-gerar-automatica").checked = false; el("financeiro-recorrencia-forma").value = "";
		el("financeiro-recorrencia-observacao").value = ""; preencherCategorias(tipo); alternarCampoMes();
		el("financeiro-recorrencia-modal").hidden = false;
	}
	function abrirEdicao(r) {
		recorrenciaEmEdicao = r; el("financeiro-recorrencia-modal-titulo").textContent = "Editar recorrencia";
		el("financeiro-recorrencia-modal-aviso").hidden = false;
		el("financeiro-recorrencia-tipo").value = r.tipo; preencherCategorias(r.tipo);
		el("financeiro-recorrencia-descricao").value = r.descricao; el("financeiro-recorrencia-valor").value = r.valorPadrao;
		el("financeiro-recorrencia-conta").value = r.contaId; el("financeiro-recorrencia-categoria").value = r.categoriaId;
		el("financeiro-recorrencia-pessoa").value = r.pessoaFinanceiraId; el("financeiro-recorrencia-parte").value = r.parteFinanceiraId || "";
		el("financeiro-recorrencia-forma").value = r.formaPagamento || "";
		el("financeiro-recorrencia-periodicidade").value = r.periodicidade; el("financeiro-recorrencia-intervalo").value = r.intervalo;
		el("financeiro-recorrencia-dia").value = r.diaReferencia; el("financeiro-recorrencia-mes").value = r.mesReferencia || "1";
		el("financeiro-recorrencia-data-inicial").value = r.dataInicial; el("financeiro-recorrencia-data-final").value = r.dataFinal || "";
		el("financeiro-recorrencia-gerar-automatica").checked = !!r.gerarAutomaticamente; el("financeiro-recorrencia-observacao").value = r.observacao || "";
		alternarCampoMes(); el("financeiro-recorrencia-modal").hidden = false;
	}
	function fecharFormulario() { el("financeiro-recorrencia-modal").hidden = true; el("financeiro-recorrencia-form").reset(); recorrenciaEmEdicao = null; }
	function salvar(e) {
		e.preventDefault(); var b = el("financeiro-recorrencia-salvar");
		var periodicidade = el("financeiro-recorrencia-periodicidade").value;
		var d = { descricao: el("financeiro-recorrencia-descricao").value, valorPadrao: el("financeiro-recorrencia-valor").value,
			contaId: el("financeiro-recorrencia-conta").value, categoriaId: el("financeiro-recorrencia-categoria").value,
			pessoaFinanceiraId: el("financeiro-recorrencia-pessoa").value, parteFinanceiraId: el("financeiro-recorrencia-parte").value || null,
			formaPagamento: el("financeiro-recorrencia-forma").value || null, intervalo: el("financeiro-recorrencia-intervalo").value,
			dia: el("financeiro-recorrencia-dia").value, mes: periodicidade === "ANUAL" ? el("financeiro-recorrencia-mes").value : null,
			dataFinal: el("financeiro-recorrencia-data-final").value || null,
			gerarAutomaticamente: el("financeiro-recorrencia-gerar-automatica").checked, observacao: el("financeiro-recorrencia-observacao").value };
		var chamada;
		if (recorrenciaEmEdicao) {
			chamada = window.FinanceiroApi.recorrencias.editar(recorrenciaEmEdicao.id, d);
		} else {
			d.tipo = el("financeiro-recorrencia-tipo").value; d.periodicidade = periodicidade; d.dataInicial = el("financeiro-recorrencia-data-inicial").value;
			chamada = window.FinanceiroApi.recorrencias.criar(d);
		}
		window.CriatiUI.setButtonLoading(b, true, "Salvando...");
		chamada.then(function () { fecharFormulario(); sucesso("Recorrencia salva.")(); }).catch(falha("Nao foi possivel salvar.")).finally(function () { window.CriatiUI.setButtonLoading(b, false); });
	}
	window.FinanceiroRecorrencias = { iniciar: iniciar };
})(window, document);
