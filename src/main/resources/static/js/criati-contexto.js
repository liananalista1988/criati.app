/* Criati - carrega usuario e empresa ativa a partir da API e monta topbar/dashboard.
   Nunca decide autorizacao nem guarda dados sensiveis: apenas exibe o que o backend retorna. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };

	function iniciais(nome) {
		if (!nome) {
			return "?";
		}
		var partes = nome.trim().split(/\s+/);
		var primeira = partes[0] ? partes[0][0] : "";
		var ultima = partes.length > 1 ? partes[partes.length - 1][0] : "";
		return (primeira + ultima).toUpperCase();
	}

	function el(id) {
		return document.getElementById(id);
	}

	function mostrar(id, mostrar) {
		var elemento = el(id);
		if (elemento) {
			elemento.hidden = !mostrar;
		}
	}

	function renderUsuario(usuario) {
		var nomeEl = el("criati-user-name");
		var avatarEl = el("criati-user-avatar");
		if (nomeEl) {
			nomeEl.textContent = usuario.nome;
		}
		if (avatarEl) {
			avatarEl.textContent = iniciais(usuario.nome);
		}
	}

	function renderTopbarEmpresa(empresas, empresaAtivaId, aoTrocar) {
		var container = el("criati-topbar-empresa");
		if (!container) {
			return;
		}
		container.innerHTML = "";

		if (empresas.length === 0) {
			return;
		}

		if (empresas.length === 1) {
			var texto = document.createElement("span");
			texto.textContent = empresas[0].nomeEmpresa;
			container.appendChild(texto);
			return;
		}

		var select = document.createElement("select");
		select.className = "criati-select";
		select.id = "criati-empresa-select";
		select.setAttribute("aria-label", "Empresa ativa");

		empresas.forEach(function (empresa) {
			var option = document.createElement("option");
			option.value = empresa.empresaId;
			option.textContent = empresa.nomeEmpresa;
			if (empresa.empresaId === empresaAtivaId) {
				option.selected = true;
			}
			select.appendChild(option);
		});

		select.addEventListener("change", function () {
			aoTrocar(select.value);
		});

		container.appendChild(select);
	}

	function renderCards(usuario, empresas, contexto) {
		var empresaVinculo = empresas.find(function (empresa) {
			return empresa.empresaId === contexto.empresaId;
		});

		el("criati-card-empresa").textContent = empresaVinculo ? empresaVinculo.nomeEmpresa : "-";
		el("criati-card-empresa").classList.remove("criati-skeleton");
		el("criati-card-empresa-sub").textContent = empresaVinculo
			? "Vinculo " + (STATUS_LABEL[empresaVinculo.status] || empresaVinculo.status).toLowerCase()
			: "";

		el("criati-card-perfil").textContent = PERFIL_LABEL[contexto.perfil] || contexto.perfil;
		el("criati-card-perfil").classList.remove("criati-skeleton");

		el("criati-card-status").textContent = STATUS_LABEL[usuario.status] || usuario.status;
		el("criati-card-status").classList.remove("criati-skeleton");

		el("criati-card-empresas").textContent = String(empresas.length);
		el("criati-card-empresas").classList.remove("criati-skeleton");

		var saudacao = el("criati-saudacao");
		if (saudacao) {
			saudacao.textContent = "Ola, " + usuario.nome.split(" ")[0] + "!";
		}
		var descricao = el("criati-empresa-descricao");
		if (descricao && empresaVinculo) {
			descricao.textContent = "Voce esta em " + empresaVinculo.nomeEmpresa + ".";
		}
	}

	function renderSelecaoEmpresa(empresas, aoConfirmar) {
		var container = el("criati-selecao-empresa");
		if (!container) {
			return;
		}
		container.innerHTML = "";

		var texto = document.createElement("p");
		texto.textContent = "Selecione a empresa que deseja acessar:";
		container.appendChild(texto);

		var linha = document.createElement("div");
		linha.style.display = "flex";
		linha.style.gap = "10px";
		linha.style.marginTop = "8px";

		var select = document.createElement("select");
		select.className = "criati-select";
		select.setAttribute("aria-label", "Escolha a empresa");
		empresas.forEach(function (empresa) {
			var option = document.createElement("option");
			option.value = empresa.empresaId;
			option.textContent = empresa.nomeEmpresa;
			select.appendChild(option);
		});

		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-primary";
		botao.textContent = "Confirmar";
		botao.addEventListener("click", function () {
			window.CriatiUI.setButtonLoading(botao, true, "Selecionando...");
			aoConfirmar(select.value).finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
		});

		linha.appendChild(select);
		linha.appendChild(botao);
		container.appendChild(linha);
		mostrar("criati-selecao-empresa", true);
	}

	function estadoCarregando() {
		mostrar("criati-dashboard-erro", false);
		mostrar("criati-dashboard-vazio", false);
		mostrar("criati-selecao-empresa", false);
		mostrar("criati-dashboard-conteudo", true);
	}

	function estadoErro() {
		mostrar("criati-dashboard-conteudo", false);
		mostrar("criati-dashboard-vazio", false);
		mostrar("criati-selecao-empresa", false);
		mostrar("criati-dashboard-erro", true);
	}

	function estadoVazio() {
		mostrar("criati-dashboard-conteudo", false);
		mostrar("criati-dashboard-erro", false);
		mostrar("criati-selecao-empresa", false);
		mostrar("criati-dashboard-vazio", true);
	}

	function estadoSelecao() {
		mostrar("criati-dashboard-conteudo", false);
		mostrar("criati-dashboard-erro", false);
		mostrar("criati-dashboard-vazio", false);
	}

	function estadoConteudo() {
		mostrar("criati-dashboard-erro", false);
		mostrar("criati-dashboard-vazio", false);
		mostrar("criati-selecao-empresa", false);
		mostrar("criati-dashboard-conteudo", true);
	}

	function iniciar() {
		estadoCarregando();

		Promise.all([
			window.CriatiApi.get("/api/auth/me"),
			window.CriatiApi.get("/api/contexto/empresas"),
			window.CriatiApi.get("/api/contexto/empresa-ativa")
		])
			.then(function (respostas) {
				var usuario = respostas[0].data;
				var empresas = respostas[1].data || [];
				var contexto = respostas[2].data;

				renderUsuario(usuario);

				if (empresas.length === 0) {
					estadoVazio();
					return;
				}

				function selecionar(empresaId) {
					return window.CriatiApi
						.post("/api/contexto/empresa-ativa", { empresaId: empresaId })
						.then(function () {
							return iniciar();
						})
						.catch(function () {
							window.CriatiUI.showToast("erro", "Nao foi possivel selecionar essa empresa agora.");
						});
				}

				if (!contexto) {
					if (empresas.length === 1) {
						selecionar(empresas[0].empresaId);
						return;
					}
					estadoSelecao();
					renderSelecaoEmpresa(empresas, selecionar);
					return;
				}

				renderTopbarEmpresa(empresas, contexto.empresaId, selecionar);
				renderCards(usuario, empresas, contexto);
				estadoConteudo();
			})
			.catch(function () {
				estadoErro();
			});
	}

	window.CriatiContexto = { iniciar: iniciar };
})(window, document);
