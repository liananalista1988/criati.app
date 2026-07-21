/* Criati - usuarios globais do painel administrativo (/app/admin/usuarios).
   Consome /api/admin/usuarios/**; somente leitura (nenhuma mutacao de status
   global ainda - limitacao documentada em docs/PAINEL_ADMINISTRATIVO.md, sem
   regra de negocio consolidada para ativacao/inativacao global). Nunca exibe
   senha/hash/token. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };
	var CONVITE_STATUS_LABEL = { PENDENTE: "Pendente", UTILIZADO: "Utilizado", EXPIRADO: "Expirado", REVOGADO: "Revogado" };

	var modalDetalhe;

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

	function iniciar() {
		var tbody = el("criati-admin-usuarios-tbody");
		if (!tbody) {
			return;
		}

		modalDetalhe = window.CriatiUI.criarModal(el("criati-admin-usuario-detalhe-modal"));
		el("criati-admin-usuario-detalhe-fechar").addEventListener("click", modalDetalhe.fechar);

		el("criati-admin-usuarios-busca").addEventListener("input", debounce(carregar, 300));
		el("criati-admin-usuarios-filtro-status").addEventListener("change", carregar);

		carregar();
	}

	function montarQuery() {
		var busca = el("criati-admin-usuarios-busca").value;
		var status = el("criati-admin-usuarios-filtro-status").value;
		var partes = [];
		if (busca) {
			partes.push("busca=" + encodeURIComponent(busca));
		}
		if (status) {
			partes.push("status=" + encodeURIComponent(status));
		}
		return partes.length ? "?" + partes.join("&") : "";
	}

	function carregar() {
		var carregando = el("criati-admin-usuarios-carregando");
		var erro = el("criati-admin-usuarios-erro");
		var vazio = el("criati-admin-usuarios-vazio");
		var tabelaWrap = el("criati-admin-usuarios-tabela-wrap");
		var cards = el("criati-admin-usuarios-cards");

		carregando.hidden = false;
		erro.hidden = true;
		vazio.hidden = true;
		tabelaWrap.hidden = true;
		cards.innerHTML = "";

		window.CriatiApi
			.get("/api/admin/usuarios" + montarQuery())
			.then(function (resposta) {
				carregando.hidden = true;
				var usuarios = resposta.data || [];
				el("criati-admin-usuarios-contador").textContent =
					usuarios.length === 1 ? "1 usuario" : usuarios.length + " usuarios";

				if (usuarios.length === 0) {
					vazio.hidden = false;
					return;
				}
				tabelaWrap.hidden = false;
				renderTabela(usuarios);
				renderCards(usuarios);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderTabela(usuarios) {
		var tbody = el("criati-admin-usuarios-tbody");
		tbody.innerHTML = "";
		usuarios.forEach(function (usuario) {
			tbody.appendChild(criarLinha(usuario));
		});
	}

	function criarLinha(usuario) {
		var linha = document.createElement("tr");

		linha.appendChild(criarCelula(usuario.nome));
		linha.appendChild(criarCelula(usuario.email));

		var celulaStatus = document.createElement("td");
		celulaStatus.appendChild(montarBadgeStatus(usuario.status));
		linha.appendChild(celulaStatus);

		linha.appendChild(criarCelula(formatarData(usuario.criadoEm)));
		linha.appendChild(criarCelula(String(usuario.quantidadeEmpresas)));
		linha.appendChild(criarCelula(String(usuario.vinculosAtivos)));

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		celulaAcoes.appendChild(criarBotaoDetalhes(usuario));
		linha.appendChild(celulaAcoes);

		return linha;
	}

	function renderCards(usuarios) {
		var container = el("criati-admin-usuarios-cards");
		container.innerHTML = "";
		usuarios.forEach(function (usuario) {
			var card = document.createElement("article");
			card.className = "criati-acesso-card";

			var titulo = document.createElement("div");
			titulo.className = "criati-acesso-card-titulo";
			titulo.textContent = usuario.nome;
			card.appendChild(titulo);

			card.appendChild(montarLinhaCard("E-mail", usuario.email));

			var linhaStatus = document.createElement("div");
			linhaStatus.className = "criati-acesso-card-linha";
			var rotulo = document.createElement("span");
			rotulo.textContent = "Status";
			var valor = document.createElement("span");
			valor.appendChild(montarBadgeStatus(usuario.status));
			linhaStatus.appendChild(rotulo);
			linhaStatus.appendChild(valor);
			card.appendChild(linhaStatus);

			card.appendChild(montarLinhaCard("Empresas", String(usuario.quantidadeEmpresas)));

			var acoes = document.createElement("div");
			acoes.className = "criati-acesso-card-acoes";
			acoes.appendChild(criarBotaoDetalhes(usuario));
			card.appendChild(acoes);

			container.appendChild(card);
		});
	}

	function criarBotaoDetalhes(usuario) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-ghost";
		botao.textContent = "Detalhes";
		botao.addEventListener("click", function () {
			abrirDetalhe(usuario.id);
		});
		return botao;
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

	function abrirDetalhe(usuarioId) {
		window.CriatiApi
			.get("/api/admin/usuarios/" + usuarioId)
			.then(function (resposta) {
				renderDetalhe(resposta.data);
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel carregar os detalhes deste usuario.");
			});
	}

	function renderDetalhe(usuario) {
		var corpo = el("criati-admin-usuario-detalhe-corpo");
		corpo.innerHTML = "";
		[
			["Nome", usuario.nome + (usuario.superAdministrador ? " (Superadministrador)" : "")],
			["E-mail", usuario.email],
			["Status global", STATUS_LABEL[usuario.status] || usuario.status],
			["Criado em", formatarData(usuario.criadoEm)]
		].forEach(function (par) {
			corpo.appendChild(montarLinhaDetalhe(par[0], par[1]));
		});

		var vinculosContainer = el("criati-admin-usuario-detalhe-vinculos");
		vinculosContainer.innerHTML = "";
		if (usuario.vinculos.length === 0) {
			var vazio = document.createElement("p");
			vazio.textContent = "Nenhuma empresa vinculada.";
			vinculosContainer.appendChild(vazio);
		} else {
			usuario.vinculos.forEach(function (vinculo) {
				var linha = document.createElement("div");
				linha.className = "criati-acesso-card-linha";
				var rotulo = document.createElement("span");
				rotulo.textContent = vinculo.empresaNome;
				var valor = document.createElement("span");
				var badgePerfil = document.createElement("span");
				badgePerfil.className = "criati-badge criati-badge-perfil";
				badgePerfil.textContent = PERFIL_LABEL[vinculo.perfil] || vinculo.perfil;
				var badgeStatus = document.createElement("span");
				badgeStatus.className = "criati-badge criati-badge-" + vinculo.status.toLowerCase();
				badgeStatus.textContent = STATUS_LABEL[vinculo.status] || vinculo.status;
				badgeStatus.style.marginLeft = "6px";
				valor.appendChild(badgePerfil);
				valor.appendChild(badgeStatus);
				linha.appendChild(rotulo);
				linha.appendChild(valor);
				vinculosContainer.appendChild(linha);
			});
		}

		var convitesContainer = el("criati-admin-usuario-detalhe-convites");
		convitesContainer.innerHTML = "";
		if (usuario.convitesPendentes.length === 0) {
			var semConvites = document.createElement("p");
			semConvites.textContent = "Nenhum convite pendente.";
			convitesContainer.appendChild(semConvites);
		} else {
			usuario.convitesPendentes.forEach(function (convite) {
				var linha = document.createElement("div");
				linha.className = "criati-acesso-card-linha";
				var rotulo = document.createElement("span");
				rotulo.textContent = convite.empresaNome;
				var valor = document.createElement("span");
				valor.textContent = (PERFIL_LABEL[convite.perfil] || convite.perfil) + " · "
					+ (CONVITE_STATUS_LABEL[convite.status] || convite.status);
				linha.appendChild(rotulo);
				linha.appendChild(valor);
				convitesContainer.appendChild(linha);
			});
		}

		modalDetalhe.abrir(el("criati-admin-usuario-detalhe-fechar"));
	}

	function montarLinhaDetalhe(rotulo, valor) {
		var linha = document.createElement("div");
		linha.className = "criati-modal-detalhe-linha";
		var spanRotulo = document.createElement("span");
		spanRotulo.textContent = rotulo;
		var spanValor = document.createElement("span");
		spanValor.textContent = valor;
		linha.appendChild(spanRotulo);
		linha.appendChild(spanValor);
		return linha;
	}

	window.CriatiAdminUsuarios = { iniciar: iniciar };
})(window, document);
