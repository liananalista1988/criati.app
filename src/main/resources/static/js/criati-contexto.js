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

	/**
	 * Renderiza o seletor de empresa da topbar a partir do estado atual - e a
	 * unica funcao que decide o conteudo de #criati-topbar-empresa, chamada em
	 * todo caminho de iniciar() (sucesso, vazio ou selecao pendente) para que o
	 * skeleton inicial (ver fragments/topbar.html) nunca fique preso. Quando
	 * nao ha empresas (ex.: Superadministrador, que nunca tem vinculo proprio),
	 * o container e simplesmente esvaziado - nao e um erro, e um estado valido
	 * ("nao aplicavel a este papel").
	 */
	function renderTopbarEmpresa(empresas, empresaAtivaId, aoTrocar) {
		var container = el("criati-topbar-empresa");
		if (!container) {
			return;
		}
		container.innerHTML = "";

		if (empresas.length === 0) {
			return;
		}

		if (empresas.length === 1 && empresaAtivaId) {
			var texto = document.createElement("span");
			texto.textContent = empresas[0].nomeEmpresa;
			container.appendChild(texto);
			return;
		}

		var select = document.createElement("select");
		select.className = "criati-select";
		select.id = "criati-empresa-select";
		select.setAttribute("aria-label", "Empresa ativa");

		if (!empresaAtivaId) {
			var placeholder = document.createElement("option");
			placeholder.value = "";
			placeholder.textContent = "Selecionar empresa";
			placeholder.disabled = true;
			placeholder.selected = true;
			select.appendChild(placeholder);
		}

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

	/**
	 * Esvazia o seletor de empresa da topbar sem exibir nenhum dado - usado
	 * quando a consulta de contexto falha (rede/servidor) e quando uma pagina
	 * nao tem o conceito de "empresa ativa" (ver iniciarIdentidade). Nunca
	 * deixa o skeleton inicial preso.
	 */
	function limparTopbarEmpresa() {
		var container = el("criati-topbar-empresa");
		if (container) {
			container.innerHTML = "";
		}
	}

	/**
	 * Superadministrador nunca tem vinculo de empresa (regra ja documentada em
	 * docs/PAINEL_ADMINISTRATIVO.md) - "nenhuma empresa disponivel" e o estado
	 * permanente e esperado para esse papel, nao uma falha. A deteccao e feita
	 * pela propria marcacao do servidor (o link para /app/admin so e
	 * renderizado por fragments/sidebar.html quando o model attribute
	 * "superAdministrador" e verdadeiro) - sem novo endpoint nem novo campo no
	 * contrato de /api/auth/me.
	 */
	function ehSuperAdministrador() {
		return Boolean(document.querySelector('a[href="/app/admin"]'));
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

		// Para o Superadministrador, "nenhuma empresa vinculada" e permanente e
		// esperado (nunca decorre de um vinculo, ver docs/PAINEL_ADMINISTRATIVO.md),
		// nao uma pendencia do usuario - a mensagem generica de baixo (que continua
		// correta para um usuario comum sem vinculo) e substituida por uma que
		// reflete isso e aponta para a tela de Empresas, ja existente e autorizada
		// (sidebar "Administracao da plataforma", visivel somente a esse papel).
		var textoVazio = el("criati-dashboard-vazio-texto");
		if (textoVazio && ehSuperAdministrador()) {
			textoVazio.textContent = "Superadministrador nao possui empresa ativa - isso e esperado para este papel.";
			mostrar("criati-dashboard-vazio-acao", true);
		} else {
			mostrar("criati-dashboard-vazio-acao", false);
		}
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

				function selecionar(empresaId) {
					return window.CriatiApi
						.post("/api/contexto/empresa-ativa", { empresaId: empresaId })
						.then(function () {
							// O backend resolve a quantidade de modulos da nova empresa:
							// um abre direto; zero ou varios exibem o panorama modular.
							window.location.assign("/app/aplicacoes");
						})
						.catch(function () {
							window.CriatiUI.showToast("erro", "Nao foi possivel selecionar essa empresa agora.");
						});
				}

				// Renderizado sempre, em qualquer ramo abaixo (vazio, selecao
				// pendente ou sucesso) - e a correcao do skeleton que ficava preso
				// em #criati-topbar-empresa quando o usuario nao tinha empresa
				// ativa (ver docs/CORRECAO-CONTEXTO-SUPERADMIN-F4-009.md).
				renderTopbarEmpresa(empresas, contexto ? contexto.empresaId : null, selecionar);

				if (empresas.length === 0) {
					estadoVazio();
					return;
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

				renderCards(usuario, empresas, contexto);
				estadoConteudo();
			})
			.catch(function () {
				limparTopbarEmpresa();
				estadoErro();
			});
	}

	/**
	 * Para paginas administrativas (/app/admin/**), que exigem ROLE_SUPERADMIN
	 * e nunca tem o conceito de "empresa ativa" (o Superadministrador opera
	 * sobre empresas explicitamente por id, nunca por contexto de sessao -
	 * ver ContextoEmpresaService/AdminEmpresaController): popula apenas a
	 * identidade do usuario na topbar e limpa o seletor de empresa
	 * imediatamente (nunca "carrega" - simplesmente nao se aplica aqui), em
	 * vez de reusar iniciar() (que pressupoe vinculo de empresa e mostraria
	 * um estado de selecao/vazio sem sentido para este papel).
	 */
	function iniciarIdentidade() {
		limparTopbarEmpresa();
		window.CriatiApi.get("/api/auth/me")
			.then(function (resposta) {
				renderUsuario(resposta.data);
			})
			.catch(function () {
				// Falha ao carregar a identidade (rede/servidor): mantem o
				// placeholder neutro ja renderizado no HTML (ver topbar.html),
				// sem expor nenhum dado nem travar a pagina.
			});
	}

	window.CriatiContexto = { iniciar: iniciar, iniciarIdentidade: iniciarIdentidade };
})(window, document);
