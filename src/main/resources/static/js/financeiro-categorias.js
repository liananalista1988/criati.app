/* Criati Financeiro - administracao hierarquica de categorias. */
(function (window, document) {
	"use strict";
	var categoriaEmEdicao = null;
	var categoriasCache = [];
	function el(id) { return document.getElementById(id); }

	function iniciar() {
		if (!el("financeiro-categoria-form")) return;
		el("financeiro-categorias-nova-receita").addEventListener("click", function () { abrirModal(null, "RECEITA"); });
		el("financeiro-categorias-nova-despesa").addEventListener("click", function () { abrirModal(null, "DESPESA"); });
		el("financeiro-categoria-cancelar").addEventListener("click", fecharModal);
		el("financeiro-categoria-form").addEventListener("submit", salvar);
		["financeiro-categorias-filtro-status", "financeiro-categorias-filtro-tipo"].forEach(function (id) {
			el(id).addEventListener("change", carregar);
		});
		el("financeiro-categorias-busca").addEventListener("input", carregar);
		el("financeiro-categoria-tipo").addEventListener("change", preencherPais);
		carregar();
	}

	function carregar() {
		el("financeiro-categorias-carregando").hidden = false;
		el("financeiro-categorias-erro").hidden = true;
		el("financeiro-categorias-vazio").hidden = true;
		el("financeiro-categorias-conteudo").hidden = true;
		var filtros = { status: el("financeiro-categorias-filtro-status").value,
			tipo: el("financeiro-categorias-filtro-tipo").value, busca: el("financeiro-categorias-busca").value };
		Promise.all([window.FinanceiroApi.categorias.listar(filtros), window.FinanceiroApi.categorias.resumo()])
			.then(function (respostas) {
				categoriasCache = respostas[0].data || [];
				el("financeiro-categorias-carregando").hidden = true;
				renderResumo(respostas[1].data || {});
				if (!categoriasCache.length) { el("financeiro-categorias-vazio").hidden = false; return; }
				el("financeiro-categorias-conteudo").hidden = false;
				renderColuna("financeiro-categorias-receitas", "RECEITA");
				renderColuna("financeiro-categorias-despesas", "DESPESA");
			}).catch(function () {
				el("financeiro-categorias-carregando").hidden = true;
				el("financeiro-categorias-erro").hidden = false;
			});
	}

	function renderResumo(r) {
		el("financeiro-categorias-resumo").textContent = (r.ativas || 0) + " ativas · " +
			(r.principais || 0) + " principais · " + (r.subcategorias || 0) + " subcategorias · " +
			(r.comOrcamento || 0) + " habilitadas para orçamento";
	}

	function renderColuna(id, tipo) {
		var container = el(id); container.innerHTML = "";
		var itens = categoriasCache.filter(function (c) { return c.tipo === tipo; });
		if (!itens.length) { var p = document.createElement("p"); p.className = "criati-card-sub";
			p.textContent = "Nenhuma categoria cadastrada."; container.appendChild(p); return; }
		itens.filter(function (c) { return c.nivel === 1; }).forEach(function (principal) {
			container.appendChild(criarLinha(principal));
			itens.filter(function (c) { return c.categoriaPaiId === principal.id; }).forEach(function (filha) {
				container.appendChild(criarLinha(filha));
			});
		});
	}

	function criarLinha(categoria) {
		var linha = document.createElement("div"); linha.className = "criati-card";
		if (categoria.nivel === 2) linha.style.marginLeft = "20px";
		var nome = document.createElement("p"); nome.className = "criati-card-value";
		nome.textContent = (categoria.nivel === 2 ? "↳ " : "") + categoria.nome;
		var detalhe = document.createElement("p"); detalhe.className = "criati-card-sub";
		detalhe.textContent = "Ordem " + categoria.ordem + " · " +
			(categoria.permiteOrcamento ? "Permite orçamento" : "Sem orçamento") + " · " +
			categoria.quantidadeSubcategorias + " subcategorias";
		var status = document.createElement("span"); status.className = "criati-badge criati-badge-" + categoria.status.toLowerCase();
		status.textContent = categoria.status === "ATIVO" ? "Ativa" : "Inativa";
		var acoes = document.createElement("div"); acoes.className = "criati-table-acoes"; acoes.style.marginTop = "10px";
		var detalhes = document.createElement("button"); detalhes.type = "button"; detalhes.className = "criati-btn criati-btn-ghost";
		detalhes.textContent = "Detalhes"; detalhes.addEventListener("click", function () { abrirDetalhes(categoria); });
		var editar = document.createElement("button"); editar.type = "button"; editar.className = "criati-btn criati-btn-ghost";
		editar.textContent = "Editar"; editar.addEventListener("click", function () { abrirModal(categoria, categoria.tipo); });
		var alternar = document.createElement("button"); alternar.type = "button"; alternar.className = "criati-btn criati-btn-ghost";
		alternar.textContent = categoria.status === "ATIVO" ? "Inativar" : "Reativar";
		alternar.addEventListener("click", function () { alternarStatus(categoria); });
		acoes.appendChild(detalhes); acoes.appendChild(editar); acoes.appendChild(alternar);
		linha.appendChild(nome); linha.appendChild(detalhe); linha.appendChild(status); linha.appendChild(acoes); return linha;
	}

	function abrirDetalhes(categoria) {
		window.FinanceiroDetalhes.abrir("Detalhes da categoria", [
			["Nome", categoria.nome],
			["Natureza", categoria.tipo === "RECEITA" ? "Receita" : "Despesa"],
			["Descrição", categoria.descricao],
			["Categoria superior", categoria.categoriaPaiNome],
			["Nível", categoria.nivel],
			["Ordem", categoria.ordem],
			["Permite orçamento", categoria.permiteOrcamento ? "Sim" : "Não"],
			["Status", categoria.status === "ATIVO" ? "Ativa" : "Inativa"]
		]);
	}

	function alternarStatus(categoria) {
		if (categoria.status === "ATIVO" && !window.confirm("A categoria continuará visível no histórico. Deseja inativá-la?")) return;
		var chamada = categoria.status === "ATIVO" ? window.FinanceiroApi.categorias.inativar(categoria.id)
			: window.FinanceiroApi.categorias.reativar(categoria.id);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Categoria atualizada com sucesso."); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível atualizar a categoria."); });
	}

	function abrirModal(categoria, tipo) {
		categoriaEmEdicao = categoria;
		el("financeiro-categoria-modal-titulo").textContent = categoria ? "Editar categoria" : "Nova categoria";
		el("financeiro-categoria-nome").value = categoria ? categoria.nome : "";
		el("financeiro-categoria-descricao").value = categoria && categoria.descricao ? categoria.descricao : "";
		el("financeiro-categoria-tipo").value = tipo;
		el("financeiro-categoria-ordem").value = categoria ? categoria.ordem : 0;
		el("financeiro-categoria-orcamento").checked = categoria ? categoria.permiteOrcamento : tipo === "DESPESA";
		preencherPais();
		el("financeiro-categoria-pai").value = categoria && categoria.categoriaPaiId ? categoria.categoriaPaiId : "";
		el("financeiro-categoria-modal").hidden = false;
	}

	function preencherPais() {
		var select = el("financeiro-categoria-pai"); var atual = select.value; select.innerHTML = "";
		var opcao = document.createElement("option"); opcao.value = ""; opcao.textContent = "Categoria principal"; select.appendChild(opcao);
		categoriasCache.filter(function (c) { return c.status === "ATIVO" && c.nivel === 1 &&
			c.tipo === el("financeiro-categoria-tipo").value && (!categoriaEmEdicao || c.id !== categoriaEmEdicao.id); })
			.forEach(function (c) { var o = document.createElement("option"); o.value = c.id; o.textContent = c.nome; select.appendChild(o); });
		select.value = atual;
	}

	function fecharModal() { el("financeiro-categoria-modal").hidden = true; el("financeiro-categoria-form").reset(); categoriaEmEdicao = null; }
	function salvar(evento) {
		evento.preventDefault(); var botao = el("financeiro-categoria-salvar");
		var dados = { nome: el("financeiro-categoria-nome").value, descricao: el("financeiro-categoria-descricao").value,
			categoriaPaiId: el("financeiro-categoria-pai").value || null, tipo: el("financeiro-categoria-tipo").value,
			ordem: Number(el("financeiro-categoria-ordem").value), permiteOrcamento: el("financeiro-categoria-orcamento").checked };
		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		var chamada = categoriaEmEdicao ? window.FinanceiroApi.categorias.editar(categoriaEmEdicao.id, dados)
			: window.FinanceiroApi.categorias.criar(dados);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Categoria salva com sucesso."); fecharModal(); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível salvar a categoria."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botao, false); });
	}
	window.FinanceiroCategorias = { iniciar: iniciar };
})(window, document);
