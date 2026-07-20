/* Criati Financeiro - tela de categorias: listar (separadas por tipo), criar, editar, inativar, reativar. */
(function (window, document) {
	"use strict";

	var categoriaEmEdicaoId = null;

	function el(id) {
		return document.getElementById(id);
	}

	function iniciar() {
		var form = el("financeiro-categoria-form");
		if (!form) {
			return;
		}

		el("financeiro-categorias-nova-receita").addEventListener("click", function () {
			abrirModal(null, "RECEITA");
		});
		el("financeiro-categorias-nova-despesa").addEventListener("click", function () {
			abrirModal(null, "DESPESA");
		});
		el("financeiro-categoria-cancelar").addEventListener("click", fecharModal);
		el("financeiro-categorias-filtro-status").addEventListener("change", carregar);
		form.addEventListener("submit", salvar);

		carregar();
	}

	function carregar() {
		var carregando = el("financeiro-categorias-carregando");
		var erro = el("financeiro-categorias-erro");
		var vazio = el("financeiro-categorias-vazio");
		var conteudo = el("financeiro-categorias-conteudo");

		carregando.hidden = false;
		erro.hidden = true;
		vazio.hidden = true;
		conteudo.hidden = true;

		var filtros = { status: el("financeiro-categorias-filtro-status").value };

		window.FinanceiroApi.categorias
			.listar(filtros)
			.then(function (resposta) {
				carregando.hidden = true;
				var categorias = resposta.data || [];
				if (categorias.length === 0) {
					vazio.hidden = false;
					return;
				}
				conteudo.hidden = false;
				renderColuna("financeiro-categorias-receitas", categorias.filter(function (c) {
					return c.tipo === "RECEITA";
				}));
				renderColuna("financeiro-categorias-despesas", categorias.filter(function (c) {
					return c.tipo === "DESPESA";
				}));
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderColuna(containerId, categorias) {
		var container = el(containerId);
		container.innerHTML = "";
		if (categorias.length === 0) {
			var vazio = document.createElement("p");
			vazio.className = "criati-card-sub";
			vazio.textContent = "Nenhuma categoria cadastrada.";
			container.appendChild(vazio);
			return;
		}
		categorias.forEach(function (categoria) {
			container.appendChild(criarLinha(categoria));
		});
	}

	function criarLinha(categoria) {
		var linha = document.createElement("div");
		linha.className = "criati-card";

		var nome = document.createElement("p");
		nome.className = "criati-card-value";
		nome.textContent = categoria.nome;

		var status = document.createElement("span");
		status.className = "criati-badge criati-badge-" + categoria.status.toLowerCase();
		status.textContent = categoria.status === "ATIVO" ? "Ativa" : "Inativa";

		var acoes = document.createElement("div");
		acoes.className = "criati-table-acoes";
		acoes.style.marginTop = "10px";

		var botaoEditar = document.createElement("button");
		botaoEditar.type = "button";
		botaoEditar.className = "criati-btn criati-btn-ghost";
		botaoEditar.textContent = "Editar";
		botaoEditar.addEventListener("click", function () {
			abrirModal(categoria, categoria.tipo);
		});

		var botaoStatus = document.createElement("button");
		botaoStatus.type = "button";
		botaoStatus.className = "criati-btn criati-btn-ghost";
		botaoStatus.textContent = categoria.status === "ATIVO" ? "Inativar" : "Reativar";
		botaoStatus.addEventListener("click", function () {
			alternarStatus(categoria);
		});

		acoes.appendChild(botaoEditar);
		acoes.appendChild(botaoStatus);

		linha.appendChild(nome);
		linha.appendChild(status);
		linha.appendChild(acoes);
		return linha;
	}

	function alternarStatus(categoria) {
		var chamada = categoria.status === "ATIVO"
			? window.FinanceiroApi.categorias.inativar(categoria.id)
			: window.FinanceiroApi.categorias.reativar(categoria.id);

		chamada
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Categoria atualizada com sucesso.");
				carregar();
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel atualizar essa categoria agora.");
			});
	}

	function abrirModal(categoria, tipo) {
		categoriaEmEdicaoId = categoria ? categoria.id : null;
		el("financeiro-categoria-modal-titulo").textContent = categoria ? "Editar categoria" : "Nova categoria";
		el("financeiro-categoria-nome").value = categoria ? categoria.nome : "";
		el("financeiro-categoria-tipo").value = tipo;
		el("financeiro-categoria-modal").hidden = false;
	}

	function fecharModal() {
		el("financeiro-categoria-modal").hidden = true;
		el("financeiro-categoria-form").reset();
		categoriaEmEdicaoId = null;
	}

	function salvar(evento) {
		evento.preventDefault();
		var botaoSalvar = el("financeiro-categoria-salvar");
		var dados = {
			nome: el("financeiro-categoria-nome").value,
			tipo: el("financeiro-categoria-tipo").value
		};

		window.CriatiUI.setButtonLoading(botaoSalvar, true, "Salvando...");
		var chamada = categoriaEmEdicaoId
			? window.FinanceiroApi.categorias.editar(categoriaEmEdicaoId, dados)
			: window.FinanceiroApi.categorias.criar(dados);

		chamada
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Categoria salva com sucesso.");
				fecharModal();
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel salvar a categoria.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botaoSalvar, false);
			});
	}

	window.FinanceiroCategorias = { iniciar: iniciar };
})(window, document);
