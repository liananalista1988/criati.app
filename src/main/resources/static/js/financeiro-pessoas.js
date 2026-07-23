/* Criati Financeiro - pessoas da residencia. */
(function (window, document) {
	"use strict";
	var emEdicao = null;
	function el(id) { return document.getElementById(id); }
	function td(texto) { var celula = document.createElement("td"); celula.textContent = texto || "-"; return celula; }

	function iniciar() {
		if (!el("pessoa-form")) { return; }
		el("pessoas-nova").addEventListener("click", function () { abrir(null); });
		el("pessoa-cancelar").addEventListener("click", fechar);
		el("pessoa-form").addEventListener("submit", salvar);
		el("pessoas-filtro-status").addEventListener("change", carregar);
		carregar();
	}

	function carregar() {
		el("pessoas-carregando").hidden = false; el("pessoas-erro").hidden = true;
		el("pessoas-vazio").hidden = true; el("pessoas-tabela-wrap").hidden = true;
		window.FinanceiroApi.pessoas.listar({ status: el("pessoas-filtro-status").value }).then(function (resposta) {
			el("pessoas-carregando").hidden = true;
			var pessoas = resposta.data || [];
			if (!pessoas.length) { el("pessoas-vazio").hidden = false; return; }
			el("pessoas-tabela-wrap").hidden = false; renderizar(pessoas);
		}).catch(function () { el("pessoas-carregando").hidden = true; el("pessoas-erro").hidden = false; });
	}

	function renderizar(pessoas) {
		var corpo = el("pessoas-tbody"); corpo.innerHTML = "";
		pessoas.forEach(function (pessoa) {
			var linha = document.createElement("tr");
			linha.appendChild(td(pessoa.nome)); linha.appendChild(td(pessoa.apelido));
			linha.appendChild(td(pessoa.usuarioNome ? pessoa.usuarioNome + " · " + pessoa.usuarioEmail : "Sem usuario"));
			var status = td(""); var badge = document.createElement("span"); badge.className = "criati-badge criati-badge-" + pessoa.status.toLowerCase(); badge.textContent = pessoa.status === "ATIVO" ? "Ativa" : "Inativa"; status.appendChild(badge); linha.appendChild(status);
			var acoes = td(""); acoes.className = "criati-table-acoes";
			var detalhes = document.createElement("button"); detalhes.type = "button"; detalhes.className = "criati-btn criati-btn-ghost"; detalhes.textContent = "Detalhes"; detalhes.addEventListener("click", function () { window.FinanceiroDetalhes.abrir("Detalhes da pessoa", [["Nome", pessoa.nome], ["Apelido ou descrição", pessoa.apelido], ["Usuário vinculado", pessoa.usuarioNome], ["E-mail do usuário", pessoa.usuarioEmail], ["Status", pessoa.status === "ATIVO" ? "Ativa" : "Inativa"]]); }); acoes.appendChild(detalhes);
			var editar = document.createElement("button"); editar.type = "button"; editar.className = "criati-btn criati-btn-ghost"; editar.textContent = "Editar"; editar.addEventListener("click", function () { abrir(pessoa); }); acoes.appendChild(editar);
			var alternar = document.createElement("button"); alternar.type = "button"; alternar.className = "criati-btn criati-btn-ghost"; alternar.textContent = pessoa.status === "ATIVO" ? "Desativar" : "Reativar"; alternar.addEventListener("click", function () { alternarStatus(pessoa); }); acoes.appendChild(alternar);
			linha.appendChild(acoes); corpo.appendChild(linha);
		});
	}

	function carregarUsuarios(selecionado) {
		var campo = el("pessoa-usuario"); campo.innerHTML = '<option value="">Sem acesso ao sistema</option>';
		return window.FinanceiroApi.usuariosVinculaveis().then(function (resposta) {
			(resposta.data || []).forEach(function (usuario) { var opcao = document.createElement("option"); opcao.value = usuario.id; opcao.textContent = usuario.nome + " · " + usuario.email; campo.appendChild(opcao); });
			campo.value = selecionado || "";
		});
	}

	function abrir(pessoa) {
		emEdicao = pessoa; el("pessoa-modal-titulo").textContent = pessoa ? "Editar pessoa" : "Nova pessoa";
		el("pessoa-nome").value = pessoa ? pessoa.nome : ""; el("pessoa-apelido").value = pessoa && pessoa.apelido ? pessoa.apelido : "";
		carregarUsuarios(pessoa ? pessoa.usuarioId : null).then(function () { el("pessoa-modal").hidden = false; el("pessoa-nome").focus(); }).catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Nao foi possivel listar os usuarios."); });
	}

	function fechar() { el("pessoa-modal").hidden = true; el("pessoa-form").reset(); emEdicao = null; }
	function salvar(evento) {
		evento.preventDefault(); var botao = el("pessoa-salvar"); var usuarioId = el("pessoa-usuario").value;
		var dados = { nome: el("pessoa-nome").value, apelido: el("pessoa-apelido").value, usuarioId: usuarioId || null };
		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		var chamada = emEdicao ? window.FinanceiroApi.pessoas.editar(emEdicao.id, dados) : window.FinanceiroApi.pessoas.criar(dados);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Pessoa salva com sucesso."); fechar(); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Nao foi possivel salvar a pessoa."); })
			.finally(function () { window.CriatiUI.setButtonLoading(botao, false); });
	}

	function alternarStatus(pessoa) {
		if (!window.confirm((pessoa.status === "ATIVO" ? "Desativar " : "Reativar ") + pessoa.nome + "?")) { return; }
		var chamada = pessoa.status === "ATIVO" ? window.FinanceiroApi.pessoas.inativar(pessoa.id) : window.FinanceiroApi.pessoas.reativar(pessoa.id);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Pessoa atualizada com sucesso."); carregar(); })
			.catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Nao foi possivel atualizar a pessoa."); });
	}
	window.FinanceiroPessoas = { iniciar: iniciar };
})(window, document);
