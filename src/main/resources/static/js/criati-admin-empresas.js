/* Criati - listagem administrativa de empresas (/app/admin/empresas).
   Consome GET /api/admin/empresas (lista basica) e, para cada empresa, GET
   /api/admin/empresas/{id} (dados agregados: usuarios ativos, administradores,
   aplicacoes habilitadas) - o backend nao pagina nem filtra ainda (volume
   inicial pequeno, decisao documentada em docs/PAINEL_ADMINISTRATIVO.md);
   busca/status/aplicacao sao filtrados aqui mesmo, no cliente. */
(function (window, document) {
	"use strict";

	var STATUS_LABEL = { ATIVO: "Ativo", INATIVO: "Inativo" };

	var empresasEmMemoria = [];
	var modalConfirmar;

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

	function iniciar() {
		var tbody = el("criati-admin-empresas-tbody");
		if (!tbody) {
			return;
		}

		modalConfirmar = window.CriatiUI.criarModal(el("criati-confirmar-modal"));

		el("criati-admin-empresas-busca").addEventListener("input", debounce(aplicarFiltros, 300));
		el("criati-admin-empresas-filtro-status").addEventListener("change", aplicarFiltros);
		el("criati-admin-empresas-filtro-aplicacao").addEventListener("change", aplicarFiltros);

		carregar();
	}

	function carregar() {
		var carregando = el("criati-admin-empresas-carregando");
		var erro = el("criati-admin-empresas-erro");

		carregando.hidden = false;
		erro.hidden = true;

		window.CriatiApi
			.get("/api/admin/empresas")
			.then(function (resposta) {
				var empresas = resposta.data || [];
				return Promise.all(empresas.map(function (empresa) {
					return window.CriatiApi
						.get("/api/admin/empresas/" + empresa.id)
						.then(function (detalheResposta) {
							return detalheResposta.data;
						})
						.catch(function () {
							return Object.assign({}, empresa, {
								quantidadeUsuariosAtivos: null,
								administradoresAtivos: null,
								aplicacoesHabilitadas: []
							});
						});
				}));
			})
			.then(function (detalhes) {
				carregando.hidden = true;
				empresasEmMemoria = detalhes;
				aplicarFiltros();
			})
			.catch(function () {
				carregando.hidden = true;
				erro.hidden = false;
			});
	}

	function aplicarFiltros() {
		var buscaNormalizada = el("criati-admin-empresas-busca").value.trim().toLowerCase();
		var statusFiltro = el("criati-admin-empresas-filtro-status").value;
		var aplicacaoFiltro = el("criati-admin-empresas-filtro-aplicacao").value;

		var filtradas = empresasEmMemoria.filter(function (empresa) {
			if (buscaNormalizada && empresa.nome.toLowerCase().indexOf(buscaNormalizada) === -1) {
				return false;
			}
			if (statusFiltro && empresa.status !== statusFiltro) {
				return false;
			}
			if (aplicacaoFiltro && (empresa.aplicacoesHabilitadas || []).indexOf(aplicacaoFiltro) === -1) {
				return false;
			}
			return true;
		});

		renderLista(filtradas);
	}

	function renderLista(empresas) {
		var vazio = el("criati-admin-empresas-vazio");
		var tabelaWrap = el("criati-admin-empresas-tabela-wrap");
		var cards = el("criati-admin-empresas-cards");
		var contador = el("criati-admin-empresas-contador");

		contador.textContent = empresas.length === 1 ? "1 empresa" : empresas.length + " empresas";
		cards.innerHTML = "";

		if (empresas.length === 0) {
			vazio.hidden = false;
			tabelaWrap.hidden = true;
			return;
		}
		vazio.hidden = true;
		tabelaWrap.hidden = false;

		var tbody = el("criati-admin-empresas-tbody");
		tbody.innerHTML = "";
		empresas.forEach(function (empresa) {
			tbody.appendChild(criarLinha(empresa));
			cards.appendChild(criarCard(empresa));
		});
	}

	function criarLinha(empresa) {
		var linha = document.createElement("tr");

		var celulaNome = document.createElement("td");
		var link = document.createElement("a");
		link.href = "/app/admin/empresas/" + empresa.id;
		link.textContent = empresa.nome;
		celulaNome.appendChild(link);

		var celulaCnpj = document.createElement("td");
		celulaCnpj.textContent = empresa.cnpj;

		var celulaStatus = document.createElement("td");
		celulaStatus.appendChild(montarBadgeStatus(empresa.status));

		var celulaCriada = document.createElement("td");
		celulaCriada.textContent = formatarData(empresa.criadoEm);

		var celulaUsuarios = document.createElement("td");
		celulaUsuarios.textContent = empresa.quantidadeUsuariosAtivos === null
			? "-" : String(empresa.quantidadeUsuariosAtivos);

		var celulaAdmins = document.createElement("td");
		celulaAdmins.textContent = empresa.administradoresAtivos === null
			? "-" : String(empresa.administradoresAtivos);

		var celulaAplicacoes = document.createElement("td");
		celulaAplicacoes.textContent = (empresa.aplicacoesHabilitadas || []).length
			? empresa.aplicacoesHabilitadas.join(", ")
			: "Nenhuma";

		var celulaAcoes = document.createElement("td");
		celulaAcoes.className = "criati-table-acoes";
		celulaAcoes.appendChild(montarAcoes(empresa));

		[celulaNome, celulaCnpj, celulaStatus, celulaCriada, celulaUsuarios, celulaAdmins, celulaAplicacoes, celulaAcoes]
			.forEach(function (celula) {
				linha.appendChild(celula);
			});
		return linha;
	}

	function criarCard(empresa) {
		var card = document.createElement("article");
		card.className = "criati-acesso-card";

		var titulo = document.createElement("div");
		titulo.className = "criati-acesso-card-titulo";
		var link = document.createElement("a");
		link.href = "/app/admin/empresas/" + empresa.id;
		link.textContent = empresa.nome;
		titulo.appendChild(link);
		card.appendChild(titulo);

		card.appendChild(montarLinhaCard("CNPJ", empresa.cnpj));

		var linhaStatus = document.createElement("div");
		linhaStatus.className = "criati-acesso-card-linha";
		var rotulo = document.createElement("span");
		rotulo.textContent = "Status";
		var valor = document.createElement("span");
		valor.appendChild(montarBadgeStatus(empresa.status));
		linhaStatus.appendChild(rotulo);
		linhaStatus.appendChild(valor);
		card.appendChild(linhaStatus);

		card.appendChild(montarLinhaCard("Criada em", formatarData(empresa.criadoEm)));
		card.appendChild(montarLinhaCard(
			"Usuarios ativos", empresa.quantidadeUsuariosAtivos === null ? "-" : String(empresa.quantidadeUsuariosAtivos)));
		card.appendChild(montarLinhaCard(
			"Aplicacoes", (empresa.aplicacoesHabilitadas || []).length ? empresa.aplicacoesHabilitadas.join(", ") : "Nenhuma"));

		var acoes = document.createElement("div");
		acoes.className = "criati-acesso-card-acoes";
		acoes.appendChild(montarAcoes(empresa));
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

	function montarAcoes(empresa) {
		var container = document.createElement("div");
		container.className = "criati-table-acoes";

		var botaoDetalhe = document.createElement("a");
		botaoDetalhe.className = "criati-btn criati-btn-ghost";
		botaoDetalhe.href = "/app/admin/empresas/" + empresa.id;
		botaoDetalhe.textContent = "Ver detalhes";
		container.appendChild(botaoDetalhe);

		var botaoAlternarStatus = document.createElement("button");
		botaoAlternarStatus.type = "button";
		botaoAlternarStatus.className = "criati-btn criati-btn-ghost";
		if (empresa.status === "ATIVO") {
			botaoAlternarStatus.textContent = "Inativar";
			botaoAlternarStatus.addEventListener("click", function () {
				confirmarAlternancia(empresa, "inativar");
			});
		} else {
			botaoAlternarStatus.textContent = "Ativar";
			botaoAlternarStatus.addEventListener("click", function () {
				confirmarAlternancia(empresa, "ativar");
			});
		}
		container.appendChild(botaoAlternarStatus);

		return container;
	}

	function confirmarAlternancia(empresa, acao) {
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
						? "A empresa \"" + empresa.nome + "\" deixara de permitir novos acessos. Nenhum dado, "
							+ "usuario ou vinculo sera excluido."
						: "A empresa \"" + empresa.nome + "\" voltara a permitir acesso conforme os vinculos ja ativos.",
					rotuloConfirmar: ehInativar ? "Inativar" : "Ativar"
				})
			.then(function (confirmado) {
				if (!confirmado) {
					return;
				}
				return window.CriatiApi
					.post("/api/admin/empresas/" + empresa.id + "/" + acao)
					.then(function () {
						window.CriatiUI.showToast("sucesso", ehInativar ? "Empresa inativada." : "Empresa ativada.");
						carregar();
					})
					.catch(function (erro) {
						window.CriatiUI.showToast("erro", (erro && erro.message) || "Nao foi possivel atualizar a empresa agora.");
					});
			});
	}

	window.CriatiAdminEmpresas = { iniciar: iniciar };
})(window, document);
