/* Criati - componentes de interface: toasts, loading, menu mobile, menu do usuario, senha visivel. */
(function (window, document) {
	"use strict";

	function showToast(tipo, mensagem) {
		var container = document.getElementById("criati-toasts");
		if (!container) {
			return;
		}
		var toast = document.createElement("div");
		toast.className = "criati-toast criati-toast-" + tipo;
		toast.setAttribute("role", tipo === "erro" ? "alert" : "status");
		toast.textContent = mensagem;
		container.appendChild(toast);
		window.setTimeout(function () {
			if (toast.parentNode) {
				toast.parentNode.removeChild(toast);
			}
		}, 5000);
	}

	function setButtonLoading(button, loading, loadingText) {
		if (!button) {
			return;
		}
		if (loading) {
			button.dataset.textoOriginal = button.dataset.textoOriginal || button.textContent;
			button.disabled = true;
			button.setAttribute("aria-busy", "true");
			button.textContent = loadingText || "Carregando...";
		} else {
			button.disabled = false;
			button.removeAttribute("aria-busy");
			if (button.dataset.textoOriginal) {
				button.textContent = button.dataset.textoOriginal;
			}
		}
	}

	function initTogglePassword(toggleButton, input) {
		if (!toggleButton || !input) {
			return;
		}
		toggleButton.addEventListener("click", function () {
			var oculto = input.type === "password";
			input.type = oculto ? "text" : "password";
			toggleButton.setAttribute("aria-label", oculto ? "Ocultar senha" : "Mostrar senha");
			toggleButton.setAttribute("aria-pressed", String(oculto));
		});
	}

	var CHAVE_SIDEBAR_RECOLHIDA = "criati.sidebar.recolhida";
	var CONSULTA_MOBILE = "(max-width: 768px)";

	function ehMobile() {
		return window.matchMedia(CONSULTA_MOBILE).matches;
	}

	function lerPreferenciaRecolhida() {
		try {
			return window.localStorage.getItem(CHAVE_SIDEBAR_RECOLHIDA) === "true";
		} catch (erro) {
			return false;
		}
	}

	function salvarPreferenciaRecolhida(recolhida) {
		try {
			window.localStorage.setItem(CHAVE_SIDEBAR_RECOLHIDA, String(recolhida));
		} catch (erro) {
			// localStorage indisponivel (modo privado, quota, etc.): a preferencia
			// simplesmente nao persiste entre visitas, sem quebrar a alternancia.
		}
	}

	function initSidebarToggle() {
		var app = document.querySelector(".criati-app");
		var toggle = document.querySelector(".criati-menu-toggle");
		var overlay = document.querySelector(".criati-sidebar-overlay");
		var sidebar = document.getElementById("criati-sidebar");
		var raiz = document.documentElement;
		if (!app || !toggle) {
			return;
		}
		if (app.dataset.sidebarInicializada === "true") {
			return;
		}
		app.dataset.sidebarInicializada = "true";

		function atualizarAria() {
			if (ehMobile()) {
				var aberto = app.classList.contains("is-sidebar-open");
				toggle.setAttribute("aria-expanded", String(aberto));
				toggle.setAttribute("aria-label", aberto ? "Fechar menu" : "Abrir menu");
			} else {
				var recolhida = raiz.classList.contains("criati-sidebar-collapsed");
				toggle.setAttribute("aria-expanded", String(!recolhida));
				toggle.setAttribute("aria-label", recolhida ? "Expandir menu" : "Recolher menu");
			}
		}

		function fecharMobile() {
			if (!app.classList.contains("is-sidebar-open")) {
				return;
			}
			app.classList.remove("is-sidebar-open");
			atualizarAria();
		}

		// No mobile a sidebar nunca aplica o modo compacto (somente aberta ou
		// fechada); ao voltar para desktop, a preferencia salva e reaplicada.
		function aplicarEstadoConformeViewport() {
			if (ehMobile()) {
				raiz.classList.remove("criati-sidebar-collapsed");
			} else {
				app.classList.remove("is-sidebar-open");
				raiz.classList.toggle("criati-sidebar-collapsed", lerPreferenciaRecolhida());
			}
			atualizarAria();
		}

		toggle.addEventListener("click", function () {
			if (ehMobile()) {
				var aberto = app.classList.toggle("is-sidebar-open");
				if (aberto) {
					var primeiroLink = app.querySelector(".criati-nav-link");
					if (primeiroLink) {
						window.setTimeout(function () {
							primeiroLink.focus();
						}, 0);
					}
				}
			} else {
				var recolhida = raiz.classList.toggle("criati-sidebar-collapsed");
				salvarPreferenciaRecolhida(recolhida);
			}
			atualizarAria();
		});

		if (overlay) {
			overlay.addEventListener("click", fecharMobile);
		}

		if (sidebar) {
			sidebar.addEventListener("click", function (evento) {
				if (evento.target.closest(".criati-nav-link") && ehMobile()) {
					fecharMobile();
				}
			});
		}

		document.addEventListener("keydown", function (evento) {
			if (evento.key === "Escape" && app.classList.contains("is-sidebar-open")) {
				fecharMobile();
				toggle.focus();
			}
		});

		var reidimensionarPendente;
		window.addEventListener("resize", function () {
			window.clearTimeout(reidimensionarPendente);
			reidimensionarPendente = window.setTimeout(aplicarEstadoConformeViewport, 120);
		});

		aplicarEstadoConformeViewport();
	}

	function initFiltrosRecolhiveis() {
		document.querySelectorAll("[data-filtros-recolhiveis]").forEach(function (container) {
			if (container.dataset.filtrosInicializados === "true") {
				return;
			}
			var botao = container.querySelector("[data-filtros-toggle]");
			var painel = container.querySelector("[data-filtros-painel]");
			var contador = container.querySelector("[data-filtros-contador]");
			if (!botao || !painel) {
				return;
			}
			container.dataset.filtrosInicializados = "true";

			function atualizarContador() {
				var ativos = Array.prototype.filter.call(
					painel.querySelectorAll("input, select, textarea"),
					function (campo) {
						var padrao = campo.getAttribute("data-filtro-padrao");
						if (padrao === null) {
							padrao = "";
						}
						return campo.value !== padrao;
					}
				).length;
				if (contador) {
					contador.textContent = ativos ? String(ativos) : "";
					contador.hidden = ativos === 0;
					contador.setAttribute("aria-label", ativos + " filtro(s) ativo(s)");
				}
			}

			function alternar() {
				var abrir = painel.hidden;
				painel.hidden = !abrir;
				botao.setAttribute("aria-expanded", String(abrir));
			}

			botao.addEventListener("click", alternar);
			painel.addEventListener("input", atualizarContador);
			painel.addEventListener("change", atualizarContador);
			atualizarContador();
		});
	}

	function initUserMenu() {
		var trigger = document.querySelector(".criati-user-trigger");
		var dropdown = document.querySelector(".criati-user-dropdown");
		if (!trigger || !dropdown) {
			return;
		}

		function fechar() {
			dropdown.hidden = true;
			trigger.setAttribute("aria-expanded", "false");
		}

		trigger.addEventListener("click", function (evento) {
			evento.stopPropagation();
			var aberto = dropdown.hidden;
			dropdown.hidden = !aberto;
			trigger.setAttribute("aria-expanded", String(aberto));
		});

		document.addEventListener("click", function (evento) {
			if (!dropdown.hidden && !dropdown.contains(evento.target)) {
				fechar();
			}
		});

		document.addEventListener("keydown", function (evento) {
			if (evento.key === "Escape") {
				fechar();
				trigger.focus();
			}
		});
	}

	var SELETOR_FOCAVEL = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), '
		+ 'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

	/**
	 * Cria um controlador acessivel para um overlay de modal (elemento com
	 * atributo `hidden` alternado para abrir/fechar). Cuida de: foco inicial
	 * dentro do modal, ciclo de Tab preso ao modal (focus trap), fechar com
	 * Esc e devolver o foco ao elemento que abriu o modal. O HTML do modal
	 * deve trazer `role="dialog"`, `aria-modal="true"` e `aria-labelledby`
	 * apontando para o titulo - isso fica no template, nao aqui.
	 */
	function criarModal(overlayEl) {
		var elementoAnterior = null;

		function elementosFocaveis() {
			return Array.prototype.slice.call(overlayEl.querySelectorAll(SELETOR_FOCAVEL));
		}

		function aoPressionarTecla(evento) {
			if (evento.key === "Escape") {
				evento.preventDefault();
				fechar();
				return;
			}
			if (evento.key !== "Tab") {
				return;
			}
			var focaveis = elementosFocaveis();
			if (focaveis.length === 0) {
				return;
			}
			var primeiro = focaveis[0];
			var ultimo = focaveis[focaveis.length - 1];
			if (evento.shiftKey && document.activeElement === primeiro) {
				evento.preventDefault();
				ultimo.focus();
			} else if (!evento.shiftKey && document.activeElement === ultimo) {
				evento.preventDefault();
				primeiro.focus();
			}
		}

		function abrir(elementoParaFoco) {
			elementoAnterior = document.activeElement;
			overlayEl.hidden = false;
			document.addEventListener("keydown", aoPressionarTecla);
			window.setTimeout(function () {
				var alvo = elementoParaFoco || elementosFocaveis()[0];
				if (alvo) {
					alvo.focus();
				}
			}, 0);
		}

		function fechar() {
			overlayEl.hidden = true;
			document.removeEventListener("keydown", aoPressionarTecla);
			if (elementoAnterior && typeof elementoAnterior.focus === "function") {
				elementoAnterior.focus();
			}
			elementoAnterior = null;
		}

		overlayEl.addEventListener("click", function (evento) {
			if (evento.target === overlayEl) {
				fechar();
			}
		});

		return { abrir: abrir, fechar: fechar };
	}

	/**
	 * Confirmacao acessivel reutilizavel (substitui window.confirm nativo):
	 * monta um dialogo a partir de um overlay/modal ja existente no HTML,
	 * preenche titulo/mensagem/rotulo do botao e resolve numa Promise<boolean>.
	 * @param {HTMLElement} overlayEl
	 * @param {{tituloEl: HTMLElement, mensagemEl: HTMLElement, confirmarBtn: HTMLElement, cancelarBtn: HTMLElement}} partes
	 * @param {{titulo: string, mensagem: string, rotuloConfirmar: string}} conteudo
	 */
	function confirmarAcao(overlayEl, partes, conteudo) {
		var modal = criarModal(overlayEl);
		partes.tituloEl.textContent = conteudo.titulo;
		partes.mensagemEl.textContent = conteudo.mensagem;
		partes.confirmarBtn.textContent = conteudo.rotuloConfirmar || "Confirmar";

		return new Promise(function (resolve) {
			function limpar() {
				partes.confirmarBtn.removeEventListener("click", aoConfirmar);
				partes.cancelarBtn.removeEventListener("click", aoCancelar);
			}
			function aoConfirmar() {
				limpar();
				modal.fechar();
				resolve(true);
			}
			function aoCancelar() {
				limpar();
				modal.fechar();
				resolve(false);
			}
			partes.confirmarBtn.addEventListener("click", aoConfirmar);
			partes.cancelarBtn.addEventListener("click", aoCancelar);
			modal.abrir(partes.cancelarBtn);
		});
	}

	/**
	 * Validacao padronizada de campos obrigatorios de um formulario: marca
	 * borda vermelha + mensagem curta (nunca so a cor, sempre com icone e
	 * texto), foca o primeiro campo invalido e remove o estado de erro assim
	 * que o campo for corrigido. Reaproveitavel por qualquer formulario do
	 * sistema (nao especifico do modulo financeiro), chamado explicitamente
	 * por cada handler de submit logo apos o preventDefault.
	 */
	function campoValido(campo) {
		if (campo.disabled || campo.type === "hidden") {
			return true;
		}
		if (campo.type === "checkbox" || campo.type === "radio") {
			return campo.checked;
		}
		if (!String(campo.value || "").trim()) {
			return false;
		}
		return typeof campo.checkValidity !== "function" || campo.checkValidity();
	}

	function mensagemErroCampo(campo) {
		if (campo.type === "checkbox" || campo.type === "radio") {
			return "Confirme esta opção.";
		}
		if (campo.tagName === "SELECT") {
			return "Selecione uma opção.";
		}
		return "Campo obrigatório.";
	}

	function limparErroCampo(campo) {
		campo.classList.remove("criati-campo-invalido");
		campo.removeAttribute("aria-invalid");
		var container = campo.closest(".criati-field") || campo.parentElement;
		var erro = container ? container.querySelector(".criati-campo-erro-mensagem") : null;
		if (erro) {
			var descritos = (campo.getAttribute("aria-describedby") || "").split(" ").filter(function (id) {
				return id && id !== erro.id;
			});
			if (descritos.length) {
				campo.setAttribute("aria-describedby", descritos.join(" "));
			} else {
				campo.removeAttribute("aria-describedby");
			}
			erro.remove();
		}
	}

	function marcarErroCampo(campo, mensagem) {
		campo.classList.add("criati-campo-invalido");
		campo.setAttribute("aria-invalid", "true");
		var container = campo.closest(".criati-field") || campo.parentElement;
		if (!container) {
			return;
		}
		var erro = container.querySelector(".criati-campo-erro-mensagem");
		if (!erro) {
			erro = document.createElement("p");
			erro.className = "criati-campo-erro-mensagem";
			erro.setAttribute("role", "alert");
			erro.id = (campo.id || "campo") + "-erro";
			container.appendChild(erro);
		}
		erro.textContent = mensagem;
		var descritos = (campo.getAttribute("aria-describedby") || "").split(" ").filter(Boolean);
		if (descritos.indexOf(erro.id) === -1) {
			descritos.push(erro.id);
			campo.setAttribute("aria-describedby", descritos.join(" "));
		}
	}

	function validarObrigatorios(form) {
		if (!form) {
			return true;
		}
		var campos = Array.prototype.slice.call(form.querySelectorAll("[required]"));
		var primeiroInvalido = null;
		campos.forEach(function (campo) {
			if (campo.dataset.validacaoLigada !== "true") {
				campo.dataset.validacaoLigada = "true";
				var aoCorrigir = function () {
					if (campoValido(campo)) {
						limparErroCampo(campo);
					}
				};
				campo.addEventListener("input", aoCorrigir);
				campo.addEventListener("change", aoCorrigir);
			}
			if (campoValido(campo)) {
				limparErroCampo(campo);
			} else {
				marcarErroCampo(campo, mensagemErroCampo(campo));
				if (!primeiroInvalido) {
					primeiroInvalido = campo;
				}
			}
		});
		if (primeiroInvalido) {
			primeiroInvalido.focus();
			return false;
		}
		return true;
	}

	window.CriatiUI = {
		showToast: showToast,
		setButtonLoading: setButtonLoading,
		initTogglePassword: initTogglePassword,
		initSidebarToggle: initSidebarToggle,
		initFiltrosRecolhiveis: initFiltrosRecolhiveis,
		initUserMenu: initUserMenu,
		criarModal: criarModal,
		confirmarAcao: confirmarAcao,
		validarObrigatorios: validarObrigatorios
	};
})(window, document);
