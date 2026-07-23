/* Criati Financeiro - contatos financeiros (ParteFinanceira). */
(function (window, document) {
	"use strict";
	var emEdicao = null;
	var ROTULOS = { PESSOA: "Pessoa", ORGANIZACAO: "Organizacao", ESTABELECIMENTO: "Estabelecimento", OUTRA: "Outra" };
	function el(id) { return document.getElementById(id); }
	function td(texto) { var celula = document.createElement("td"); celula.textContent = texto || "-"; return celula; }
	function iniciar() {
		if (!el("contato-form")) { return; }
		el("contatos-novo").addEventListener("click", function () { abrir(null); }); el("contato-cancelar").addEventListener("click", fechar); el("contato-form").addEventListener("submit", salvar);
		["contatos-filtro-tipo", "contatos-filtro-status"].forEach(function (id) { el(id).addEventListener("change", carregar); });
		el("contatos-busca").addEventListener("input", carregar); carregar();
	}
	function carregar() {
		el("contatos-carregando").hidden = false; el("contatos-erro").hidden = true; el("contatos-vazio").hidden = true; el("contatos-tabela-wrap").hidden = true;
		window.FinanceiroApi.contatos.listar({ busca: el("contatos-busca").value, tipo: el("contatos-filtro-tipo").value, status: el("contatos-filtro-status").value }).then(function (resposta) {
			el("contatos-carregando").hidden = true; var contatos = resposta.data || []; if (!contatos.length) { el("contatos-vazio").hidden = false; return; }
			el("contatos-tabela-wrap").hidden = false; renderizar(contatos);
		}).catch(function () { el("contatos-carregando").hidden = true; el("contatos-erro").hidden = false; });
	}
	function renderizar(contatos) {
		var corpo = el("contatos-tbody"); corpo.innerHTML = ""; contatos.forEach(function (contato) {
			var linha = document.createElement("tr"); linha.appendChild(td(contato.nome)); linha.appendChild(td(ROTULOS[contato.tipo] || contato.tipo)); linha.appendChild(td(contato.documento)); linha.appendChild(td(contato.apelido));
			var status = td(""); var badge = document.createElement("span"); badge.className = "criati-badge criati-badge-" + contato.status.toLowerCase(); badge.textContent = contato.status === "ATIVO" ? "Ativo" : "Inativo"; status.appendChild(badge); linha.appendChild(status);
			var acoes = td(""); acoes.className = "criati-table-acoes"; var detalhes = document.createElement("button"); detalhes.type = "button"; detalhes.className = "criati-btn criati-btn-ghost"; detalhes.textContent = "Detalhes"; detalhes.addEventListener("click", function () { window.FinanceiroDetalhes.abrir("Detalhes do contato", [["Nome", contato.nome], ["Tipo", ROTULOS[contato.tipo] || contato.tipo], ["Documento", contato.documento], ["Apelido", contato.apelido], ["Observação", contato.observacao], ["Status", contato.status === "ATIVO" ? "Ativo" : "Inativo"]]); }); acoes.appendChild(detalhes); var editar = document.createElement("button"); editar.type = "button"; editar.className = "criati-btn criati-btn-ghost"; editar.textContent = "Editar"; editar.addEventListener("click", function () { abrir(contato); }); acoes.appendChild(editar);
			var alternar = document.createElement("button"); alternar.type = "button"; alternar.className = "criati-btn criati-btn-ghost"; alternar.textContent = contato.status === "ATIVO" ? "Desativar" : "Reativar"; alternar.addEventListener("click", function () { alternarStatus(contato); }); acoes.appendChild(alternar); linha.appendChild(acoes); corpo.appendChild(linha);
		});
	}
	function abrir(contato) { emEdicao = contato; el("contato-modal-titulo").textContent = contato ? "Editar contato" : "Novo contato"; el("contato-nome").value = contato ? contato.nome : ""; el("contato-tipo").value = contato ? contato.tipo : "PESSOA"; el("contato-documento").value = contato && contato.documento ? contato.documento : ""; el("contato-apelido").value = contato && contato.apelido ? contato.apelido : ""; el("contato-observacao").value = contato && contato.observacao ? contato.observacao : ""; el("contato-modal").hidden = false; el("contato-nome").focus(); }
	function fechar() { el("contato-modal").hidden = true; el("contato-form").reset(); emEdicao = null; }
	function salvar(evento) {
		evento.preventDefault(); var botao = el("contato-salvar"); var dados = { nome: el("contato-nome").value, tipo: el("contato-tipo").value, documento: el("contato-documento").value, apelido: el("contato-apelido").value, observacao: el("contato-observacao").value };
		window.CriatiUI.setButtonLoading(botao, true, "Salvando..."); var chamada = emEdicao ? window.FinanceiroApi.contatos.editar(emEdicao.id, dados) : window.FinanceiroApi.contatos.criar(dados);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Contato salvo com sucesso."); fechar(); carregar(); }).catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Nao foi possivel salvar o contato."); }).finally(function () { window.CriatiUI.setButtonLoading(botao, false); });
	}
	function alternarStatus(contato) {
		if (!window.confirm((contato.status === "ATIVO" ? "Desativar " : "Reativar ") + contato.nome + "?")) { return; }
		var chamada = contato.status === "ATIVO" ? window.FinanceiroApi.contatos.inativar(contato.id) : window.FinanceiroApi.contatos.reativar(contato.id);
		chamada.then(function () { window.CriatiUI.showToast("sucesso", "Contato atualizado com sucesso."); carregar(); }).catch(function (erro) { window.CriatiUI.showToast("erro", erro.message || "Nao foi possivel atualizar o contato."); });
	}
	window.FinanceiroContatos = { iniciar: iniciar };
})(window, document);
