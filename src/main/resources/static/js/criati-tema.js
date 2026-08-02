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

	/* ---------------------------------------------------------------------
	 * Personalizacao de cores do menu lateral, area de conteudo e topo
	 * (CRIATI-UX-002), independente por tema claro/escuro. Vive neste mesmo
	 * arquivo (em vez de um modulo proprio) porque criati-tema.js ja e
	 * carregado em toda pagina autenticada e publica - assim o botao de
	 * cores funciona em qualquer tela sem precisar adicionar mais uma tag
	 * <script> em dezenas de templates. Preferencia somente visual, guardada
	 * no navegador (mesmo padrao do restante deste arquivo).
	 * ------------------------------------------------------------------- */

	var CHAVE_CORES = "criati.cores";
	var TEMAS_CORES = ["light", "dark"];
	var CAMPOS_CORES = [
		{ chave: "sidebar", variavel: "--criati-sidebar-bg", rotulo: "menu lateral" },
		{ chave: "conteudo", variavel: "--criati-content-bg", rotulo: "área de conteúdo" },
		{ chave: "topo", variavel: "--criati-topbar-bg", rotulo: "topo" }
	];
	// Cor de texto padrao de cada tema (ver criati-base.css) - usada somente
	// para calcular o aviso de contraste, nunca para decidir a cor aplicada.
	var TEXTO_PADRAO_POR_TEMA = { light: "#1e2530", dark: "#f5f7fa" };
	var CONTRASTE_MINIMO_AA = 4.5;

	function lerCoresSalvas() {
		try {
			var bruto = window.localStorage.getItem(CHAVE_CORES);
			var dados = bruto ? JSON.parse(bruto) : {};
			return { light: dados.light || {}, dark: dados.dark || {} };
		} catch (erro) {
			return { light: {}, dark: {} };
		}
	}

	function salvarCores(dados) {
		try {
			window.localStorage.setItem(CHAVE_CORES, JSON.stringify(dados));
		} catch (erro) {
			// localStorage indisponivel: a preferencia nao persiste, sem quebrar a tela.
		}
	}

	function aplicarCores() {
		var tema = resolverTemaAplicado(lerPreferencia());
		var cores = lerCoresSalvas()[tema] || {};
		var raiz = document.documentElement;
		CAMPOS_CORES.forEach(function (campo) {
			if (cores[campo.chave]) {
				raiz.style.setProperty(campo.variavel, cores[campo.chave]);
			} else {
				raiz.style.removeProperty(campo.variavel);
			}
		});
	}

	function definirCor(tema, chave, valorHex) {
		if (TEMAS_CORES.indexOf(tema) === -1) {
			return;
		}
		var dados = lerCoresSalvas();
		dados[tema][chave] = valorHex;
		salvarCores(dados);
		if (tema === resolverTemaAplicado(lerPreferencia())) {
			aplicarCores();
		}
	}

	function restaurarCoresPadrao() {
		salvarCores({ light: {}, dark: {} });
		aplicarCores();
	}

	function obterCores(tema) {
		return lerCoresSalvas()[tema] || {};
	}

	function canalLinear(c) {
		return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
	}

	function luminanciaRelativa(hex) {
		var valor = (hex || "").replace("#", "");
		if (valor.length === 3) {
			valor = valor.charAt(0) + valor.charAt(0) + valor.charAt(1) + valor.charAt(1) + valor.charAt(2) + valor.charAt(2);
		}
		if (valor.length !== 6) {
			return null;
		}
		var r = canalLinear(parseInt(valor.substring(0, 2), 16) / 255);
		var g = canalLinear(parseInt(valor.substring(2, 4), 16) / 255);
		var b = canalLinear(parseInt(valor.substring(4, 6), 16) / 255);
		return 0.2126 * r + 0.7152 * g + 0.0722 * b;
	}

	// Razao de contraste WCAG entre duas cores (1 a 21); null quando alguma
	// cor nao pode ser interpretada.
	function razaoContraste(hexA, hexB) {
		var lA = luminanciaRelativa(hexA);
		var lB = luminanciaRelativa(hexB);
		if (lA === null || lB === null) {
			return null;
		}
		var maior = Math.max(lA, lB) + 0.05;
		var menor = Math.min(lA, lB) + 0.05;
		return maior / menor;
	}

	function contrasteSuficiente(corFundo, tema) {
		var razao = razaoContraste(corFundo, TEXTO_PADRAO_POR_TEMA[tema]);
		return razao === null || razao >= CONTRASTE_MINIMO_AA;
	}

	function elCores(id) {
		return document.getElementById(id);
	}

	// <input type="color"> so aceita #rrggbb; a cor computada pode vir como
	// rgb(...) - normaliza via expressao regular (conversao simples, sem
	// custo de canvas).
	function corParaHex(valorCss) {
		if (!valorCss) {
			return "#000000";
		}
		if (/^#[0-9a-fA-F]{6}$/.test(valorCss)) {
			return valorCss;
		}
		var casamento = valorCss.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
		if (!casamento) {
			return "#000000";
		}
		return "#" + [casamento[1], casamento[2], casamento[3]].map(function (parte) {
			var hex = Number(parte).toString(16);
			return hex.length === 1 ? "0" + hex : hex;
		}).join("");
	}

	function corAtualOuPadrao(tema, chave) {
		var cores = obterCores(tema);
		if (cores[chave]) {
			return cores[chave];
		}
		var variavel = CAMPOS_CORES.filter(function (campo) { return campo.chave === chave; })[0].variavel;
		var valor = window.getComputedStyle(document.documentElement).getPropertyValue(variavel).trim();
		return corParaHex(valor);
	}

	function atualizarAvisoContraste(tema) {
		var avisoEl = elCores("cores-" + tema + "-aviso-contraste");
		if (!avisoEl) {
			return;
		}
		var camposComBaixoContraste = CAMPOS_CORES.filter(function (campo) {
			var input = elCores("cores-" + tema + "-" + campo.chave);
			return input && !contrasteSuficiente(input.value, tema);
		}).map(function (campo) { return campo.rotulo; });
		if (camposComBaixoContraste.length === 0) {
			avisoEl.hidden = true;
			return;
		}
		avisoEl.hidden = false;
		avisoEl.textContent = "Contraste baixo com o texto do tema " + (tema === "light" ? "claro" : "escuro")
			+ " em: " + camposComBaixoContraste.join(", ") + ". Considere uma cor mais "
			+ (tema === "light" ? "escura" : "clara") + ".";
	}

	function preencherFormularioCores() {
		TEMAS_CORES.forEach(function (tema) {
			CAMPOS_CORES.forEach(function (campo) {
				var input = elCores("cores-" + tema + "-" + campo.chave);
				if (input) {
					input.value = corAtualOuPadrao(tema, campo.chave);
				}
			});
			atualizarAvisoContraste(tema);
		});
	}

	function initControleCores() {
		var botaoAbrir = elCores("criati-cores-abrir");
		var modalEl = elCores("criati-cores-modal");
		if (!botaoAbrir || !modalEl || botaoAbrir.dataset.inicializado === "true" || !window.CriatiUI) {
			return;
		}
		botaoAbrir.dataset.inicializado = "true";
		var modal = window.CriatiUI.criarModal(modalEl);

		botaoAbrir.addEventListener("click", function () {
			preencherFormularioCores();
			modal.abrir();
		});
		elCores("criati-cores-fechar").addEventListener("click", function () {
			modal.fechar();
		});
		elCores("criati-cores-restaurar").addEventListener("click", function () {
			if (!window.confirm("Restaurar as cores padrão da Criati para os temas claro e escuro?")) {
				return;
			}
			restaurarCoresPadrao();
			preencherFormularioCores();
			window.CriatiUI.showToast("sucesso", "Cores restauradas para o padrão Criati.");
		});
		TEMAS_CORES.forEach(function (tema) {
			CAMPOS_CORES.forEach(function (campo) {
				var input = elCores("cores-" + tema + "-" + campo.chave);
				if (!input) {
					return;
				}
				input.addEventListener("input", function () {
					definirCor(tema, campo.chave, input.value);
					atualizarAvisoContraste(tema);
				});
			});
		});
	}

	window.CriatiCores = {
		aplicar: aplicarCores,
		definirCor: definirCor,
		restaurarPadrao: restaurarCoresPadrao,
		obterCores: obterCores,
		razaoContraste: razaoContraste
	};

	aplicarCores();
	document.addEventListener("DOMContentLoaded", function () {
		aplicarCores();
		initControleCores();
	});
	document.addEventListener("criati:tema-alterado", aplicarCores);
})(window, document);
