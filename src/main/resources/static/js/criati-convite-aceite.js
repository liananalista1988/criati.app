/* Criati - pagina publica de aceite de convite (/convites/{token}).
   Consome exclusivamente GET /api/convites/{token} e POST .../aceitar (API
   publica ja existente); nao duplica cliente HTTP (reaproveita CriatiApi) e
   nunca autentica automaticamente - so exibe o resultado do backend e
   direciona ao login apos sucesso. O token nunca e persistido (nem
   localStorage/sessionStorage), nunca logado, e so e lido da propria URL. */
(function (window, document) {
	"use strict";

	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };
	var MENSAGEM_INDISPONIVEL = "Este convite nao esta mais disponivel.";
	var ESTADOS = [
		"criati-convite-carregando",
		"criati-convite-invalido",
		"criati-convite-erro-rede",
		"criati-convite-valido",
		"criati-convite-sucesso"
	];

	var token = null;
	var enviando = false;

	function el(id) {
		return document.getElementById(id);
	}

	function obterTokenDaUrl() {
		var partes = window.location.pathname.split("/").filter(function (parte) {
			return parte.length > 0;
		});
		return partes.length ? partes[partes.length - 1] : "";
	}

	function mostrarEstado(idParaMostrar, elementoParaFoco) {
		ESTADOS.forEach(function (id) {
			el(id).hidden = id !== idParaMostrar;
		});
		if (elementoParaFoco) {
			window.setTimeout(function () {
				elementoParaFoco.focus();
			}, 0);
		}
	}

	function formatarDataHora(iso) {
		if (!iso) {
			return "-";
		}
		var data = new Date(iso);
		if (isNaN(data.getTime())) {
			return "-";
		}
		return data.toLocaleString("pt-BR", {
			day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit"
		});
	}

	function iniciar() {
		if (!el("criati-convite-carregando")) {
			return;
		}
		token = obterTokenDaUrl();
		if (!token) {
			mostrarInvalido();
			return;
		}

		window.CriatiUI.initTogglePassword(el("criati-convite-toggle-senha"), el("criati-convite-senha"));
		window.CriatiUI.initTogglePassword(el("criati-convite-toggle-confirmacao"), el("criati-convite-confirmacao"));
		el("criati-convite-tentar-novamente").addEventListener("click", validarConvite);
		el("criati-convite-form").addEventListener("submit", aceitar);

		validarConvite();
	}

	function validarConvite() {
		mostrarEstado("criati-convite-carregando");
		window.CriatiApi
			.get("/api/convites/" + encodeURIComponent(token), { redirectOn401: false })
			.then(function (resposta) {
				if (!resposta.data || !resposta.data.valido) {
					mostrarInvalido();
					return;
				}
				mostrarValido(resposta.data);
			})
			.catch(function () {
				mostrarEstado("criati-convite-erro-rede", el("criati-convite-erro-rede").querySelector("h2"));
			});
	}

	function mostrarInvalido(mensagemPersonalizada) {
		el("criati-convite-invalido-mensagem").textContent = mensagemPersonalizada || MENSAGEM_INDISPONIVEL;
		mostrarEstado("criati-convite-invalido", el("criati-convite-invalido-titulo"));
	}

	function mostrarValido(dados) {
		el("criati-convite-info-empresa").textContent = dados.empresa || "-";
		el("criati-convite-info-email").textContent = dados.emailMascarado || "-";

		var linhaPerfil = el("criati-convite-info-perfil-linha");
		if (dados.perfil) {
			el("criati-convite-info-perfil").textContent = PERFIL_LABEL[dados.perfil] || dados.perfil;
			linhaPerfil.hidden = false;
		} else {
			linhaPerfil.hidden = true;
		}

		el("criati-convite-info-expira").textContent = formatarDataHora(dados.expiraEm);
		mostrarEstado("criati-convite-valido", el("criati-convite-nome"));
	}

	function limparCamposSensiveis() {
		el("criati-convite-senha").value = "";
		el("criati-convite-confirmacao").value = "";
	}

	function mostrarErroFormulario(mensagem) {
		var erro = el("criati-convite-form-erro");
		erro.textContent = mensagem;
		erro.hidden = false;
	}

	function esconderErroFormulario() {
		var erro = el("criati-convite-form-erro");
		erro.hidden = true;
		erro.textContent = "";
	}

	function validarFormularioLocalmente(nome, senha, confirmacao) {
		if (!nome) {
			return "Nome e obrigatorio.";
		}
		if (!senha) {
			return "Senha e obrigatoria.";
		}
		if (senha.length < 15) {
			return "Senha deve possuir no minimo 15 caracteres.";
		}
		if (!confirmacao) {
			return "Confirmacao de senha e obrigatoria.";
		}
		if (senha !== confirmacao) {
			return "Confirmacao de senha nao corresponde a senha.";
		}
		return null;
	}

	function aceitar(evento) {
		evento.preventDefault();
		if (enviando) {
			return;
		}

		var nome = el("criati-convite-nome").value.trim();
		var senha = el("criati-convite-senha").value;
		var confirmacao = el("criati-convite-confirmacao").value;

		esconderErroFormulario();
		var erroLocal = validarFormularioLocalmente(nome, senha, confirmacao);
		if (erroLocal) {
			mostrarErroFormulario(erroLocal);
			return;
		}

		enviando = true;
		var botao = el("criati-convite-enviar");
		window.CriatiUI.setButtonLoading(botao, true, "Enviando...");

		window.CriatiApi
			.post(
				"/api/convites/" + encodeURIComponent(token) + "/aceitar",
				{ nome: nome, senha: senha, confirmacaoSenha: confirmacao },
				{ redirectOn401: false }
			)
			.then(function () {
				limparCamposSensiveis();
				mostrarEstado("criati-convite-sucesso", el("criati-convite-sucesso-titulo"));
			})
			.catch(function (erro) {
				limparCamposSensiveis();
				if (erro.status === 404 || erro.status === 410) {
					mostrarInvalido();
					return;
				}
				if (erro.status === 409) {
					mostrarErroFormulario("Nao foi possivel concluir o convite. Entre em contato com o administrador.");
					return;
				}
				if (erro.status === 400) {
					mostrarErroFormulario(erro.message || "Verifique os dados informados.");
					return;
				}
				mostrarErroFormulario("Nao foi possivel processar sua solicitacao agora. Tente novamente.");
			})
			.finally(function () {
				enviando = false;
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	window.CriatiConviteAceite = { iniciar: iniciar };
})(window, document);
