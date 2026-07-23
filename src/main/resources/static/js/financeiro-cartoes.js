/* Criati Financeiro - cartoes de credito: estrutura, limites e vinculo fisico/virtual. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var TIPO_LABEL = { FISICO: "Físico", VIRTUAL: "Virtual" };
	var BANDEIRA_LABEL = {
		VISA: "Visa", MASTERCARD: "Mastercard", ELO: "Elo",
		AMERICAN_EXPRESS: "American Express", HIPERCARD: "Hipercard", OUTRA: "Outra"
	};

	var titulares = [], instituicoes = [], cartoesFisicos = [];
	var cartaoEmEdicao = null;

	function el(id) { return document.getElementById(id); }

	function iniciar() {
		if (!el("cartao-form")) return;
		el("cartoes-novo").addEventListener("click", abrirCriacao);
		el("cartao-cancelar").addEventListener("click", fecharFormulario);
		el("cartao-form").addEventListener("submit", salvar);
		el("cartao-tipo").addEventListener("change", alternarCamposPorTipo);
		el("cartao-principal").addEventListener("change", preencherDadosDoPrincipal);
		el("cartao-virtuais-fechar").addEventListener("click", function () { el("cartao-virtuais-modal").hidden = true; });

		["titular", "instituicao", "tipo", "status", "bloqueado"].forEach(function (s) {
			el("cartoes-filtro-" + s).addEventListener("change", carregar);
		});
		el("cartoes-filtro-busca").addEventListener("input", debounce(carregar, 300));

		carregarAuxiliares().then(carregar);
	}

	function debounce(fn, atraso) { var timer; return function () { clearTimeout(timer); timer = setTimeout(fn, atraso); }; }
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
			window.FinanceiroApi.pessoas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.contas.instituicoes(),
			window.FinanceiroApi.cartoes.listar({ tipo: "FISICO", status: "ATIVO" })
		]).then(function (r) {
			titulares = r[0].data || [];
			instituicoes = r[1].data || [];
			cartoesFisicos = r[2].data || [];
			preencher(el("cartoes-filtro-titular"), titulares, "Todos os titulares");
			preencher(el("cartoes-filtro-instituicao"), instituicoes, "Todas as instituições");
			preencher(el("cartao-titular"), titulares, null);
			preencher(el("cartao-instituicao"), instituicoes, null);
			preencher(el("cartao-principal"), cartoesFisicos, "Selecione o cartão principal");
		});
	}

	function filtros() {
		return {
			titularId: el("cartoes-filtro-titular").value,
			instituicaoId: el("cartoes-filtro-instituicao").value,
			tipo: el("cartoes-filtro-tipo").value,
			status: el("cartoes-filtro-status").value,
			bloqueado: el("cartoes-filtro-bloqueado").value,
			busca: el("cartoes-filtro-busca").value
		};
	}

	function carregar() {
		el("cartoes-carregando").hidden = false; el("cartoes-erro").hidden = true;
		el("cartoes-vazio").hidden = true; el("cartoes-tabela-wrap").hidden = true;
		Promise.all([window.FinanceiroApi.cartoes.listar(filtros()), window.FinanceiroApi.cartoes.resumo()])
			.then(function (r) {
				el("cartoes-carregando").hidden = true;
				renderResumo(r[1].data || {});
				var itens = r[0].data || [];
				if (!itens.length) { el("cartoes-vazio").hidden = false; return; }
				el("cartoes-tabela-wrap").hidden = false;
				var tbody = el("cartoes-tbody"); tbody.innerHTML = "";
				itens.forEach(function (c) { tbody.appendChild(linha(c)); });
			}).catch(function () { el("cartoes-carregando").hidden = true; el("cartoes-erro").hidden = false; });
	}

	function renderResumo(r) {
		var m = window.FinanceiroFormatacao.moeda;
		el("resumo-cartoes-ativos").textContent = r.quantidadeAtivos || 0;
		el("resumo-cartoes-limite-total").textContent = m(r.limiteTotalConsolidado || 0);
		el("resumo-cartoes-limite-saudavel").textContent = m(r.limiteSaudavelConsolidado || 0);
		el("resumo-cartoes-limite-disponivel").textContent = m(r.limiteDisponivelConsolidado || 0);
		el("resumo-cartoes-bloqueados").textContent = r.quantidadeBloqueados || 0;
		el("resumo-cartoes-virtuais").textContent = r.quantidadeVirtuais || 0;
	}

	function celula(texto) { var td = document.createElement("td"); td.textContent = texto || "—"; return td; }
	function botao(texto, fn) {
		var b = document.createElement("button"); b.type = "button"; b.className = "criati-btn criati-btn-ghost";
		b.textContent = texto; b.addEventListener("click", fn); return b;
	}
	function badge(texto, classe) {
		var td = document.createElement("td"), span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + classe; span.textContent = texto; td.appendChild(span); return td;
	}

	function linha(c) {
		var tr = document.createElement("tr");
		var m = window.FinanceiroFormatacao.moeda;
		tr.appendChild(celula(c.nome));
		tr.appendChild(celula(c.titularNome));
		tr.appendChild(celula(c.instituicaoNome));
		tr.appendChild(celula(BANDEIRA_LABEL[c.bandeira] || c.bandeira));
		tr.appendChild(celula(TIPO_LABEL[c.tipo] || c.tipo));
		tr.appendChild(celula(c.ultimosQuatroDigitos ? "**** " + c.ultimosQuatroDigitos : "—"));
		tr.appendChild(celula(c.limiteTotal != null ? m(c.limiteTotal) : "—"));
		tr.appendChild(celula(c.limiteSaudavel != null ? m(c.limiteSaudavel) : "—"));
		tr.appendChild(celula(c.limiteDisponivel != null ? m(c.limiteDisponivel) : "—"));
		tr.appendChild(celula(c.diaFechamento || "—"));
		tr.appendChild(celula(c.diaVencimento || "—"));
		var situacao = document.createElement("td");
		var statusSpan = document.createElement("span");
		statusSpan.className = "criati-badge criati-badge-" + c.status.toLowerCase();
		statusSpan.textContent = STATUS_LABEL[c.status] || c.status;
		situacao.appendChild(statusSpan);
		if (c.bloqueadoEfetivo) {
			var bloqueadoSpan = document.createElement("span");
			bloqueadoSpan.className = "criati-badge criati-badge-bloqueado";
			bloqueadoSpan.textContent = "Bloqueado";
			situacao.appendChild(document.createTextNode(" "));
			situacao.appendChild(bloqueadoSpan);
		}
		if (c.tipo === "VIRTUAL") {
			var virtualSpan = document.createElement("span");
			virtualSpan.className = "criati-badge criati-badge-virtual";
			virtualSpan.textContent = "Virtual";
			situacao.appendChild(document.createTextNode(" "));
			situacao.appendChild(virtualSpan);
		}
		tr.appendChild(situacao);
		tr.appendChild(acoes(c));
		return tr;
	}

	function acoes(c) {
		var td = document.createElement("td"); td.className = "criati-table-acoes";
		td.appendChild(botao("Detalhes", function () {
			window.FinanceiroDetalhes.abrir("Detalhes do cartão", [
				["Nome", c.nome], ["Titular", c.titularNome], ["Instituição", c.instituicaoNome],
				["Bandeira", c.bandeira], ["Tipo", c.tipo === "FISICO" ? "Físico" : "Virtual"],
				["Cartão principal", c.cartaoPrincipalNome], ["Final", c.ultimosDigitos],
				["Limite total", window.FinanceiroFormatacao.moeda(c.limiteTotal)],
				["Limite saudável", window.FinanceiroFormatacao.moeda(c.limiteSaudavel)],
				["Limite comprometido", window.FinanceiroFormatacao.moeda(c.limiteComprometido)],
				["Limite disponível", window.FinanceiroFormatacao.moeda(c.limiteDisponivel)],
				["Fechamento", c.diaFechamento ? "Dia " + c.diaFechamento : null],
				["Vencimento", c.diaVencimento ? "Dia " + c.diaVencimento : null],
				["Bloqueio", c.bloqueado ? "Bloqueado" : "Não bloqueado"],
				["Motivo do bloqueio", c.motivoBloqueio], ["Status", c.status]
			]);
		}));
		td.appendChild(botao("Editar", function () { abrirEdicao(c); }));
		if (c.tipo === "FISICO") {
			td.appendChild(botao("Virtuais (" + c.quantidadeCartoesVirtuais + ")", function () { abrirVirtuais(c); }));
		}
		if (c.bloqueado) {
			td.appendChild(botao("Desbloquear", function () { desbloquear(c); }));
		} else {
			td.appendChild(botao("Bloquear", function () { bloquear(c); }));
		}
		if (c.status === "ATIVO") {
			td.appendChild(botao("Desativar", function () { inativar(c); }));
		} else {
			td.appendChild(botao("Reativar", function () { reativar(c); }));
		}
		return td;
	}

	function sucesso(msg) { return function () { window.CriatiUI.showToast("sucesso", msg); carregar(); }; }
	function falha(msg) { return function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || msg); }; }

	function bloquear(c) {
		var motivo = prompt("Motivo do bloqueio (opcional):", "");
		if (motivo === null) return;
		window.FinanceiroApi.cartoes.bloquear(c.id, motivo).then(sucesso("Cartão bloqueado.")).catch(falha("Não foi possível bloquear."));
	}
	function desbloquear(c) {
		window.FinanceiroApi.cartoes.desbloquear(c.id).then(sucesso("Cartão desbloqueado.")).catch(falha("Não foi possível desbloquear."));
	}
	function inativar(c) {
		if (!confirm("Desativar preserva o histórico do cartão e ele deixa de ser opção ativa. Continuar?")) return;
		window.FinanceiroApi.cartoes.inativar(c.id).then(sucesso("Cartão desativado.")).catch(falha("Não foi possível desativar."));
	}
	function reativar(c) {
		window.FinanceiroApi.cartoes.reativar(c.id).then(sucesso("Cartão reativado.")).catch(falha("Não foi possível reativar."));
	}

	function abrirVirtuais(c) {
		window.FinanceiroApi.cartoes.virtuais(c.id).then(function (resp) {
			var itens = resp.data || [];
			var tbody = el("cartao-virtuais-tbody"); tbody.innerHTML = "";
			el("cartao-virtuais-vazio").hidden = !!itens.length;
			itens.forEach(function (v) {
				var tr = document.createElement("tr");
				tr.appendChild(celula(v.nome));
				tr.appendChild(celula(v.ultimosQuatroDigitos ? "**** " + v.ultimosQuatroDigitos : "—"));
				tr.appendChild(celula(STATUS_LABEL[v.status] || v.status));
				tbody.appendChild(tr);
			});
			el("cartao-virtuais-modal").hidden = false;
		}).catch(falha("Não foi possível carregar os cartões virtuais."));
	}

	function alternarCamposPorTipo() {
		var virtual = el("cartao-tipo").value === "VIRTUAL";
		el("cartao-principal-campo").hidden = !virtual;
		el("cartao-aviso-virtual").hidden = !virtual;
		el("cartao-limite-total-campo").hidden = virtual;
		el("cartao-limite-saudavel-campo").hidden = virtual;
		el("cartao-fechamento-campo").hidden = virtual;
		el("cartao-vencimento-campo").hidden = virtual;
		el("cartao-principal").required = virtual;
		el("cartao-limite-total").required = !virtual;
		el("cartao-fechamento").required = !virtual;
		el("cartao-vencimento").required = !virtual;
	}

	function preencherDadosDoPrincipal() {
		var principalId = el("cartao-principal").value;
		var principal = cartoesFisicos.filter(function (c) { return c.id === principalId; })[0];
		if (!principal) return;
		el("cartao-titular").value = principal.titularId;
		el("cartao-instituicao").value = principal.instituicaoId;
		el("cartao-bandeira").value = principal.bandeira;
	}

	function abrirCriacao() {
		cartaoEmEdicao = null;
		el("cartao-modal-titulo").textContent = "Cadastrar cartão";
		el("cartao-tipo").disabled = false;
		el("cartao-nome").value = ""; el("cartao-tipo").value = "FISICO"; el("cartao-principal").value = "";
		el("cartao-titular").value = ""; el("cartao-instituicao").value = ""; el("cartao-bandeira").value = "VISA";
		el("cartao-ultimos-digitos").value = ""; el("cartao-limite-total").value = ""; el("cartao-limite-saudavel").value = "";
		el("cartao-fechamento").value = ""; el("cartao-vencimento").value = ""; el("cartao-observacao").value = "";
		alternarCamposPorTipo();
		el("cartao-modal").hidden = false;
	}

	function abrirEdicao(c) {
		cartaoEmEdicao = c;
		el("cartao-modal-titulo").textContent = "Editar cartão";
		el("cartao-tipo").value = c.tipo;
		el("cartao-tipo").disabled = true;
		el("cartao-principal").value = c.cartaoPrincipalId || "";
		el("cartao-nome").value = c.nome;
		el("cartao-titular").value = c.titularId;
		el("cartao-instituicao").value = c.instituicaoId;
		el("cartao-bandeira").value = c.bandeira;
		el("cartao-ultimos-digitos").value = c.ultimosQuatroDigitos || "";
		el("cartao-limite-total").value = c.limiteTotal != null ? c.limiteTotal : "";
		el("cartao-limite-saudavel").value = c.limiteSaudavel != null ? c.limiteSaudavel : "";
		el("cartao-fechamento").value = c.diaFechamento || "";
		el("cartao-vencimento").value = c.diaVencimento || "";
		el("cartao-observacao").value = c.observacao || "";
		alternarCamposPorTipo();
		el("cartao-modal").hidden = false;
	}

	function fecharFormulario() {
		el("cartao-modal").hidden = true; el("cartao-form").reset(); el("cartao-tipo").disabled = false;
		cartaoEmEdicao = null;
	}

	function salvar(e) {
		e.preventDefault();
		var b = el("cartao-salvar");
		var virtual = el("cartao-tipo").value === "VIRTUAL";
		var d = {
			nome: el("cartao-nome").value,
			titularId: el("cartao-titular").value,
			instituicaoId: el("cartao-instituicao").value,
			bandeira: el("cartao-bandeira").value,
			ultimosQuatroDigitos: el("cartao-ultimos-digitos").value || null,
			limiteTotal: virtual ? null : (el("cartao-limite-total").value || null),
			limiteSaudavel: virtual ? null : (el("cartao-limite-saudavel").value || null),
			diaFechamento: virtual ? null : (el("cartao-fechamento").value || null),
			diaVencimento: virtual ? null : (el("cartao-vencimento").value || null),
			observacao: el("cartao-observacao").value
		};
		var chamada;
		if (cartaoEmEdicao) {
			chamada = window.FinanceiroApi.cartoes.editar(cartaoEmEdicao.id, d);
		} else {
			d.tipo = el("cartao-tipo").value;
			d.cartaoPrincipalId = virtual ? el("cartao-principal").value : null;
			chamada = window.FinanceiroApi.cartoes.criar(d);
		}
		window.CriatiUI.setButtonLoading(b, true, "Salvando...");
		chamada.then(function () { fecharFormulario(); sucesso("Cartão salvo.")(); })
			.catch(falha("Não foi possível salvar o cartão."))
			.finally(function () { window.CriatiUI.setButtonLoading(b, false); });
	}

	window.FinanceiroCartoes = { iniciar: iniciar };
})(window, document);
