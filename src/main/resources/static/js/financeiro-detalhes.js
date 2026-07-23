(function () {
	"use strict";

	var overlay;
	var titulo;
	var lista;
	var fechar;

	function criar() {
		if (overlay) return;
		overlay = document.createElement("div");
		overlay.className = "criati-modal-overlay";
		overlay.hidden = true;

		var modal = document.createElement("div");
		modal.className = "criati-modal";
		modal.setAttribute("role", "dialog");
		modal.setAttribute("aria-modal", "true");
		modal.setAttribute("aria-labelledby", "financeiro-detalhes-titulo");

		titulo = document.createElement("h2");
		titulo.id = "financeiro-detalhes-titulo";
		lista = document.createElement("dl");
		lista.className = "financeiro-detalhes-lista";

		var acoes = document.createElement("div");
		acoes.className = "criati-modal-acoes";
		fechar = document.createElement("button");
		fechar.type = "button";
		fechar.className = "criati-btn criati-btn-primary";
		fechar.textContent = "Fechar";
		fechar.addEventListener("click", ocultar);
		acoes.appendChild(fechar);

		modal.append(titulo, lista, acoes);
		overlay.appendChild(modal);
		overlay.addEventListener("click", function (evento) {
			if (evento.target === overlay) ocultar();
		});
		document.body.appendChild(overlay);
	}

	function abrir(textoTitulo, campos) {
		criar();
		titulo.textContent = textoTitulo;
		lista.replaceChildren();
		campos.forEach(function (campo) {
			var termo = document.createElement("dt");
			termo.textContent = campo[0];
			var descricao = document.createElement("dd");
			descricao.textContent = campo[1] == null || campo[1] === "" ? "—" : String(campo[1]);
			lista.append(termo, descricao);
		});
		overlay.hidden = false;
		fechar.focus();
	}

	function ocultar() {
		if (overlay) overlay.hidden = true;
	}

	window.FinanceiroDetalhes = { abrir: abrir };
}());
