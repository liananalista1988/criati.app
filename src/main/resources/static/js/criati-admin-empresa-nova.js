/* Criati - onboarding de nova empresa pelo Superadministrador
   (/app/admin/empresas/nova). Formulario organizado em secoes (dados da
   empresa, aplicacoes, administrador inicial); tudo e enviado numa unica
   chamada transacional ao backend (POST /api/admin/empresas/com-*), que
   garante rollback completo em caso de falha - nada e feito em etapas
   separadas no cliente. Nunca define senha: o modo "usuario existente" so
   referencia um usuario global ja cadastrado (nenhuma senha), e o modo
   "convite" gera um convite cuja senha e definida pelo proprio convidado. */
(function (window, document) {
	"use strict";

	var usuarioSelecionadoId = null;

	function el(id) {
		return document.getElementById(id);
	}

	function debounce(fn, atraso) {
		var timer;
		return function () {
			window.clearTimeout(timer);
			timer = window.setTimeout(fn, atraso);
		};
	}

	var modalSucesso;

	function iniciar() {
		var form = el("criati-onboarding-form");
		if (!form) {
			return;
		}

		modalSucesso = window.CriatiUI.criarModal(el("criati-onboarding-sucesso-modal"));

		var radiosModo = document.getElementsByName("criati-onboarding-modo-admin");
		Array.prototype.forEach.call(radiosModo, function (radio) {
			radio.addEventListener("change", atualizarModoAdministrador);
		});

		el("criati-onboarding-admin-busca").addEventListener("input", debounce(buscarUsuarios, 350));
		form.addEventListener("submit", enviar);

		atualizarModoAdministrador();
	}

	function modoAtual() {
		var radios = document.getElementsByName("criati-onboarding-modo-admin");
		for (var i = 0; i < radios.length; i++) {
			if (radios[i].checked) {
				return radios[i].value;
			}
		}
		return "existente";
	}

	function atualizarModoAdministrador() {
		var existente = modoAtual() === "existente";
		el("criati-onboarding-admin-existente").hidden = !existente;
		el("criati-onboarding-admin-convite").hidden = existente;
	}

	function buscarUsuarios() {
		var termo = el("criati-onboarding-admin-busca").value.trim();
		var resultados = el("criati-onboarding-admin-resultados");
		if (!termo) {
			resultados.innerHTML = "";
			return;
		}

		window.CriatiApi
			.get("/api/admin/usuarios?busca=" + encodeURIComponent(termo) + "&status=ATIVO")
			.then(function (resposta) {
				renderResultados(resposta.data || []);
			})
			.catch(function () {
				resultados.innerHTML = "";
			});
	}

	function renderResultados(usuarios) {
		var resultados = el("criati-onboarding-admin-resultados");
		resultados.innerHTML = "";
		usuarios.forEach(function (usuario) {
			var card = document.createElement("article");
			card.className = "criati-acesso-card";

			var titulo = document.createElement("div");
			titulo.className = "criati-acesso-card-titulo";
			titulo.textContent = usuario.nome;
			card.appendChild(titulo);

			var linhaEmail = document.createElement("div");
			linhaEmail.className = "criati-acesso-card-linha";
			var spanRotulo = document.createElement("span");
			spanRotulo.textContent = "E-mail";
			var spanValor = document.createElement("span");
			spanValor.textContent = usuario.email;
			linhaEmail.appendChild(spanRotulo);
			linhaEmail.appendChild(spanValor);
			card.appendChild(linhaEmail);

			var botao = document.createElement("button");
			botao.type = "button";
			botao.className = "criati-btn criati-btn-ghost";
			botao.textContent = "Selecionar";
			botao.addEventListener("click", function () {
				selecionarUsuario(usuario);
			});
			card.appendChild(botao);

			resultados.appendChild(card);
		});
	}

	function selecionarUsuario(usuario) {
		usuarioSelecionadoId = usuario.id;
		var texto = el("criati-onboarding-admin-selecionado");
		texto.hidden = false;
		texto.textContent = "Selecionado: " + usuario.nome + " (" + usuario.email + ")";
		el("criati-onboarding-admin-resultados").innerHTML = "";
		el("criati-onboarding-admin-busca").value = "";
	}

	function aplicacoesIniciais() {
		var lista = [];
		if (el("criati-onboarding-app-financeiro").checked) {
			lista.push("FINANCEIRO");
		}
		if (el("criati-onboarding-app-clinica").checked) {
			lista.push("CLINICA");
		}
		return lista;
	}

	function enviar(evento) {
		evento.preventDefault();
		var botao = el("criati-onboarding-confirmar");
		var erroContainer = el("criati-onboarding-erro");
		erroContainer.hidden = true;

		var dadosEmpresa = {
			nome: el("criati-onboarding-nome").value,
			nomeFantasia: el("criati-onboarding-nome-fantasia").value || null,
			cnpj: el("criati-onboarding-cnpj").value
		};

		var existente = modoAtual() === "existente";
		if (existente && !usuarioSelecionadoId) {
			el("criati-onboarding-erro-mensagem").textContent = "Selecione um usuario existente para ser o Administrador.";
			erroContainer.hidden = false;
			return;
		}

		var caminho;
		var corpo;
		if (existente) {
			caminho = "/api/admin/empresas/com-administrador-existente";
			corpo = {
				empresa: dadosEmpresa,
				aplicacoesIniciais: aplicacoesIniciais(),
				administradorUsuarioId: usuarioSelecionadoId
			};
		} else {
			caminho = "/api/admin/empresas/com-administrador-convidado";
			corpo = {
				empresa: dadosEmpresa,
				aplicacoesIniciais: aplicacoesIniciais(),
				administrador: {
					nome: el("criati-onboarding-admin-nome").value,
					email: el("criati-onboarding-admin-email").value
				}
			};
		}

		window.CriatiUI.setButtonLoading(botao, true, "Criando...");
		window.CriatiApi
			.post(caminho, corpo)
			.then(function (resposta) {
				abrirSucesso(resposta.data, existente);
			})
			.catch(function (erro) {
				el("criati-onboarding-erro-mensagem").textContent =
					(erro && erro.message) || "Nao foi possivel criar a empresa agora.";
				erroContainer.hidden = false;
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	function abrirSucesso(dados, existente) {
		var mensagem = el("criati-onboarding-sucesso-mensagem");
		var linkContainer = el("criati-onboarding-sucesso-link");
		var aviso = el("criati-onboarding-sucesso-aviso");
		var linkInput = el("criati-onboarding-sucesso-link-input");
		var verEmpresa = el("criati-onboarding-sucesso-ver-empresa");

		verEmpresa.href = "/app/admin/empresas/" + dados.empresa.id;

		if (existente) {
			mensagem.textContent = "Empresa \"" + dados.empresa.nome + "\" criada e vinculada ao administrador selecionado.";
			linkContainer.hidden = true;
			aviso.hidden = true;
		} else if (dados.tokenBruto) {
			mensagem.textContent = "Empresa \"" + dados.empresa.nome + "\" criada. Convite enviado para "
				+ dados.convite.email + ".";
			linkInput.value = window.location.origin + "/convites/" + dados.tokenBruto;
			linkContainer.hidden = false;
			aviso.hidden = false;
		} else {
			mensagem.textContent = "Empresa \"" + dados.empresa.nome + "\" criada. Convite gerado para "
				+ dados.convite.email + " (entrega por e-mail sera integrada em etapa futura).";
			linkContainer.hidden = true;
			aviso.hidden = true;
		}

		modalSucesso.abrir(verEmpresa);

		el("criati-onboarding-sucesso-copiar").onclick = function () {
			if (!navigator.clipboard || !linkInput.value) {
				window.CriatiUI.showToast("erro", "Nao foi possivel copiar automaticamente; selecione o link manualmente.");
				return;
			}
			navigator.clipboard.writeText(linkInput.value).then(function () {
				window.CriatiUI.showToast("sucesso", "Link copiado.");
			});
		};
	}

	window.CriatiAdminEmpresaNova = { iniciar: iniciar };
})(window, document);
