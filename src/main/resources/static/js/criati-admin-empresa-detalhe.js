/* Criati - detalhe administrativo de uma empresa (/app/admin/empresas/{id}).
   O id da empresa vem sempre da propria URL (ultimo segmento do path), nunca
   de um campo escondido/JS - mesmo padrao ja usado por convite/aceitar (token
   lido da URL, nunca do servidor). Consome /api/admin/empresas/{id}/** com
   ROLE_SUPERADMIN; nunca troca o contexto empresarial da sessao. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };
	var SITUACAO_LABEL = { PRONTA: "Pronta para operar", PENDENTE: "Pendente" };
	var VINCULO_LABEL = { ATIVO: "Habilitada", INATIVO: "Desabilitada" };
	var PERFIL_LABEL = { ADMINISTRADOR: "Administrador", GESTOR: "Gestor", USUARIO: "Usuario" };
	var CONVITE_STATUS_LABEL = { PENDENTE: "Pendente", UTILIZADO: "Utilizado", EXPIRADO: "Expirado", REVOGADO: "Revogado" };

	var empresaId = null;
	var modalConvite;
	var modalConviteSucesso;
	var modalConfirmar;
	var tokenAtual = null;

	function el(id) {
		return document.getElementById(id);
	}

	function obterEmpresaIdDaUrl() {
		var partes = window.location.pathname.split("/").filter(Boolean);
		return partes[partes.length - 1];
	}

	function formatarData(iso) {
		if (!iso) {
			return "-";
		}
		var data = new Date(iso);
		return isNaN(data.getTime()) ? "-" : data.toLocaleDateString("pt-BR");
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
		var conteudo = el("criati-empresa-detalhe-conteudo");
		if (!conteudo) {
			return;
		}
		empresaId = obterEmpresaIdDaUrl();
		el("criati-empresa-ver-vinculos").href = "/app/admin/vinculos?empresaId=" + empresaId;

		modalConvite = window.CriatiUI.criarModal(el("criati-convite-modal"));
		modalConviteSucesso = window.CriatiUI.criarModal(el("criati-convite-sucesso-modal"));
		modalConfirmar = window.CriatiUI.criarModal(el("criati-confirmar-modal"));

		el("criati-empresa-convite-novo").addEventListener("click", abrirModalConvite);
		el("criati-convite-cancelar").addEventListener("click", modalConvite.fechar);
		el("criati-convite-form").addEventListener("submit", salvarConvite);
		el("criati-convite-sucesso-fechar").addEventListener("click", fecharModalSucesso);
		el("criati-convite-sucesso-copiar").addEventListener("click", copiarLink);

		carregarDetalhe();
		carregarAplicacoes();
		carregarConvites();
	}

	function carregarDetalhe() {
		var carregando = el("criati-empresa-detalhe-carregando");
		var erro = el("criati-empresa-detalhe-erro");
		var conteudo = el("criati-empresa-detalhe-conteudo");

		window.CriatiApi
			.get("/api/admin/empresas/" + empresaId)
			.then(function (resposta) {
				carregando.hidden = true;
				conteudo.hidden = false;
				renderDetalhe(resposta.data);
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function renderDetalhe(empresa) {
		el("criati-empresa-detalhe-nome").textContent = empresa.nome;
		document.title = empresa.nome + " · Criati";

		var corpo = el("criati-empresa-detalhe-corpo");
		corpo.innerHTML = "";
		[
			["Nome fantasia", empresa.nomeFantasia || "-"],
			["CNPJ", empresa.cnpj],
			["Criada em", formatarData(empresa.criadoEm)],
			["Ultima atualizacao", formatarData(empresa.atualizadoEm)],
			["Usuarios ativos", String(empresa.quantidadeUsuariosAtivos)],
			["Administradores ativos", String(empresa.administradoresAtivos)],
			["Convites pendentes", String(empresa.convitesPendentes)]
		].forEach(function (par) {
			corpo.appendChild(montarLinhaDetalhe(par[0], par[1]));
		});

		var linhaStatus = montarLinhaDetalhe("Status", "");
		var valorStatus = linhaStatus.querySelector("span:last-child");
		valorStatus.textContent = "";
		valorStatus.appendChild(montarBadge(empresa.status, STATUS_LABEL));
		corpo.insertBefore(linhaStatus, corpo.firstChild);

		var linhaSituacao = montarLinhaDetalhe("Situacao operacional", "");
		var valorSituacao = linhaSituacao.querySelector("span:last-child");
		valorSituacao.textContent = "";
		var badgeSituacao = document.createElement("span");
		badgeSituacao.className = "criati-badge " +
			(empresa.situacaoOperacional === "PRONTA" ? "criati-badge-ativo" : "criati-badge-pendente");
		badgeSituacao.textContent = SITUACAO_LABEL[empresa.situacaoOperacional] || empresa.situacaoOperacional;
		valorSituacao.appendChild(badgeSituacao);
		corpo.insertBefore(linhaSituacao, corpo.children[1]);

		var acoes = el("criati-empresa-detalhe-acoes");
		acoes.innerHTML = "";
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-ghost";
		if (empresa.status === "ATIVO") {
			botao.textContent = "Inativar empresa";
			botao.addEventListener("click", function () {
				confirmarAlternanciaEmpresa(empresa, "inativar");
			});
		} else {
			botao.textContent = "Ativar empresa";
			botao.addEventListener("click", function () {
				confirmarAlternanciaEmpresa(empresa, "ativar");
			});
		}
		acoes.appendChild(botao);

		var resumoUsuarios = el("criati-empresa-usuarios-resumo");
		resumoUsuarios.innerHTML = "";
		var paragrafo = document.createElement("p");
		paragrafo.textContent = empresa.quantidadeUsuariosAtivos + " usuario(s) ativo(s), sendo "
			+ empresa.administradoresAtivos + " administrador(es).";
		resumoUsuarios.appendChild(paragrafo);
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

	function montarBadge(valor, mapa) {
		var badge = document.createElement("span");
		badge.className = "criati-badge criati-badge-" + valor.toLowerCase();
		badge.textContent = mapa[valor] || valor;
		return badge;
	}

	function confirmarAlternanciaEmpresa(empresa, acao) {
		var ehInativar = acao === "inativar";
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
					titulo: ehInativar ? "Inativar empresa" : "Ativar empresa",
					mensagem: ehInativar
						? "Esta empresa deixara de permitir novos acessos. Nenhum dado sera excluido."
						: "Esta empresa voltara a permitir acesso conforme os vinculos ja ativos.",
					rotuloConfirmar: ehInativar ? "Inativar" : "Ativar"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.post("/api/admin/empresas/" + empresaId + "/" + acao)
					.then(function () {
						window.CriatiUI.showToast("sucesso", ehInativar ? "Empresa inativada." : "Empresa ativada.");
						carregarDetalhe();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel atualizar a empresa agora.");
					});
			});
	}

	// --- Aplicacoes ---

	function carregarAplicacoes() {
		var lista = el("criati-empresa-aplicacoes-lista");
		lista.textContent = "Carregando aplicacoes...";
		window.CriatiApi
			.get("/api/admin/empresas/" + empresaId + "/aplicacoes")
			.then(function (resposta) {
				renderAplicacoes(lista, resposta.data || []);
			})
			.catch(function () {
				lista.textContent = "Nao foi possivel carregar as aplicacoes desta empresa.";
			});
	}

	function renderAplicacoes(lista, aplicacoes) {
		lista.innerHTML = "";
		aplicacoes.forEach(function (aplicacao) {
			var linha = document.createElement("div");
			linha.className = "criati-acesso-card";

			var nome = document.createElement("p");
			nome.className = "criati-acesso-card-titulo";
			nome.textContent = aplicacao.nome;

			var situacao = document.createElement("p");
			situacao.className = "criati-acesso-card-linha";
			situacao.textContent = "Situacao: " + (VINCULO_LABEL[aplicacao.statusVinculo] || "Nao habilitada");

			var habilitada = aplicacao.statusVinculo === "ATIVO";
			var botao = document.createElement("button");
			botao.type = "button";
			botao.className = "criati-btn " + (habilitada ? "criati-btn-ghost" : "criati-btn-primary");
			botao.textContent = habilitada ? "Desabilitar" : "Habilitar";
			botao.addEventListener("click", function () {
				alternarAplicacao(aplicacao.codigo, habilitada, botao);
			});

			linha.appendChild(nome);
			linha.appendChild(situacao);
			linha.appendChild(botao);
			lista.appendChild(linha);
		});
	}

	function alternarAplicacao(codigo, habilitada, botao) {
		var acao = habilitada ? "desabilitar" : "habilitar";
		window.CriatiUI.setButtonLoading(botao, true, "Aguarde...");
		window.CriatiApi
			.post("/api/admin/empresas/" + empresaId + "/aplicacoes/" + codigo + "/" + acao)
			.then(function () {
				carregarAplicacoes();
				carregarDetalhe();
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel atualizar essa aplicacao agora.");
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
	}

	// --- Convites ---

	function carregarConvites() {
		window.CriatiApi
			.get("/api/admin/empresas/" + empresaId + "/convites")
			.then(function (resposta) {
				renderConvites(resposta.data || []);
			})
			.catch(function () {
				window.CriatiUI.showToast("erro", "Nao foi possivel carregar os convites desta empresa.");
			});
	}

	function renderConvites(convites) {
		var vazio = el("criati-empresa-convites-vazio");
		var wrap = el("criati-empresa-convites-wrap");
		var tbody = el("criati-empresa-convites-tbody");

		tbody.innerHTML = "";
		if (convites.length === 0) {
			vazio.hidden = false;
			wrap.hidden = true;
			return;
		}
		vazio.hidden = true;
		wrap.hidden = false;

		convites.forEach(function (convite) {
			tbody.appendChild(criarLinhaConvite(convite));
		});
	}

	function criarLinhaConvite(convite) {
		var linha = document.createElement("tr");

		var celulaEmail = document.createElement("td");
		celulaEmail.textContent = convite.email;

		var celulaPerfil = document.createElement("td");
		var badgePerfil = document.createElement("span");
		badgePerfil.className = "criati-badge criati-badge-perfil";
		badgePerfil.textContent = PERFIL_LABEL[convite.perfil] || convite.perfil;
		celulaPerfil.appendChild(badgePerfil);

		var celulaStatus = document.createElement("td");
		var badgeStatus = document.createElement("span");
		badgeStatus.className = "criati-badge criati-badge-" + convite.status.toLowerCase();
		badgeStatus.textContent = CONVITE_STATUS_LABEL[convite.status] || convite.status;
		celulaStatus.appendChild(badgeStatus);

		var celulaCriado = document.createElement("td");
		celulaCriado.textContent = formatarDataHora(convite.criadoEm);

		var celulaExpira = document.createElement("td");
		celulaExpira.textContent = formatarDataHora(convite.expiraEm);

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		var botaoRevogar = document.createElement("button");
		botaoRevogar.type = "button";
		botaoRevogar.className = "criati-btn criati-btn-ghost";
		botaoRevogar.textContent = "Revogar";
		if (convite.status === "PENDENTE") {
			botaoRevogar.addEventListener("click", function () {
				confirmarRevogarConvite(convite);
			});
		} else {
			botaoRevogar.disabled = true;
			botaoRevogar.title = "Somente convites pendentes podem ser revogados.";
		}
		celulaAcoes.appendChild(botaoRevogar);

		[celulaEmail, celulaPerfil, celulaStatus, celulaCriado, celulaExpira, celulaAcoes].forEach(function (celula) {
			linha.appendChild(celula);
		});
		return linha;
	}

	function abrirModalConvite() {
		el("criati-convite-form").reset();
		el("criati-convite-perfil").value = "ADMINISTRADOR";
		modalConvite.abrir(el("criati-convite-email"));
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
			.post("/api/admin/empresas/" + empresaId + "/convites", dados)
			.then(function (resposta) {
				modalConvite.fechar();
				abrirModalSucesso(resposta.data);
				carregarConvites();
				carregarDetalhe();
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

		modalConviteSucesso.abrir(el("criati-convite-sucesso-fechar"));
	}

	function fecharModalSucesso() {
		tokenAtual = null;
		el("criati-convite-sucesso-link-input").value = "";
		modalConviteSucesso.fechar();
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

	function confirmarRevogarConvite(convite) {
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
					mensagem: "Este convite sera revogado e nao podera mais ser utilizado.",
					rotuloConfirmar: "Revogar"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.delete("/api/admin/empresas/" + empresaId + "/convites/" + convite.id)
					.then(function () {
						window.CriatiUI.showToast("sucesso", "Convite revogado.");
						carregarConvites();
						carregarDetalhe();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel revogar agora.");
					});
			});
	}

	window.CriatiAdminEmpresaDetalhe = { iniciar: iniciar };
})(window, document);
