/* Criati - tela de gestao de convites da empresa ativa (/app/convites).
   Consome exclusivamente /api/contexto/convites/**; nunca envia empresaId, e
   o token bruto (quando o backend o retorna, so em local/test) nunca e
   gravado em localStorage/sessionStorage nem no console - fica apenas numa
   variavel local, descartada ao fechar o modal de sucesso. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { PENDENTE: "Pendente", UTILIZADO: "Utilizado", EXPIRADO: "Expirado", REVOGADO: "Revogado" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };

	var conviteEmMemoria = [];
	var tokenAtual = null;

	function el(id) {
		return document.getElementById(id);
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

	function debounce(fn, atraso) {
		var timer;
		return function () {
			window.clearTimeout(timer);
			timer = window.setTimeout(fn, atraso);
		};
	}

	var modalCriar;
	var modalSucesso;
	var modalConfirmar;

	function iniciar() {
		var tbody = el("criati-convites-tbody");
		if (!tbody) {
			return;
		}

		modalCriar = window.CriatiUI.criarModal(el("criati-convite-modal"));
		modalSucesso = window.CriatiUI.criarModal(el("criati-convite-sucesso-modal"));
		modalConfirmar = window.CriatiUI.criarModal(el("criati-confirmar-modal"));

		el("criati-convites-novo").addEventListener("click", abrirModalCriar);
		el("criati-convite-cancelar").addEventListener("click", modalCriar.fechar);
		el("criati-convite-form").addEventListener("submit", salvarConvite);
		el("criati-convite-sucesso-fechar").addEventListener("click", fecharModalSucesso);
		el("criati-convite-sucesso-copiar").addEventListener("click", copiarLink);

		el("criati-convites-busca").addEventListener("input", debounce(aplicarFiltros, 350));
		el("criati-convites-filtro-perfil").addEventListener("change", aplicarFiltros);
		el("criati-convites-filtro-status").addEventListener("change", aplicarFiltros);

		carregar();
	}

	function carregar() {
		var carregando = el("criati-convites-carregando");
		var erro = el("criati-convites-erro");

		carregando.hidden = false;
		erro.hidden = true;

		window.CriatiApi
			.get("/api/contexto/convites")
			.then(function (resposta) {
				carregando.hidden = true;
				conviteEmMemoria = resposta.data || [];
				aplicarFiltros();
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	// Filtros aplicados no cliente: o backend (GET /api/contexto/convites) nao
	// aceita parametros de busca/status/perfil, apenas retorna todos os
	// convites da empresa ativa. Filtrar aqui e so uma conveniencia de
	// exibicao, sem nenhuma regra de autorizacao nova.
	function aplicarFiltros() {
		var buscaNormalizada = el("criati-convites-busca").value.trim().toLowerCase();
		var perfilFiltro = el("criati-convites-filtro-perfil").value;
		var statusFiltro = el("criati-convites-filtro-status").value;

		var filtrados = conviteEmMemoria.filter(function (convite) {
			if (buscaNormalizada && convite.email.toLowerCase().indexOf(buscaNormalizada) === -1) {
				return false;
			}
			if (perfilFiltro && convite.perfil !== perfilFiltro) {
				return false;
			}
			if (statusFiltro && convite.status !== statusFiltro) {
				return false;
			}
			return true;
		});

		renderLista(filtrados);
	}

	function renderLista(convites) {
		var vazio = el("criati-convites-vazio");
		var tabelaWrap = el("criati-convites-tabela-wrap");
		var cards = el("criati-convites-cards");

		cards.innerHTML = "";
		if (convites.length === 0) {
			vazio.hidden = false;
			tabelaWrap.hidden = true;
			return;
		}
		vazio.hidden = true;
		tabelaWrap.hidden = false;

		var tbody = el("criati-convites-tbody");
		tbody.innerHTML = "";
		convites.forEach(function (convite) {
			tbody.appendChild(criarLinha(convite));
			cards.appendChild(criarCard(convite));
		});
	}

	function criarLinha(convite) {
		var linha = document.createElement("tr");
		linha.appendChild(criarCelula(convite.email));

		var celulaPerfil = document.createElement("td");
		var badgePerfil = document.createElement("span");
		badgePerfil.className = "criati-badge criati-badge-perfil";
		badgePerfil.textContent = PERFIL_LABEL[convite.perfil] || convite.perfil;
		celulaPerfil.appendChild(badgePerfil);
		linha.appendChild(celulaPerfil);

		var celulaStatus = document.createElement("td");
		celulaStatus.appendChild(montarBadgeStatus(convite.status));
		linha.appendChild(celulaStatus);

		linha.appendChild(criarCelula(formatarDataHora(convite.criadoEm)));
		linha.appendChild(criarCelula(formatarDataHora(convite.expiraEm)));
		linha.appendChild(criarCelula(formatarDataHora(convite.utilizadoEm)));

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		celulaAcoes.appendChild(montarAcoes(convite));
		linha.appendChild(celulaAcoes);

		return linha;
	}

	function criarCard(convite) {
		var card = document.createElement("article");
		card.className = "criati-acesso-card";

		var titulo = document.createElement("div");
		titulo.className = "criati-acesso-card-titulo";
		titulo.textContent = convite.email;
		card.appendChild(titulo);

		card.appendChild(montarLinhaCard("Perfil", PERFIL_LABEL[convite.perfil] || convite.perfil));

		var linhaStatus = document.createElement("div");
		linhaStatus.className = "criati-acesso-card-linha";
		var rotulo = document.createElement("span");
		rotulo.textContent = "Status";
		var valor = document.createElement("span");
		valor.appendChild(montarBadgeStatus(convite.status));
		linhaStatus.appendChild(rotulo);
		linhaStatus.appendChild(valor);
		card.appendChild(linhaStatus);

		card.appendChild(montarLinhaCard("Criado em", formatarDataHora(convite.criadoEm)));
		card.appendChild(montarLinhaCard("Expira em", formatarDataHora(convite.expiraEm)));
		card.appendChild(montarLinhaCard("Utilizado em", formatarDataHora(convite.utilizadoEm)));

		var acoes = document.createElement("div");
		acoes.className = "criati-acesso-card-acoes";
		acoes.appendChild(montarAcoes(convite));
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

	function montarAcoes(convite) {
		var container = document.createElement("div");
		container.className = "criati-table-acoes";

		var botaoRevogar = document.createElement("button");
		botaoRevogar.type = "button";
		botaoRevogar.className = "criati-btn criati-btn-ghost";
		botaoRevogar.textContent = "Revogar";
		if (convite.status === "PENDENTE") {
			botaoRevogar.addEventListener("click", function () {
				confirmarRevogar(convite);
			});
		} else {
			botaoRevogar.disabled = true;
			botaoRevogar.title = "Somente convites pendentes podem ser revogados.";
		}
		container.appendChild(botaoRevogar);
		return container;
	}

	function abrirModalCriar() {
		el("criati-convite-form").reset();
		el("criati-convite-perfil").value = "USUARIO";
		modalCriar.abrir(el("criati-convite-email"));
	}

	function salvarConvite(evento) {
		evento.preventDefault();
		var botao = el("criati-convite-salvar");
		var dados = {
			email: el("criati-convite-email").value,
			perfil: el("criati-convite-perfil").value
		};

		window.CriatiUI.setButtonLoading(botao, true, "Enviando...");
		window.CriatiApi
			.post("/api/contexto/convites", dados)
			.then(function (resposta) {
				modalCriar.fechar();
				abrirModalSucesso(resposta.data);
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel criar o convite agora.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	function abrirModalSucesso(criado) {
		var linkContainer = el("criati-convite-sucesso-link");
		var aviso = el("criati-convite-sucesso-aviso");
		var mensagem = el("criati-convite-sucesso-mensagem");
		var linkInput = el("criati-convite-sucesso-link-input");

		if (criado.tokenBruto) {
			tokenAtual = criado.tokenBruto;
			mensagem.textContent = "Convite criado para " + criado.convite.email + ".";
			linkInput.value = window.location.origin + "/convites/" + tokenAtual;
			linkContainer.hidden = false;
			aviso.hidden = false;
		} else {
			tokenAtual = null;
			mensagem.textContent = "Convite criado. A entrega por e-mail sera integrada em etapa futura.";
			linkInput.value = "";
			linkContainer.hidden = true;
			aviso.hidden = true;
		}

		modalSucesso.abrir(el("criati-convite-sucesso-fechar"));
	}

	function fecharModalSucesso() {
		tokenAtual = null;
		el("criati-convite-sucesso-link-input").value = "";
		modalSucesso.fechar();
	}

	function copiarLink() {
		var linkInput = el("criati-convite-sucesso-link-input");
		if (!tokenAtual || !navigator.clipboard) {
			window.CriatiUI.showToast("erro", "Nao foi possivel copiar automaticamente; selecione o link manualmente.");
			return;
		}
		navigator.clipboard
			.writeText(linkInput.value)
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Link copiado.");
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel copiar automaticamente; selecione o link manualmente.");
			});
	}

	function confirmarRevogar(convite) {
		window.CriatiUI
			.confirmarAcao(
				el("criati-confirmar-modal"),
				{
					tituloEl: el("criati-confirmar-titulo"),
					mensagemEl: el("criati-confirmar-mensagem"),
					confirmarBtn: el("criati-confirmar-confirmar"),
					cancelarBtn: el("criati-confirmar-cancelar")
				},
				{
					titulo: "Revogar convite",
					mensagem: "Este convite sera revogado e nao podera mais ser utilizado para entrar na empresa.",
					rotuloConfirmar: "Revogar"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.delete("/api/contexto/convites/" + convite.id)
					.then(function () {
						window.CriatiUI.showToast("sucesso", "Convite revogado.");
						carregar();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel revogar agora.");
					});
			});
	}

	window.CriatiConvites = { iniciar: iniciar };
})(window, document);
