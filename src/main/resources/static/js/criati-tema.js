/* Criati - tema claro/escuro/automatico: aplica, persiste (somente preferencia
   visual) e controla o seletor de tema. Nao depende de sessao nem de backend;
   funciona igual em paginas publicas e autenticadas. */
(function (window, document) {
	"use strict";

	var CHAVE_TEMA = "criati.tema";
	var VALORES_VALIDOS = ["auto", "light", "dark"];
	var consultaSistemaEscuro = window.matchMedia("(prefers-color-scheme: dark)");

	var ROTULO_TEMA = {
		auto: "Automatico",
		light: "Claro",
		dark: "Escuro"
	};

	function lerPreferencia() {
		try {
			var valor = window.localStorage.getItem(CHAVE_TEMA);
			return VALORES_VALIDOS.indexOf(valor) !== -1 ? valor : "auto";
		} catch (erro) {
			return "auto";
		}
	}

	function salvarPreferencia(valor) {
		try {
			window.localStorage.setItem(CHAVE_TEMA, valor);
		} catch (erro) {
			// localStorage indisponivel (modo privado, quota, etc.): a preferencia
			// simplesmente nao persiste entre visitas, sem quebrar a alternancia.
		}
	}

	function resolverTemaAplicado(preferencia) {
		if (preferencia === "auto") {
			return consultaSistemaEscuro.matches ? "dark" : "light";
		}
		return preferencia;
	}

	function aplicarTema(preferencia) {
		var temaAplicado = resolverTemaAplicado(preferencia);
		var raiz = document.documentElement;
		raiz.setAttribute("data-theme", temaAplicado);
		raiz.style.colorScheme = temaAplicado;
		document.dispatchEvent(new CustomEvent("criati:tema-alterado", {
			detail: { preferencia: preferencia, temaAplicado: temaAplicado }
		}));
		return temaAplicado;
	}

	function definirTema(preferencia) {
		if (VALORES_VALIDOS.indexOf(preferencia) === -1) {
			return;
		}
		salvarPreferencia(preferencia);
		aplicarTema(preferencia);
		atualizarControles();
	}

	function atualizarControles() {
		var preferenciaAtual = lerPreferencia();
		document.querySelectorAll(".criati-tema-controle").forEach(function (controle) {
			controle.setAttribute("data-tema-atual", preferenciaAtual);
			var toggle = controle.querySelector(".criati-tema-toggle");
			if (toggle) {
				toggle.setAttribute("aria-label", "Tema: " + ROTULO_TEMA[preferenciaAtual]);
			}
			controle.querySelectorAll(".criati-tema-opcao").forEach(function (opcao) {
				var selecionada = opcao.getAttribute("data-tema") === preferenciaAtual;
				opcao.setAttribute("aria-checked", String(selecionada));
			});
		});
	}

	function initControleTema() {
		document.querySelectorAll(".criati-tema-controle").forEach(function (controle) {
			var toggle = controle.querySelector(".criati-tema-toggle");
			var menu = controle.querySelector(".criati-tema-menu");
			if (!toggle || !menu) {
				return;
			}

			function fechar() {
				menu.hidden = true;
				toggle.setAttribute("aria-expanded", "false");
			}

			function abrir() {
				menu.hidden = false;
				toggle.setAttribute("aria-expanded", "true");
				var opcaoAtiva = menu.querySelector('.criati-tema-opcao[aria-checked="true"]')
					|| menu.querySelector(".criati-tema-opcao");
				if (opcaoAtiva) {
					opcaoAtiva.focus();
				}
			}

			toggle.addEventListener("click", function (evento) {
				evento.stopPropagation();
				if (menu.hidden) {
					abrir();
				} else {
					fechar();
				}
			});

			controle.querySelectorAll(".criati-tema-opcao").forEach(function (opcao) {
				opcao.addEventListener("click", function () {
					definirTema(opcao.getAttribute("data-tema"));
					fechar();
					toggle.focus();
				});
			});

			menu.addEventListener("keydown", function (evento) {
				if (evento.key === "Escape") {
					evento.preventDefault();
					fechar();
					toggle.focus();
					return;
				}
				if (evento.key !== "ArrowDown" && evento.key !== "ArrowUp") {
					return;
				}
				evento.preventDefault();
				var opcoes = Array.prototype.slice.call(controle.querySelectorAll(".criati-tema-opcao"));
				var indiceAtual = opcoes.indexOf(document.activeElement);
				var proximoIndice;
				if (indiceAtual === -1) {
					proximoIndice = 0;
				} else if (evento.key === "ArrowDown") {
					proximoIndice = (indiceAtual + 1) % opcoes.length;
				} else {
					proximoIndice = (indiceAtual - 1 + opcoes.length) % opcoes.length;
				}
				opcoes[proximoIndice].focus();
			});

			document.addEventListener("click", function (evento) {
				if (!menu.hidden && !controle.contains(evento.target)) {
					fechar();
				}
			});
		});

		atualizarControles();
	}

	consultaSistemaEscuro.addEventListener("change", function () {
		if (lerPreferencia() === "auto") {
			aplicarTema("auto");
		}
	});

	window.CriatiTema = {
		aplicar: definirTema,
		obterPreferencia: lerPreferencia,
		initControle: initControleTema
	};

	document.addEventListener("DOMContentLoaded", initControleTema);
})(window, document);
