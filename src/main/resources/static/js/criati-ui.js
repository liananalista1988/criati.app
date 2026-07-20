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

	function initSidebarToggle() {
		var app = document.querySelector(".criati-app");
		var toggle = document.querySelector(".criati-menu-toggle");
		var overlay = document.querySelector(".criati-sidebar-overlay");
		if (!app || !toggle) {
			return;
		}

		function fechar() {
			app.classList.remove("is-sidebar-open");
			toggle.setAttribute("aria-expanded", "false");
		}

		toggle.addEventListener("click", function () {
			var aberto = app.classList.toggle("is-sidebar-open");
			toggle.setAttribute("aria-expanded", String(aberto));
		});

		if (overlay) {
			overlay.addEventListener("click", fechar);
		}

		document.addEventListener("keydown", function (evento) {
			if (evento.key === "Escape") {
				fechar();
			}
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

	window.CriatiUI = {
		showToast: showToast,
		setButtonLoading: setButtonLoading,
		initTogglePassword: initTogglePassword,
		initSidebarToggle: initSidebarToggle,
		initUserMenu: initUserMenu
	};
})(window, document);
