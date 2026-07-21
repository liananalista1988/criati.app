/* Criati - vinculos globais do painel administrativo (/app/admin/vinculos).
   Consome /api/admin/vinculos/** e /api/admin/empresas (para popular os
   seletores de empresa) e /api/admin/usuarios (busca de usuario existente).
   Reaplica as mesmas regras ja usadas pela gestao empresarial comum (ultimo
   Administrador ativo, duplicidade, transicoes de status) - o backend e quem
   decide, este JS so reage ao resultado. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };

	var vinculosEmMemoria = [];
	var usuarioSelecionadoParaVinculo = null;
	var vinculoEmEdicaoPerfilId = null;

	var modalVinculo;
	var modalPerfil;
	var modalConfirmar;

	function el(id) {
		return document.getElementById(id);
	}

	function formatarData(iso) {
		if (!iso) {
			return "-";
		}
		var data = new Date(iso);
		return isNaN(data.getTime()) ? "-" : data.toLocaleDateString("pt-BR");
	}

	function debounce(fn, atraso) {
		var timer;
		return function () {
			window.clearTimeout(timer);
			timer = window.setTimeout(fn, atraso);
		};
	}

	function parametroDaUrl(nome) {
		var params = new URLSearchParams(window.location.search);
		return params.get(nome);
	}

	function iniciar() {
		var tbody = el("criati-admin-vinculos-tbody");
		if (!tbody) {
			return;
		}

		modalVinculo = window.CriatiUI.criarModal(el("criati-vinculo-modal"));
		modalPerfil = window.CriatiUI.criarModal(el("criati-vinculo-perfil-modal"));
		modalConfirmar = window.CriatiUI.criarModal(el("criati-confirmar-modal"));

		el("criati-admin-vinculo-novo").addEventListener("click", abrirModalNovoVinculo);
		el("criati-vinculo-cancelar").addEventListener("click", modalVinculo.fechar);
		el("criati-vinculo-form").addEventListener("submit", salvarNovoVinculo);
		el("criati-vinculo-usuario-busca").addEventListener("input", debounce(buscarUsuariosParaVinculo, 350));

		el("criati-vinculo-perfil-cancelar").addEventListener("click", modalPerfil.fechar);
		el("criati-vinculo-perfil-form").addEventListener("submit", salvarPerfil);

		el("criati-admin-vinculos-busca").addEventListener("input", debounce(aplicarFiltroBusca, 300));
		el("criati-admin-vinculos-filtro-empresa").addEventListener("change", carregar);
		el("criati-admin-vinculos-filtro-perfil").addEventListener("change", carregar);
		el("criati-admin-vinculos-filtro-status").addEventListener("change", carregar);

		carregarEmpresasParaSelects();
	}

	function carregarEmpresasParaSelects() {
		window.CriatiApi
			.get("/api/admin/empresas")
			.then(function (resposta) {
				var empresas = resposta.data || [];
				popularSelectEmpresas(el("criati-admin-vinculos-filtro-empresa"), empresas, true);
				popularSelectEmpresas(el("criati-vinculo-empresa"), empresas, false);

				var empresaIdUrl = parametroDaUrl("empresaId");
				if (empresaIdUrl) {
					el("criati-admin-vinculos-filtro-empresa").value = empresaIdUrl;
				}
				carregar();
			})
			.catch(function () {
				carregar();
			});
	}

	function popularSelectEmpresas(select, empresas, comOpcaoTodas) {
		var valorAtual = select.value;
		select.innerHTML = "";
		if (comOpcaoTodas) {
			var optTodas = document.createElement("option");
			optTodas.value = "";
			optTodas.textContent = "Todas";
			select.appendChild(optTodas);
		} else {
			var optSelecione = document.createElement("option");
			optSelecione.value = "";
			optSelecione.textContent = "Selecione...";
			select.appendChild(optSelecione);
		}
		empresas.forEach(function (empresa) {
			var opt = document.createElement("option");
			opt.value = empresa.id;
			opt.textContent = empresa.nome;
			select.appendChild(opt);
		});
		if (valorAtual) {
			select.value = valorAtual;
		}
	}

	function montarQuery() {
		var empresaId = el("criati-admin-vinculos-filtro-empresa").value;
		var perfil = el("criati-admin-vinculos-filtro-perfil").value;
		var status = el("criati-admin-vinculos-filtro-status").value;
		var partes = [];
		if (empresaId) {
			partes.push("empresaId=" + encodeURIComponent(empresaId));
		}
		if (perfil) {
			partes.push("perfil=" + encodeURIComponent(perfil));
		}
		if (status) {
			partes.push("status=" + encodeURIComponent(status));
		}
		return partes.length ? "?" + partes.join("&") : "";
	}

	function carregar() {
		var carregando = el("criati-admin-vinculos-carregando");
		var erro = el("criati-admin-vinculos-erro");

		carregando.hidden = false;
		erro.hidden = true;

		window.CriatiApi
			.get("/api/admin/vinculos" + montarQuery())
			.then(function (resposta) {
				carregando.hidden = true;
				vinculosEmMemoria = resposta.data || [];
				aplicarFiltroBusca();
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function aplicarFiltroBusca() {
		var buscaNormalizada = el("criati-admin-vinculos-busca").value.trim().toLowerCase();
		var filtrados = vinculosEmMemoria.filter(function (vinculo) {
			if (!buscaNormalizada) {
				return true;
			}
			return vinculo.usuarioNome.toLowerCase().indexOf(buscaNormalizada) !== -1
				|| vinculo.usuarioEmail.toLowerCase().indexOf(buscaNormalizada) !== -1
				|| vinculo.empresaNome.toLowerCase().indexOf(buscaNormalizada) !== -1;
		});
		renderLista(filtrados);
	}

	function renderLista(vinculos) {
		var vazio = el("criati-admin-vinculos-vazio");
		var tabelaWrap = el("criati-admin-vinculos-tabela-wrap");
		var cards = el("criati-admin-vinculos-cards");
		var contador = el("criati-admin-vinculos-contador");

		contador.textContent = vinculos.length === 1 ? "1 vinculo" : vinculos.length + " vinculos";
		cards.innerHTML = "";

		if (vinculos.length === 0) {
			vazio.hidden = false;
			tabelaWrap.hidden = true;
			return;
		}
		vazio.hidden = true;
		tabelaWrap.hidden = false;

		var tbody = el("criati-admin-vinculos-tbody");
		tbody.innerHTML = "";
		vinculos.forEach(function (vinculo) {
			tbody.appendChild(criarLinha(vinculo));
			cards.appendChild(criarCard(vinculo));
		});
	}

	function criarLinha(vinculo) {
		var linha = document.createElement("tr");
		linha.appendChild(criarCelula(vinculo.usuarioNome));
		linha.appendChild(criarCelula(vinculo.usuarioEmail));
		linha.appendChild(criarCelula(vinculo.empresaNome));

		var celulaPerfil = document.createElement("td");
		var badgePerfil = document.createElement("span");
		badgePerfil.className = "criati-badge criati-badge-perfil";
		badgePerfil.textContent = PERFIL_LABEL[vinculo.perfil] || vinculo.perfil;
		celulaPerfil.appendChild(badgePerfil);
		linha.appendChild(celulaPerfil);

		var celulaStatus = document.createElement("td");
		celulaStatus.appendChild(montarBadgeStatus(vinculo.status));
		linha.appendChild(celulaStatus);

		linha.appendChild(criarCelula(formatarData(vinculo.criadoEm)));

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		celulaAcoes.appendChild(montarAcoes(vinculo));
		linha.appendChild(celulaAcoes);

		return linha;
	}

	function criarCard(vinculo) {
		var card = document.createElement("article");
		card.className = "criati-acesso-card";

		var titulo = document.createElement("div");
		titulo.className = "criati-acesso-card-titulo";
		titulo.textContent = vinculo.usuarioNome;
		card.appendChild(titulo);

		card.appendChild(montarLinhaCard("E-mail", vinculo.usuarioEmail));
		card.appendChild(montarLinhaCard("Empresa", vinculo.empresaNome));
		card.appendChild(montarLinhaCard("Perfil", PERFIL_LABEL[vinculo.perfil] || vinculo.perfil));

		var linhaStatus = document.createElement("div");
		linhaStatus.className = "criati-acesso-card-linha";
		var rotulo = document.createElement("span");
		rotulo.textContent = "Status";
		var valor = document.createElement("span");
		valor.appendChild(montarBadgeStatus(vinculo.status));
		linhaStatus.appendChild(rotulo);
		linhaStatus.appendChild(valor);
		card.appendChild(linhaStatus);

		var acoes = document.createElement("div");
		acoes.className = "criati-acesso-card-acoes";
		acoes.appendChild(montarAcoes(vinculo));
		card.appendChild(acoes);

		return card;
	}

	function montarLinhaCard(rotulo, valor) {
		var linha = document.createElement("div");
		linha.className = "criati-acesso-card-linha";
		var spanRotulo = document.createElement("span");
		spanRotulo.textContent = rotulo;
		var spanValor = document.createElement("span");
		spanValor.textContent = valor;
		linha.appendChild(spanRotulo);
		linha.appendChild(spanValor);
		return linha;
	}

	function montarBadgeStatus(status) {
		var badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + status.toLowerCase();
		badge.textContent = STATUS_LABEL[status] || status;
		return badge;
	}

	function criarCelula(texto) {
		var celula = document.createElement("td");
		celula.textContent = texto;
		return celula;
	}

	function montarAcoes(vinculo) {
		var container = document.createElement("div");
		container.className = "criati-table-acoes";

		container.appendChild(criarBotao("Perfil", function () {
			abrirModalPerfil(vinculo);
		}));

		if (vinculo.status === "ATIVO") {
			container.appendChild(criarBotao("Suspender", function () {
				confirmarAcaoVinculo(vinculo, "suspender");
			}));
			container.appendChild(criarBotao("Remover", function () {
				confirmarAcaoVinculo(vinculo, "remover");
			}));
		} else {
			container.appendChild(criarBotao("Reativar", function () {
				confirmarAcaoVinculo(vinculo, "reativar");
			}));
		}

		return container;
	}

	function criarBotao(texto, aoClicar) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-ghost";
		botao.textContent = texto;
		botao.addEventListener("click", aoClicar);
		return botao;
	}

	function abrirModalPerfil(vinculo) {
		vinculoEmEdicaoPerfilId = vinculo.usuarioEmpresaId;
		el("criati-vinculo-perfil-select").value = vinculo.perfil;
		modalPerfil.abrir(el("criati-vinculo-perfil-select"));
	}

	function salvarPerfil(evento) {
		evento.preventDefault();
		var botao = el("criati-vinculo-perfil-salvar");
		var novoPerfil = el("criati-vinculo-perfil-select").value;

		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		window.CriatiApi
			.request("/api/admin/vinculos/" + vinculoEmEdicaoPerfilId + "/perfil", {
				method: "PATCH",
				body: { perfil: novoPerfil }
			})
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Perfil atualizado.");
				modalPerfil.fechar();
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel alterar o perfil agora.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	function confirmarAcaoVinculo(vinculo, acao) {
		var textos = {
			suspender: {
				titulo: "Suspender vinculo",
				mensagem: "Este vinculo sera suspenso. O usuario e a empresa continuam existindo normalmente.",
				rotulo: "Suspender"
			},
			reativar: {
				titulo: "Reativar vinculo",
				mensagem: "Este vinculo voltara a ficar ativo.",
				rotulo: "Reativar"
			},
			remover: {
				titulo: "Remover vinculo",
				mensagem: "Este vinculo sera removido (logicamente). O usuario global e a empresa nao serao excluidos.",
				rotulo: "Remover"
			}
		};
		var textoAcao = textos[acao];

		window.CriatiUI
			.confirmarAcao(
				el("criati-confirmar-modal"),
				{
					tituloEl: el("criati-confirmar-titulo"),
					mensagemEl: el("criati-confirmar-mensagem"),
					confirmarBtn: el("criati-confirmar-confirmar"),
					cancelarBtn: el("criati-confirmar-cancelar")
				},
				{ titulo: textoAcao.titulo, mensagem: textoAcao.mensagem, rotuloConfirmar: textoAcao.rotulo })
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				var chamada = acao === "remover"
					? window.CriatiApi.delete("/api/admin/vinculos/" + vinculo.usuarioEmpresaId)
					: window.CriatiApi.post("/api/admin/vinculos/" + vinculo.usuarioEmpresaId + "/" + acao);

				return chamada
					.then(function () {
						window.CriatiUI.showToast("sucesso", "Vinculo atualizado.");
						carregar();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel concluir a acao agora.");
					});
			});
	}

	// --- Novo vinculo ---

	function abrirModalNovoVinculo() {
		el("criati-vinculo-form").reset();
		usuarioSelecionadoParaVinculo = null;
		el("criati-vinculo-usuario-selecionado").hidden = true;
		el("criati-vinculo-usuario-resultados").innerHTML = "";
		var empresaFiltroAtual = el("criati-admin-vinculos-filtro-empresa").value;
		if (empresaFiltroAtual) {
			el("criati-vinculo-empresa").value = empresaFiltroAtual;
		}
		modalVinculo.abrir(el("criati-vinculo-empresa"));
	}

	function buscarUsuariosParaVinculo() {
		var termo = el("criati-vinculo-usuario-busca").value.trim();
		var resultados = el("criati-vinculo-usuario-resultados");
		if (!termo) {
			resultados.innerHTML = "";
			return;
		}
		window.CriatiApi
			.get("/api/admin/usuarios?busca=" + encodeURIComponent(termo) + "&status=ATIVO")
			.then(function (resposta) {
				renderResultadosUsuario(resposta.data || []);
			})
			.catch(function () {
				resultados.innerHTML = "";
			});
	}

	function renderResultadosUsuario(usuarios) {
		var resultados = el("criati-vinculo-usuario-resultados");
		resultados.innerHTML = "";
		usuarios.forEach(function (usuario) {
			var card = document.createElement("article");
			card.className = "criati-acesso-card";

			var titulo = document.createElement("div");
			titulo.className = "criati-acesso-card-titulo";
			titulo.textContent = usuario.nome;
			card.appendChild(titulo);
			card.appendChild(montarLinhaCard("E-mail", usuario.email));

			var botao = document.createElement("button");
			botao.type = "button";
			botao.className = "criati-btn criati-btn-ghost";
			botao.textContent = "Selecionar";
			botao.addEventListener("click", function () {
				usuarioSelecionadoParaVinculo = usuario;
				var texto = el("criati-vinculo-usuario-selecionado");
				texto.hidden = false;
				texto.textContent = "Selecionado: " + usuario.nome + " (" + usuario.email + ")";
				resultados.innerHTML = "";
				el("criati-vinculo-usuario-busca").value = "";
			});
			card.appendChild(botao);

			resultados.appendChild(card);
		});
	}

	function salvarNovoVinculo(evento) {
		evento.preventDefault();
		var botao = el("criati-vinculo-salvar");
		var empresaId = el("criati-vinculo-empresa").value;
		var perfil = el("criati-vinculo-perfil").value;

		if (!empresaId) {
			window.CriatiUI.showToast("erro", "Selecione a empresa.");
			return;
		}
		if (!usuarioSelecionadoParaVinculo) {
			window.CriatiUI.showToast("erro", "Selecione o usuario.");
			return;
		}

		window.CriatiUI.setButtonLoading(botao, true, "Criando...");
		window.CriatiApi
			.post("/api/admin/vinculos", {
				usuarioId: usuarioSelecionadoParaVinculo.id,
				empresaId: empresaId,
				perfil: perfil
			})
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Vinculo criado.");
				modalVinculo.fechar();
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel criar o vinculo agora.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	window.CriatiAdminVinculos = { iniciar: iniciar };
})(window, document);
