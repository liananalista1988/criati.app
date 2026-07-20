/* Criati - autenticacao no navegador: login e logout. Nunca decide autorizacao, apenas consome a API. */
(function (window, document) {
	"use strict";

	function initLoginForm() {
		var form = document.getElementById("criati-login-form");
		if (!form) {
			return;
		}

		var emailInput = document.getElementById("email");
		var senhaInput = document.getElementById("senha");
		var submitButton = document.getElementById("criati-login-submit");
		var erroBox = document.getElementById("criati-login-erro");
		var statusBox = document.getElementById("criati-login-status");
		var enviando = false;

		var params = new URLSearchParams(window.location.search);
		if (params.get("motivo") === "sessao" && statusBox) {
			statusBox.textContent = "Sua sessao expirou. Faca login novamente.";
			statusBox.hidden = false;
		}

		function mostrarErro(mensagem) {
			if (!erroBox) {
				return;
			}
			erroBox.textContent = mensagem;
			erroBox.hidden = false;
		}

		function limparErro() {
			if (erroBox) {
				erroBox.hidden = true;
				erroBox.textContent = "";
			}
		}

		form.addEventListener("submit", function (evento) {
			evento.preventDefault();
			if (enviando) {
				return;
			}
			enviando = true;
			limparErro();
			window.CriatiUI.setButtonLoading(submitButton, true, "Entrando...");

			window.CriatiApi
				.post(
					"/api/auth/login",
					{ email: emailInput.value, senha: senhaInput.value },
					{ redirectOn401: false }
				)
				.then(function () {
					window.location.href = "/app/dashboard";
				})
				.catch(function (erro) {
					senhaInput.value = "";
					senhaInput.focus();
					if (erro.network) {
						mostrarErro("Sem conexao com o servidor. Verifique sua internet e tente novamente.");
					} else if (erro.status === 401) {
						mostrarErro("E-mail ou senha invalidos.");
					} else {
						mostrarErro("Nao foi possivel entrar agora. Tente novamente em instantes.");
					}
				})
				.finally(function () {
					enviando = false;
					window.CriatiUI.setButtonLoading(submitButton, false);
				});
		});
	}

	function initLogout() {
		var button = document.querySelector(".criati-logout-btn");
		if (!button) {
			return;
		}
		var emAndamento = false;

		button.addEventListener("click", function () {
			if (emAndamento) {
				return;
			}
			emAndamento = true;
			window.CriatiUI.setButtonLoading(button, true, "Saindo...");

			window.CriatiApi
				.post("/api/auth/logout", undefined, { redirectOn401: false })
				.then(function () {
					window.location.href = "/login";
				})
				.catch(function () {
					window.CriatiUI.showToast("erro", "Nao foi possivel sair agora. Tente novamente.");
				})
				.finally(function () {
					emAndamento = false;
					window.CriatiUI.setButtonLoading(button, false);
				});
		});
	}

	window.CriatiAuth = {
		initLoginForm: initLoginForm,
		initLogout: initLogout
	};

	document.addEventListener("DOMContentLoaded", initLoginForm);
})(window, document);
