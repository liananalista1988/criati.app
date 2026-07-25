/* Criati - tela de gestao de usuarios da empresa ativa (/app/usuarios).
   Consome exclusivamente /api/contexto/usuarios/**; nunca decide autorizacao
   por conta propria (isso e sempre confirmado pelo backend a cada chamada) e
   nunca envia empresaId - a empresa ativa vem sempre da sessao no servidor. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };

	var usuarioEmEdicaoPerfilId = null;
	var usuarioEmRedefinicaoSenhaId = null;

	function el(id) {
		return document.getElementById(id);
	}

	function formatarData(iso) {
		if (!iso) {
			return "-";
		}
		var data = new Date(iso);
		if (isNaN(data.getTime())) {
			return "-";
		}
		return data.toLocaleDateString("pt-BR");
	}

	function debounce(fn, atraso) {
		var timer;
		return function () {
			window.clearTimeout(timer);
			timer = window.setTimeout(fn, atraso);
		};
	}

	var modalDetalhe;
	var modalPerfil;
	var modalConfirmar;
	var modalRedefinirSenha;

	function iniciar() {
		var tbody = el("criati-usuarios-tbody");
		if (!tbody) {
			return;
		}

		modalDetalhe = window.CriatiUI.criarModal(el("criati-usuario-detalhe-modal"));
		modalPerfil = window.CriatiUI.criarModal(el("criati-perfil-modal"));
		modalConfirmar = window.CriatiUI.criarModal(el("criati-confirmar-modal"));
		modalRedefinirSenha = window.CriatiUI.criarModal(el("criati-redefinir-senha-modal"));

		el("criati-usuario-detalhe-fechar").addEventListener("click", modalDetalhe.fechar);
		el("criati-perfil-cancelar").addEventListener("click", modalPerfil.fechar);
		el("criati-perfil-form").addEventListener("submit", salvarPerfil);
		el("criati-redefinir-senha-cancelar").addEventListener("click", modalRedefinirSenha.fechar);
		el("criati-redefinir-senha-form").addEventListener("submit", salvarRedefinicaoSenha);

		el("criati-usuarios-busca").addEventListener("input", debounce(carregar, 350));
		el("criati-usuarios-filtro-perfil").addEventListener("change", carregar);
		el("criati-usuarios-filtro-status").addEventListener("change", carregar);

		carregar();
	}

	function montarQuery() {
		var params = {
			status: el("criati-usuarios-filtro-status").value,
			perfil: el("criati-usuarios-filtro-perfil").value,
			busca: el("criati-usuarios-busca").value
		};
		var partes = [];
		Object.keys(params).forEach(function (chave) {
			if (params[chave]) {
				partes.push(encodeURIComponent(chave) + "=" + encodeURIComponent(params[chave]));
			}
		});
		return partes.length ? "?" + partes.join("&") : "";
	}

	function carregar() {
		var carregando = el("criati-usuarios-carregando");
		var erro = el("criati-usuarios-erro");
		var vazio = el("criati-usuarios-vazio");
		var tabelaWrap = el("criati-usuarios-tabela-wrap");
		var cards = el("criati-usuarios-cards");

		carregando.hidden = false;
		erro.hidden = true;
		vazio.hidden = true;
		tabelaWrap.hidden = true;
		cards.innerHTML = "";

		window.CriatiApi
			.get("/api/contexto/usuarios" + montarQuery())
			.then(function (resposta) {
				carregando.hidden = true;
				var usuarios = resposta.data || [];
				el("criati-usuarios-contador").textContent = usuarios.length === 1
					? "1 integrante"
					: usuarios.length + " integrantes";

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
		var tbody = el("criati-usuarios-tbody");
		tbody.innerHTML = "";
		usuarios.forEach(function (usuario) {
			tbody.appendChild(criarLinha(usuario));
		});
	}

	function criarLinha(usuario) {
		var linha = document.createElement("tr");

		var celulaNome = document.createElement("td");
		celulaNome.appendChild(montarNomeComBadge(usuario));

		var celulaEmail = document.createElement("td");
		celulaEmail.textContent = usuario.email;

		var celulaPerfil = document.createElement("td");
		var badgePerfil = document.createElement("span");
		badgePerfil.className = "criati-badge criati-badge-perfil";
		badgePerfil.textContent = PERFIL_LABEL[usuario.perfil] || usuario.perfil;
		celulaPerfil.appendChild(badgePerfil);

		var celulaStatus = document.createElement("td");
		celulaStatus.appendChild(montarBadgeStatus(usuario.status));

		var celulaEntrada = document.createElement("td");
		celulaEntrada.textContent = formatarData(usuario.criadoEm);

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		celulaAcoes.appendChild(montarAcoes(usuario));

		linha.appendChild(celulaNome);
		linha.appendChild(celulaEmail);
		linha.appendChild(celulaPerfil);
		linha.appendChild(celulaStatus);
		linha.appendChild(celulaEntrada);
		linha.appendChild(celulaAcoes);
		return linha;
	}

	function renderCards(usuarios) {
		var container = el("criati-usuarios-cards");
		container.innerHTML = "";
		usuarios.forEach(function (usuario) {
			container.appendChild(criarCard(usuario));
		});
	}

	function criarCard(usuario) {
		var card = document.createElement("article");
		card.className = "criati-acesso-card";

		var titulo = document.createElement("div");
		titulo.className = "criati-acesso-card-titulo";
		titulo.appendChild(montarNomeComBadge(usuario));

		var linhaEmail = montarLinhaCard("E-mail", usuario.email);
		var linhaPerfil = montarLinhaCard("Perfil", PERFIL_LABEL[usuario.perfil] || usuario.perfil);

		var linhaStatus = document.createElement("div");
		linhaStatus.className = "criati-acesso-card-linha";
		var rotuloStatus = document.createElement("span");
		rotuloStatus.textContent = "Status";
		var valorStatus = document.createElement("span");
		valorStatus.appendChild(montarBadgeStatus(usuario.status));
		linhaStatus.appendChild(rotuloStatus);
		linhaStatus.appendChild(valorStatus);

		var linhaEntrada = montarLinhaCard("Entrada", formatarData(usuario.criadoEm));

		var acoes = document.createElement("div");
		acoes.className = "criati-acesso-card-acoes";
		acoes.appendChild(montarAcoes(usuario));

		card.appendChild(titulo);
		card.appendChild(linhaEmail);
		card.appendChild(linhaPerfil);
		card.appendChild(linhaStatus);
		card.appendChild(linhaEntrada);
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

	function montarNomeComBadge(usuario) {
		var container = document.createElement("span");
		var nome = document.createTextNode(usuario.nome + " ");
		container.appendChild(nome);
		if (usuario.proprioUsuario) {
			var badge = document.createElement("span");
			badge.className = "criati-badge criati-badge-voce";
			badge.textContent = "Voce";
			container.appendChild(badge);
		}
		return container;
	}

	function montarBadgeStatus(status) {
		var badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + status.toLowerCase();
		badge.textContent = STATUS_LABEL[status] || status;
		return badge;
	}

	function montarAcoes(usuario) {
		var container = document.createElement("div");
		container.className = "criati-table-acoes";

		var botaoDetalhes = criarBotao("Detalhes", function () {
			abrirDetalhe(usuario);
		});
		container.appendChild(botaoDetalhes);

		if (usuario.status === "ATIVO") {
			container.appendChild(criarBotao("Perfil", function () {
				abrirPerfilModal(usuario);
			}));

			var botaoRedefinirSenha = criarBotao("Redefinir senha", function () {
				abrirRedefinirSenhaModal(usuario);
			});
			if (usuario.proprioUsuario) {
				botaoRedefinirSenha.disabled = true;
				botaoRedefinirSenha.title = "Voce nao pode redefinir a propria senha por este fluxo.";
			}
			container.appendChild(botaoRedefinirSenha);

			var botaoSuspender = criarBotao("Suspender", function () {
				confirmarSuspender(usuario);
			});
			if (usuario.proprioUsuario) {
				botaoSuspender.disabled = true;
				botaoSuspender.title = "Voce nao pode suspender o proprio acesso.";
			}
			container.appendChild(botaoSuspender);

			var botaoRemover = criarBotao("Remover acesso", function () {
				confirmarRemover(usuario);
			});
			if (usuario.proprioUsuario) {
				botaoRemover.disabled = true;
				botaoRemover.title = "Voce nao pode remover o proprio acesso.";
			}
			container.appendChild(botaoRemover);
		} else {
			container.appendChild(criarBotao("Reativar", function () {
				reativar(usuario);
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

	function abrirDetalhe(usuario) {
		window.CriatiApi
			.get("/api/contexto/usuarios/" + usuario.usuarioEmpresaId)
			.then(function (resposta) {
				renderDetalhe(resposta.data);
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel carregar os detalhes deste usuario.");
			});
	}

	function renderDetalhe(usuario) {
		var corpo = el("criati-usuario-detalhe-corpo");
		corpo.innerHTML = "";
		[
			["Nome", usuario.nome + (usuario.proprioUsuario ? " (Voce)" : "")],
			["E-mail", usuario.email],
			["Perfil", PERFIL_LABEL[usuario.perfil] || usuario.perfil],
			["Status", STATUS_LABEL[usuario.status] || usuario.status],
			["Entrada", formatarData(usuario.criadoEm)],
			["Ultima atualizacao", formatarData(usuario.atualizadoEm)]
		].forEach(function (par) {
			var linha = document.createElement("div");
			linha.className = "criati-modal-detalhe-linha";
			var rotulo = document.createElement("span");
			rotulo.textContent = par[0];
			var valor = document.createElement("span");
			valor.textContent = par[1];
			linha.appendChild(rotulo);
			linha.appendChild(valor);
			corpo.appendChild(linha);
		});

		var acoes = el("criati-usuario-detalhe-acoes");
		acoes.innerHTML = "";
		acoes.appendChild(montarAcoes(usuario));

		modalDetalhe.abrir(el("criati-usuario-detalhe-fechar"));
	}

	function abrirPerfilModal(usuario) {
		usuarioEmEdicaoPerfilId = usuario.usuarioEmpresaId;
		el("criati-perfil-atual").textContent = "Perfil atual: " + (PERFIL_LABEL[usuario.perfil] || usuario.perfil);
		el("criati-perfil-select").value = usuario.perfil;

		var optGestor = el("criati-perfil-select").querySelector('option[value="GESTOR"]');
		var optUsuario = el("criati-perfil-select").querySelector('option[value="USUARIO"]');
		var ajuda = el("criati-perfil-ajuda");
		if (usuario.proprioUsuario) {
			optGestor.disabled = true;
			optUsuario.disabled = true;
			ajuda.hidden = false;
		} else {
			optGestor.disabled = false;
			optUsuario.disabled = false;
			ajuda.hidden = true;
		}

		modalPerfil.abrir(el("criati-perfil-select"));
	}

	function salvarPerfil(evento) {
		evento.preventDefault();
		var botao = el("criati-perfil-salvar");
		var novoPerfil = el("criati-perfil-select").value;

		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		window.CriatiApi
			.request("/api/contexto/usuarios/" + usuarioEmEdicaoPerfilId + "/perfil", {
				method: "PATCH",
				body: { perfil: novoPerfil }
			})
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Perfil atualizado com sucesso.");
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

	function abrirRedefinirSenhaModal(usuario) {
		usuarioEmRedefinicaoSenhaId = usuario.usuarioEmpresaId;
		el("criati-redefinir-senha-usuario").textContent = "Usuario: " + usuario.nome + " (" + usuario.email + ")";
		el("criati-redefinir-senha-form").reset();
		var mensagem = el("criati-redefinir-senha-mensagem");
		mensagem.hidden = true;
		modalRedefinirSenha.abrir(el("criati-redefinir-senha-nova"));
	}

	function salvarRedefinicaoSenha(evento) {
		evento.preventDefault();
		var mensagem = el("criati-redefinir-senha-mensagem");
		mensagem.hidden = true;
		var botao = el("criati-redefinir-senha-salvar");
		var novaSenha = el("criati-redefinir-senha-nova").value;
		var confirmacaoSenha = el("criati-redefinir-senha-confirmacao").value;

		window.CriatiUI.setButtonLoading(botao, true, "Redefinindo...");
		window.CriatiApi
			.post("/api/contexto/usuarios/" + usuarioEmRedefinicaoSenhaId + "/redefinir-senha", {
				novaSenha: novaSenha,
				confirmacaoSenha: confirmacaoSenha
			})
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Senha redefinida com sucesso.");
				modalRedefinirSenha.fechar();
			})
			.catch(function (erro) {
				mensagem.textContent = (erro && erro.message) || "Nao foi possivel redefinir a senha agora.";
				mensagem.className = "criati-alert criati-alert-erro";
				mensagem.hidden = false;
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	function confirmarSuspender(usuario) {
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
					titulo: "Suspender acesso",
					mensagem: "Este usuario perdera acesso a esta empresa, mas continuara existindo na "
						+ "plataforma e podera manter acesso a outras empresas.",
					rotuloConfirmar: "Suspender"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.post("/api/contexto/usuarios/" + usuario.usuarioEmpresaId + "/suspender")
					.then(function () {
						window.CriatiUI.showToast("sucesso", "Acesso suspenso.");
						modalDetalhe.fechar();
						carregar();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel suspender agora.");
					});
			});
	}

	function confirmarRemover(usuario) {
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
					titulo: "Remover acesso",
					mensagem: "Este acesso sera removido apenas desta empresa. O usuario global e os acessos "
						+ "a outras empresas serao preservados.",
					rotuloConfirmar: "Remover acesso"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.delete("/api/contexto/usuarios/" + usuario.usuarioEmpresaId)
					.then(function () {
						window.CriatiUI.showToast("sucesso", "Acesso removido desta empresa.");
						modalDetalhe.fechar();
						carregar();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel remover agora.");
					});
			});
	}

	function reativar(usuario) {
		window.CriatiApi
			.post("/api/contexto/usuarios/" + usuario.usuarioEmpresaId + "/reativar")
			.then(function () {
				window.CriatiUI.showToast("sucesso", "Acesso reativado.");
				modalDetalhe.fechar();
				carregar();
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel reativar agora.");
			});
	}

	window.CriatiUsuarios = { iniciar: iniciar };
})(window, document);
