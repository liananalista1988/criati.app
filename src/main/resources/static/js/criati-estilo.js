/* Criati - estilo visual Criati/Windows/Compacto: aplica, persiste (somente
   preferencia visual) e controla o seletor de estilo. Independente do tema
   (criati-tema.js): tema controla cor/contraste, estilo controla forma,
   tipografia e densidade. Nao depende de sessao nem de backend; funciona
   igual em paginas publicas e autenticadas. */
(function (window, document) {
	"use strict";

	var CHAVE_ESTILO = "criati.estilo";
	var VALORES_VALIDOS = ["criati", "windows", "compact"];
	var PADRAO = "criati";

	var ROTULO_ESTILO = {
		criati: "Criati",
		windows: "Windows",
		compact: "Compacto"
	};

	function lerPreferencia() {
		try {
			var valor = window.localStorage.getItem(CHAVE_ESTILO);
			return VALORES_VALIDOS.indexOf(valor) !== -1 ? valor : PADRAO;
		} catch (erro) {
			return PADRAO;
		}
	}

	function salvarPreferencia(valor) {
		try {
			window.localStorage.setItem(CHAVE_ESTILO, valor);
		} catch (erro) {
			// localStorage indisponivel (modo privado, quota, etc.): a preferencia
			// simplesmente nao persiste entre visitas, sem quebrar a alternancia.
		}
	}

	function aplicarEstilo(estilo) {
		document.documentElement.setAttribute("data-style", estilo);
		document.dispatchEvent(new CustomEvent("criati:estilo-alterado", {
			detail: { estilo: estilo }
		}));
	}

	function definirEstilo(estilo) {
		if (VALORES_VALIDOS.indexOf(estilo) === -1) {
			return;
		}
		salvarPreferencia(estilo);
		aplicarEstilo(estilo);
		atualizarControles();
	}

	function atualizarControles() {
		var preferenciaAtual = lerPreferencia();
		document.querySelectorAll(".criati-estilo-controle").forEach(function (controle) {
			controle.setAttribute("data-estilo-atual", preferenciaAtual);
			var toggle = controle.querySelector(".criati-estilo-toggle");
			if (toggle) {
				toggle.setAttribute("aria-label", "Estilo: " + ROTULO_ESTILO[preferenciaAtual]);
			}
			controle.querySelectorAll(".criati-estilo-opcao").forEach(function (opcao) {
				var selecionada = opcao.getAttribute("data-estilo") === preferenciaAtual;
				opcao.setAttribute("aria-checked", String(selecionada));
			});
		});
	}

	function initControleEstilo() {
		document.querySelectorAll(".criati-estilo-controle").forEach(function (controle) {
			var toggle = controle.querySelector(".criati-estilo-toggle");
			var menu = controle.querySelector(".criati-estilo-menu");
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
				var opcaoAtiva = menu.querySelector('.criati-estilo-opcao[aria-checked="true"]')
					|| menu.querySelector(".criati-estilo-opcao");
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

			controle.querySelectorAll(".criati-estilo-opcao").forEach(function (opcao) {
				opcao.addEventListener("click", function () {
					definirEstilo(opcao.getAttribute("data-estilo"));
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
				var opcoes = Array.prototype.slice.call(controle.querySelectorAll(".criati-estilo-opcao"));
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

	window.CriatiEstilo = {
		aplicar: definirEstilo,
		obterPreferencia: lerPreferencia,
		initControle: initControleEstilo
	};

	document.addEventListener("DOMContentLoaded", initControleEstilo);
})(window, document);
