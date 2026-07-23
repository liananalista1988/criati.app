/* Criati Financeiro - contas a pagar: compromissos, ocorrencias e pagamentos. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { PENDENTE: "Pendente", PARCIALMENTE_PAGA: "Parcialmente paga", PAGA: "Paga", CANCELADA: "Cancelada" };
	var TIPO_VALOR_LABEL = { FIXO: "Fixo", VARIAVEL: "Variavel" };
	var PAGAMENTO_STATUS_LABEL = { ATIVO: "Ativo", ESTORNADO: "Estornado" };

	var categorias = [], categoriasDespesa = [], pessoas = [], partes = [], contas = [], recorrenciasDisponiveis = [];
	var ocorrenciaEmEdicao = null;
	var compromissoEmEdicao = null;
	var ocorrenciaAtualPagamento = null;
	var ocorrenciaAtualHistorico = null;

	function el(id) { return document.getElementById(id); }
	function hoje() { return new Date().toISOString().slice(0, 10); }

	function iniciar() {
		if (!el("contas-a-pagar-aba-ocorrencias")) return;
		el("contas-a-pagar-aba-ocorrencias").addEventListener("click", function () { mostrarAba("ocorrencias"); });
		el("contas-a-pagar-aba-compromissos").addEventListener("click", function () { mostrarAba("compromissos"); });
		el("contas-a-pagar-aba-calendario").addEventListener("click", function () { mostrarAba("calendario"); });

		["status", "vencidas", "pessoa", "categoria"].forEach(function (s) {
			el("ocorrencias-filtro-" + s).addEventListener("change", carregarOcorrencias);
		});
		el("ocorrencias-filtro-busca").addEventListener("input", debounce(carregarOcorrencias, 300));
		el("ocorrencias-nova").addEventListener("click", abrirCriacaoOcorrencia);
		el("ocorrencia-cancelar").addEventListener("click", fecharFormularioOcorrencia);
		el("ocorrencia-form").addEventListener("submit", salvarOcorrencia);

		el("pagamento-integral").addEventListener("change", alternarPagamentoIntegral);
		el("pagamento-cancelar").addEventListener("click", fecharPagamento);
		el("pagamento-form").addEventListener("submit", confirmarPagamento);
		el("pagamentos-historico-fechar").addEventListener("click", fecharHistorico);

		el("compromissos-filtro-ativo").addEventListener("change", carregarCompromissos);
		el("compromissos-filtro-busca").addEventListener("input", debounce(carregarCompromissos, 300));
		el("compromissos-novo").addEventListener("click", abrirCriacaoCompromisso);
		el("compromisso-cancelar").addEventListener("click", fecharFormularioCompromisso);
		el("compromisso-form").addEventListener("submit", salvarCompromisso);

		el("calendario-mes").value = window.FinanceiroFormatacao.competenciaAtual();
		el("calendario-mes").addEventListener("change", carregarCalendario);

		carregarAuxiliares().then(carregarOcorrencias);
	}

	function debounce(fn, atraso) { var timer; return function () { clearTimeout(timer); timer = setTimeout(fn, atraso); }; }

	function mostrarAba(nome) {
		el("contas-a-pagar-secao-ocorrencias").hidden = nome !== "ocorrencias";
		el("contas-a-pagar-secao-compromissos").hidden = nome !== "compromissos";
		el("contas-a-pagar-secao-calendario").hidden = nome !== "calendario";
		if (nome === "compromissos") { carregarCompromissos(); }
		if (nome === "calendario") { carregarCalendario(); }
	}

	function preencher(select, itens, texto, valorProp, textoProp) {
		select.innerHTML = "";
		if (texto !== null) { var vazio = document.createElement("option"); vazio.value = ""; vazio.textContent = texto; select.appendChild(vazio); }
		itens.forEach(function (item) {
			var o = document.createElement("option");
			o.value = item[valorProp || "id"];
			o.textContent = item[textoProp || "nome"];
			select.appendChild(o);
		});
	}

	function carregarAuxiliares() {
		return Promise.all([
			window.FinanceiroApi.categorias.listar({ status: "ATIVO" }),
			window.FinanceiroApi.pessoas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.contatos.listar({ status: "ATIVO" }),
			window.FinanceiroApi.contas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.recorrencias.listar({ status: "ATIVA", tipo: "DESPESA" })
		]).then(function (r) {
			categorias = r[0].data || [];
			categoriasDespesa = categorias.filter(function (c) { return c.tipo === "DESPESA"; });
			pessoas = r[1].data || [];
			partes = r[2].data || [];
			contas = r[3].data || [];
			recorrenciasDisponiveis = r[4].data || [];

			preencher(el("ocorrencias-filtro-pessoa"), pessoas, "Todas as pessoas");
			preencher(el("ocorrencias-filtro-categoria"), categoriasDespesa, "Todas as categorias");
			preencher(el("ocorrencia-categoria"), categoriasDespesa, null);
			preencher(el("ocorrencia-pessoa"), pessoas, null);
			preencher(el("ocorrencia-parte"), partes, "Sem contato");
			preencher(el("ocorrencia-conta-prevista"), contas, "Sem conta prevista");
			preencher(el("pagamento-conta"), contas, null);
			preencher(el("compromisso-categoria"), categoriasDespesa, null);
			preencher(el("compromisso-pessoa"), pessoas, null);
			preencher(el("compromisso-parte"), partes, "Sem contato");
			preencher(el("compromisso-conta-padrao"), contas, "Sem conta padrao");
			preencher(el("compromisso-recorrencia"), recorrenciasDisponiveis, "Nenhuma (compromisso avulso)", "id", "descricao");
		});
	}

	function sucesso(msg, recarregar) { return function () { window.CriatiUI.showToast("sucesso", msg); recarregar(); }; }
	function falha(msg) { return function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || msg); }; }
	function celula(texto) { var td = document.createElement("td"); td.textContent = texto || "—"; return td; }
	function botao(texto, fn) {
		var b = document.createElement("button"); b.type = "button"; b.className = "criati-btn criati-btn-ghost";
		b.textContent = texto; b.addEventListener("click", fn); return b;
	}
	function badge(status, labelMap) {
		var td = document.createElement("td"), span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + status.toLowerCase();
		span.textContent = labelMap[status] || status; td.appendChild(span); return td;
	}

	/* ==================== OCORRENCIAS ==================== */

	function filtrosOcorrencias() {
		return {
			status: el("ocorrencias-filtro-status").value,
			vencidas: el("ocorrencias-filtro-vencidas").value,
			pessoaId: el("ocorrencias-filtro-pessoa").value,
			categoriaId: el("ocorrencias-filtro-categoria").value,
			busca: el("ocorrencias-filtro-busca").value
		};
	}

	function carregarOcorrencias() {
		el("ocorrencias-carregando").hidden = false; el("ocorrencias-erro").hidden = true;
		el("ocorrencias-vazio").hidden = true; el("ocorrencias-tabela-wrap").hidden = true;
		Promise.all([
			window.FinanceiroApi.ocorrenciasCompromisso.listar(filtrosOcorrencias()),
			window.FinanceiroApi.ocorrenciasCompromisso.resumo({})
		]).then(function (r) {
			el("ocorrencias-carregando").hidden = true;
			renderResumoOcorrencias(r[1].data || {});
			var itens = r[0].data || [];
			if (!itens.length) { el("ocorrencias-vazio").hidden = false; return; }
			el("ocorrencias-tabela-wrap").hidden = false;
			renderTabelaOcorrencias(itens);
		}).catch(function () { el("ocorrencias-carregando").hidden = true; el("ocorrencias-erro").hidden = false; });
	}

	function renderResumoOcorrencias(r) {
		var m = window.FinanceiroFormatacao.moeda;
		el("resumo-total-previsto").textContent = m(r.totalPrevisto || 0);
		el("resumo-total-principal").textContent = m((r.totalPrincipal || 0));
		el("resumo-total-pago").textContent = m(r.totalPago || 0);
		el("resumo-saldo-pendente").textContent = m(r.saldoPendente || 0);
		el("resumo-qtd-pendente").textContent = r.quantidadePendente || 0;
		el("resumo-qtd-parcial").textContent = r.quantidadeParcialmentePaga || 0;
		el("resumo-qtd-vencida").textContent = r.quantidadeVencida || 0;
		el("resumo-qtd-vencendo").textContent = r.quantidadeVencendoEmBreve || 0;
	}

	function renderTabelaOcorrencias(itens) {
		var tbody = el("ocorrencias-tbody"); tbody.innerHTML = "";
		itens.forEach(function (o) { tbody.appendChild(linhaOcorrencia(o)); });
	}

	function linhaOcorrencia(o) {
		var tr = document.createElement("tr");
		var m = window.FinanceiroFormatacao.moeda, d = window.FinanceiroFormatacao.dataBr;
		var descricao = o.descricao + (o.vencida ? " (vencida ha " + o.diasEmAtraso + " dia(s))" : "");
		tr.appendChild(celula(descricao));
		tr.appendChild(celula(window.FinanceiroFormatacao.competenciaLabel(o.competencia)));
		tr.appendChild(celula(d(o.vencimento)));
		tr.appendChild(celula(m(o.valorTotal)));
		tr.appendChild(celula(m(o.valorPago)));
		tr.appendChild(celula(m(o.saldoPendente)));
		tr.appendChild(celula(o.pessoaFinanceiraNome));
		tr.appendChild(celula(o.categoriaNome));
		tr.appendChild(badge(o.status, STATUS_LABEL));
		tr.appendChild(acoesOcorrencia(o));
		return tr;
	}

	function acoesOcorrencia(o) {
		var td = document.createElement("td"); td.className = "criati-table-acoes";
		td.appendChild(botao("Detalhes", function () { window.FinanceiroDetalhes.abrir("Detalhes da conta a pagar", [
			["Descrição", o.descricao], ["Competência", window.FinanceiroFormatacao.competenciaLabel(o.competencia)],
			["Vencimento", window.FinanceiroFormatacao.dataBr(o.vencimento)],
			["Valor principal", window.FinanceiroFormatacao.moeda(o.valorPrincipal)],
			["Juros", window.FinanceiroFormatacao.moeda(o.juros)], ["Multa", window.FinanceiroFormatacao.moeda(o.multa)],
			["Desconto", window.FinanceiroFormatacao.moeda(o.desconto)], ["Valor total", window.FinanceiroFormatacao.moeda(o.valorTotal)],
			["Valor pago", window.FinanceiroFormatacao.moeda(o.valorPago)], ["Saldo pendente", window.FinanceiroFormatacao.moeda(o.saldoPendente)],
			["Pessoa", o.pessoaFinanceiraNome], ["Contato", o.parteFinanceiraNome], ["Categoria", o.categoriaNome],
			["Conta prevista", o.contaPrevistaNome], ["Observação", o.observacao], ["Status", STATUS_LABEL[o.status] || o.status]
		]); }));
		if (o.status !== "PAGA" && o.status !== "CANCELADA") {
			td.appendChild(botao("Pagar", function () { abrirPagamento(o); }));
		}
		td.appendChild(botao("Pagamentos", function () { abrirHistorico(o); }));
		if (o.status !== "CANCELADA") {
			td.appendChild(botao("Editar", function () { abrirEdicaoOcorrencia(o); }));
			td.appendChild(botao("Cancelar", function () { cancelarOcorrencia(o); }));
		}
		return td;
	}

	function cancelarOcorrencia(o) {
		if (!confirm("Cancelar impede novos pagamentos nesta ocorrencia. Continuar?")) return;
		window.FinanceiroApi.ocorrenciasCompromisso.cancelar(o.id)
			.then(sucesso("Ocorrencia cancelada.", carregarOcorrencias)).catch(falha("Nao foi possivel cancelar."));
	}

	function preencherCategoriaFiltradaPorDespesa(selectId) {
		preencher(el(selectId), categoriasDespesa, null);
	}

	function abrirCriacaoOcorrencia() {
		ocorrenciaEmEdicao = null;
		el("ocorrencia-modal-titulo").textContent = "Nova obrigacao avulsa";
		preencherCategoriaFiltradaPorDespesa("ocorrencia-categoria");
		el("ocorrencia-descricao").value = ""; el("ocorrencia-competencia").value = window.FinanceiroFormatacao.competenciaAtual();
		el("ocorrencia-valor-previsto").value = ""; el("ocorrencia-valor-principal").value = "";
		el("ocorrencia-vencimento").value = hoje(); el("ocorrencia-data-recebimento").value = "";
		el("ocorrencia-juros").value = "0"; el("ocorrencia-multa").value = "0"; el("ocorrencia-desconto").value = "0";
		el("ocorrencia-observacao").value = "";
		el("ocorrencia-modal").hidden = false;
	}

	function abrirEdicaoOcorrencia(o) {
		ocorrenciaEmEdicao = o;
		el("ocorrencia-modal-titulo").textContent = "Editar ocorrencia";
		preencherCategoriaFiltradaPorDespesa("ocorrencia-categoria");
		el("ocorrencia-descricao").value = o.descricao;
		el("ocorrencia-competencia").value = o.competencia;
		el("ocorrencia-competencia").disabled = true;
		el("ocorrencia-categoria").value = o.categoriaId;
		el("ocorrencia-pessoa").value = o.pessoaFinanceiraId;
		el("ocorrencia-parte").value = o.parteFinanceiraId || "";
		el("ocorrencia-conta-prevista").value = o.contaPrevistaId || "";
		el("ocorrencia-valor-previsto").value = o.valorPrevisto || "";
		el("ocorrencia-valor-principal").value = o.valorPrincipal;
		el("ocorrencia-vencimento").value = o.vencimento;
		el("ocorrencia-data-recebimento").value = o.dataRecebimentoCobranca || "";
		el("ocorrencia-juros").value = o.juros; el("ocorrencia-multa").value = o.multa; el("ocorrencia-desconto").value = o.desconto;
		el("ocorrencia-observacao").value = o.observacao || "";
		el("ocorrencia-modal").hidden = false;
	}

	function fecharFormularioOcorrencia() {
		el("ocorrencia-modal").hidden = true; el("ocorrencia-form").reset();
		el("ocorrencia-competencia").disabled = false; ocorrenciaEmEdicao = null;
	}

	function salvarOcorrencia(e) {
		e.preventDefault();
		var b = el("ocorrencia-salvar");
		var d = {
			descricao: el("ocorrencia-descricao").value,
			competencia: el("ocorrencia-competencia").value,
			categoriaId: el("ocorrencia-categoria").value,
			pessoaFinanceiraId: el("ocorrencia-pessoa").value,
			parteFinanceiraId: el("ocorrencia-parte").value || null,
			contaPrevistaId: el("ocorrencia-conta-prevista").value || null,
			valorPrevisto: el("ocorrencia-valor-previsto").value || null,
			valorPrincipal: el("ocorrencia-valor-principal").value,
			vencimento: el("ocorrencia-vencimento").value,
			dataRecebimentoCobranca: el("ocorrencia-data-recebimento").value || null,
			juros: el("ocorrencia-juros").value || 0,
			multa: el("ocorrencia-multa").value || 0,
			desconto: el("ocorrencia-desconto").value || 0,
			observacao: el("ocorrencia-observacao").value
		};
		var chamada = ocorrenciaEmEdicao
			? window.FinanceiroApi.ocorrenciasCompromisso.editar(ocorrenciaEmEdicao.id, d)
			: window.FinanceiroApi.ocorrenciasCompromisso.criar(d);
		window.CriatiUI.setButtonLoading(b, true, "Salvando...");
		chamada.then(function () { fecharFormularioOcorrencia(); sucesso("Ocorrencia salva.", carregarOcorrencias)(); })
			.catch(falha("Nao foi possivel salvar."))
			.finally(function () { window.CriatiUI.setButtonLoading(b, false); });
	}

	/* ==================== PAGAMENTO ==================== */

	function alternarPagamentoIntegral() {
		var integral = el("pagamento-integral").checked;
		el("pagamento-valor").disabled = integral;
		if (integral && ocorrenciaAtualPagamento) { el("pagamento-valor").value = ocorrenciaAtualPagamento.saldoPendente; }
	}

	function abrirPagamento(o) {
		ocorrenciaAtualPagamento = o;
		var m = window.FinanceiroFormatacao.moeda;
		el("pagamento-valor-total").textContent = m(o.valorTotal);
		el("pagamento-valor-pago").textContent = m(o.valorPago);
		el("pagamento-saldo-pendente").textContent = m(o.saldoPendente);
		el("pagamento-erro-excedente").hidden = true;
		el("pagamento-integral").checked = true;
		el("pagamento-valor").value = o.saldoPendente;
		el("pagamento-valor").disabled = true;
		el("pagamento-data").value = hoje();
		el("pagamento-forma").value = ""; el("pagamento-observacao").value = "";
		el("pagamento-conta").value = o.contaPrevistaId || (contas[0] && contas[0].id) || "";
		el("pagamento-modal").hidden = false;
	}

	function fecharPagamento() {
		el("pagamento-modal").hidden = true; el("pagamento-form").reset(); ocorrenciaAtualPagamento = null;
	}

	function confirmarPagamento(e) {
		e.preventDefault();
		if (!ocorrenciaAtualPagamento) return;
		var integral = el("pagamento-integral").checked;
		var valor = Number(el("pagamento-valor").value);
		if (!integral && valor > Number(ocorrenciaAtualPagamento.saldoPendente)) {
			el("pagamento-erro-excedente").hidden = false;
			return;
		}
		el("pagamento-erro-excedente").hidden = true;
		var dados = {
			contaId: el("pagamento-conta").value,
			dataPagamento: el("pagamento-data").value,
			formaPagamento: el("pagamento-forma").value || null,
			observacao: el("pagamento-observacao").value
		};
		var b = el("pagamento-confirmar");
		var chamada = integral
			? window.FinanceiroApi.ocorrenciasCompromisso.pagarIntegral(ocorrenciaAtualPagamento.id, dados)
			: window.FinanceiroApi.ocorrenciasCompromisso.pagarParcial(ocorrenciaAtualPagamento.id,
				Object.assign({ valor: valor }, dados));
		window.CriatiUI.setButtonLoading(b, true, "Confirmando...");
		chamada.then(function () { fecharPagamento(); sucesso("Pagamento registrado.", carregarOcorrencias)(); })
			.catch(falha("Nao foi possivel registrar o pagamento."))
			.finally(function () { window.CriatiUI.setButtonLoading(b, false); });
	}

	/* ==================== HISTORICO / ESTORNO ==================== */

	function abrirHistorico(o) {
		ocorrenciaAtualHistorico = o;
		window.FinanceiroApi.ocorrenciasCompromisso.pagamentos(o.id).then(function (resp) {
			var itens = resp.data || [];
			var tbody = el("pagamentos-historico-tbody"); tbody.innerHTML = "";
			el("pagamentos-historico-vazio").hidden = !!itens.length;
			itens.forEach(function (p) { tbody.appendChild(linhaPagamento(p)); });
			el("pagamentos-historico-modal").hidden = false;
		}).catch(falha("Nao foi possivel carregar os pagamentos."));
	}

	function linhaPagamento(p) {
		var tr = document.createElement("tr");
		var m = window.FinanceiroFormatacao.moeda, d = window.FinanceiroFormatacao.dataBr;
		tr.appendChild(celula(d(p.dataPagamento)));
		tr.appendChild(celula(m(p.valor)));
		tr.appendChild(celula(p.contaNome));
		tr.appendChild(celula(p.formaPagamento || "-"));
		tr.appendChild(badge(p.status, PAGAMENTO_STATUS_LABEL));
		var acoes = document.createElement("td");
		if (p.status === "ATIVO") {
			acoes.appendChild(botao("Estornar", function () { estornarPagamento(p); }));
		}
		tr.appendChild(acoes);
		return tr;
	}

	function estornarPagamento(p) {
		var motivo = prompt("Motivo do estorno (opcional):", "");
		if (motivo === null) return;
		window.FinanceiroApi.ocorrenciasCompromisso.estornarPagamento(ocorrenciaAtualHistorico.id, p.id, motivo)
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Pagamento estornado.");
				abrirHistorico(ocorrenciaAtualHistorico);
				carregarOcorrencias();
			}).catch(falha("Nao foi possivel estornar o pagamento."));
	}

	function fecharHistorico() { el("pagamentos-historico-modal").hidden = true; ocorrenciaAtualHistorico = null; }

	/* ==================== COMPROMISSOS ==================== */

	function filtrosCompromissos() {
		return { ativo: el("compromissos-filtro-ativo").value, busca: el("compromissos-filtro-busca").value };
	}

	function carregarCompromissos() {
		el("compromissos-carregando").hidden = false; el("compromissos-erro").hidden = true;
		el("compromissos-vazio").hidden = true; el("compromissos-tabela-wrap").hidden = true;
		window.FinanceiroApi.compromissos.listar(filtrosCompromissos()).then(function (resp) {
			el("compromissos-carregando").hidden = true;
			var itens = resp.data || [];
			if (!itens.length) { el("compromissos-vazio").hidden = false; return; }
			el("compromissos-tabela-wrap").hidden = false;
			var tbody = el("compromissos-tbody"); tbody.innerHTML = "";
			itens.forEach(function (c) { tbody.appendChild(linhaCompromisso(c)); });
		}).catch(function () { el("compromissos-carregando").hidden = true; el("compromissos-erro").hidden = false; });
	}

	function linhaCompromisso(c) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(c.descricao));
		tr.appendChild(celula(TIPO_VALOR_LABEL[c.tipoValor] || c.tipoValor));
		tr.appendChild(celula(c.recorrenciaId ? "Recorrente" : "Avulso"));
		tr.appendChild(celula(c.pessoaFinanceiraNome));
		tr.appendChild(celula(c.categoriaNome));
		tr.appendChild(celula(c.ativo ? "Ativo" : "Inativo"));
		var acoes = document.createElement("td"); acoes.className = "criati-table-acoes";
		acoes.appendChild(botao("Detalhes", function () { window.FinanceiroDetalhes.abrir("Detalhes do compromisso", [
			["Descrição", c.descricao], ["Tipo de valor", TIPO_VALOR_LABEL[c.tipoValor] || c.tipoValor],
			["Valor padrão", window.FinanceiroFormatacao.moeda(c.valorPadrao)], ["Dia de vencimento", c.diaVencimentoPadrao],
			["Pessoa", c.pessoaFinanceiraNome], ["Contato", c.parteFinanceiraNome], ["Categoria", c.categoriaNome],
			["Conta padrão", c.contaPadraoNome], ["Recorrência", c.recorrenciaDescricao],
			["Forma de pagamento", c.formaPagamentoPadrao], ["Observação", c.observacao], ["Situação", c.ativo ? "Ativo" : "Inativo"]
		]); }));
		acoes.appendChild(botao("Editar", function () { abrirEdicaoCompromisso(c); }));
		if (c.recorrenciaId) {
			acoes.appendChild(botao("Gerar ocorrencia", function () { gerarOcorrenciaPorCompromisso(c); }));
		}
		acoes.appendChild(botao(c.ativo ? "Desativar" : "Ativar", function () { alternarSituacaoCompromisso(c); }));
		tr.appendChild(acoes);
		return tr;
	}

	function gerarOcorrenciaPorCompromisso(c) {
		window.FinanceiroApi.ocorrenciasCompromisso.gerarPorCompromisso(c.id)
			.then(sucesso("Ocorrencia gerada.", carregarOcorrencias)).catch(falha("Nao foi possivel gerar a ocorrencia."));
	}

	function alternarSituacaoCompromisso(c) {
		var chamada = c.ativo ? window.FinanceiroApi.compromissos.desativar(c.id) : window.FinanceiroApi.compromissos.ativar(c.id);
		chamada.then(sucesso("Compromisso atualizado.", carregarCompromissos)).catch(falha("Nao foi possivel atualizar o compromisso."));
	}

	function abrirCriacaoCompromisso() {
		compromissoEmEdicao = null;
		el("compromisso-modal-titulo").textContent = "Novo compromisso";
		el("compromisso-recorrencia").disabled = false;
		el("compromisso-descricao").value = "";
		el("compromisso-categoria").value = ""; el("compromisso-pessoa").value = "";
		el("compromisso-parte").value = ""; el("compromisso-conta-padrao").value = ""; el("compromisso-recorrencia").value = "";
		el("compromisso-tipo-valor").value = "FIXO"; el("compromisso-valor-padrao").value = "";
		el("compromisso-dia-vencimento").value = ""; el("compromisso-forma").value = ""; el("compromisso-observacao").value = "";
		el("compromisso-modal").hidden = false;
	}

	function abrirEdicaoCompromisso(c) {
		compromissoEmEdicao = c;
		el("compromisso-modal-titulo").textContent = "Editar compromisso";
		el("compromisso-descricao").value = c.descricao;
		el("compromisso-categoria").value = c.categoriaId;
		el("compromisso-pessoa").value = c.pessoaFinanceiraId;
		el("compromisso-parte").value = c.parteFinanceiraId || "";
		el("compromisso-conta-padrao").value = c.contaPadraoId || "";
		el("compromisso-recorrencia").value = c.recorrenciaId || "";
		el("compromisso-recorrencia").disabled = true;
		el("compromisso-tipo-valor").value = c.tipoValor;
		el("compromisso-valor-padrao").value = c.valorPadrao || "";
		el("compromisso-dia-vencimento").value = c.diaVencimentoPadrao || "";
		el("compromisso-forma").value = c.formaPagamentoPadrao || "";
		el("compromisso-observacao").value = c.observacao || "";
		el("compromisso-modal").hidden = false;
	}

	function fecharFormularioCompromisso() {
		el("compromisso-modal").hidden = true; el("compromisso-form").reset(); compromissoEmEdicao = null;
	}

	function salvarCompromisso(e) {
		e.preventDefault();
		var b = el("compromisso-salvar");
		var d = {
			descricao: el("compromisso-descricao").value,
			categoriaId: el("compromisso-categoria").value,
			pessoaFinanceiraId: el("compromisso-pessoa").value,
			parteFinanceiraId: el("compromisso-parte").value || null,
			contaPadraoId: el("compromisso-conta-padrao").value || null,
			tipoValor: el("compromisso-tipo-valor").value,
			valorPadrao: el("compromisso-valor-padrao").value || null,
			diaVencimentoPadrao: el("compromisso-dia-vencimento").value || null,
			formaPagamentoPadrao: el("compromisso-forma").value || null,
			observacao: el("compromisso-observacao").value
		};
		var chamada;
		if (compromissoEmEdicao) {
			chamada = window.FinanceiroApi.compromissos.editar(compromissoEmEdicao.id, d);
		} else {
			d.recorrenciaId = el("compromisso-recorrencia").value || null;
			chamada = window.FinanceiroApi.compromissos.criar(d);
		}
		window.CriatiUI.setButtonLoading(b, true, "Salvando...");
		chamada.then(function () { fecharFormularioCompromisso(); sucesso("Compromisso salvo.", carregarCompromissos)(); })
			.catch(falha("Nao foi possivel salvar o compromisso."))
			.finally(function () { window.CriatiUI.setButtonLoading(b, false); });
	}

	/* ==================== CALENDARIO ==================== */

	function carregarCalendario() {
		el("calendario-carregando").hidden = false; el("calendario-vazio").hidden = true; el("calendario-tabela-wrap").hidden = true;
		var mes = el("calendario-mes").value;
		window.FinanceiroApi.ocorrenciasCompromisso.calendario(mes).then(function (resp) {
			el("calendario-carregando").hidden = true;
			var itens = resp.data || [];
			if (!itens.length) { el("calendario-vazio").hidden = false; return; }
			el("calendario-tabela-wrap").hidden = false;
			var tbody = el("calendario-tbody"); tbody.innerHTML = "";
			itens.forEach(function (o) { tbody.appendChild(linhaCalendario(o)); });
		}).catch(function () { el("calendario-carregando").hidden = true; el("calendario-vazio").hidden = false; });
	}

	function linhaCalendario(o) {
		var tr = document.createElement("tr");
		var m = window.FinanceiroFormatacao.moeda, d = window.FinanceiroFormatacao.dataBr;
		tr.appendChild(celula(d(o.vencimento)));
		tr.appendChild(celula(o.descricao));
		tr.appendChild(celula(m(o.valorTotal)));
		tr.appendChild(celula(m(o.saldoPendente)));
		tr.appendChild(badge(o.status, STATUS_LABEL));
		tr.appendChild(celula(o.vencida ? (o.diasEmAtraso + " dia(s)") : "-"));
		return tr;
	}

	window.FinanceiroContasAPagar = { iniciar: iniciar };
})(window, document);
