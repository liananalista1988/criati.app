/* Telas operacionais de importacao bancaria OFX (CRIATI-FIN-016).
   Fundacao apenas: upload, listagem, pre-visualizacao e descarte logico de
   lotes. Valores, contadores, status e sinalizacao de duplicidade vem sempre
   do backend (ImportacaoBancariaService); este arquivo nao recalcula hash,
   duplicidade nem nenhum valor financeiro, e nunca cria LancamentoFinanceiro. */
(function (window, document) {
	"use strict";

	var api = window.FinanceiroApi;
	var formatacao = window.FinanceiroFormatacao;

	// Espelha o padrao configuravel no backend (application.properties /
	// CRIATI_IMPORTACAO_OFX_TAMANHO_MAXIMO_BYTES) apenas como aviso leve ao
	// usuario antes do upload; o backend permanece a unica validacao real.
	var TAMANHO_MAXIMO_ADVISORIO_BYTES = 1048576;

	var loteAtual = null;
	var podeGerenciarAtual = false;

	var STATUS_LOTE = {
		PREVIA_DISPONIVEL: "Pré-visualização disponível",
		DESCARTADO: "Descartado"
	};
	var STATUS_LOTE_CLASSE = {
		PREVIA_DISPONIVEL: "ativo",
		DESCARTADO: "cancelado"
	};
	var TIPO_BANCARIO_LABEL = {
		DEBIT: "Débito",
		CREDIT: "Crédito",
		XFER: "Transferência",
		CHECK: "Cheque",
		PAYMENT: "Pagamento",
		DEP: "Depósito",
		ATM: "Caixa eletrônico",
		POS: "Compra",
		FEE: "Tarifa",
		SRVCHG: "Tarifa de serviço",
		INT: "Juros",
		DIV: "Dividendo",
		DIRECTDEP: "Depósito direto",
		DIRECTDEBIT: "Débito automático"
	};

	function el(id) {
		return document.getElementById(id);
	}

	function mensagemErro(erro, fallback) {
		return erro && erro.message && erro.message !== "Erro inesperado." ? erro.message : fallback;
	}

	function mostrarMensagem(id, mensagem, tipo) {
		var elemento = el(id);
		if (!elemento) {
			return;
		}
		elemento.textContent = mensagem;
		elemento.className = "criati-alert " + (tipo === "sucesso" ? "criati-alert-sucesso" : "criati-alert-erro");
		elemento.hidden = false;
	}

	function ocultarMensagem(id) {
		var elemento = el(id);
		if (elemento) {
			elemento.hidden = true;
		}
	}

	function celula(valor) {
		var td = document.createElement("td");
		td.textContent = valor === null || valor === undefined || valor === "" ? "—" : String(valor);
		return td;
	}

	function celulaMoeda(valor) {
		var td = celula(formatacao.moeda(valor));
		formatacao.aplicarSemantica(td, valor, "SALDO");
		return td;
	}

	function badge(valor, texto, classe) {
		var span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + classe;
		span.textContent = texto;
		return span;
	}

	function celulaBadge(valor, labels, classes) {
		var td = document.createElement("td");
		td.appendChild(badge(valor, labels[valor] || valor, classes[valor] || "cancelado"));
		return td;
	}

	function preencherSelect(select, itens, textoVazio) {
		select.textContent = "";
		var vazio = document.createElement("option");
		vazio.value = "";
		vazio.textContent = textoVazio;
		select.appendChild(vazio);
		itens.forEach(function (item) {
			var option = document.createElement("option");
			option.value = item.id;
			option.textContent = item.nome;
			select.appendChild(option);
		});
	}

	function dataHoraBr(isoDateTime) {
		return isoDateTime ? formatacao.dataBr(isoDateTime.slice(0, 10)) : "—";
	}

	// ---- Historico ----

	function linhaLote(lote) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(lote.contaNome));
		tr.appendChild(celula(lote.nomeOriginal));
		tr.appendChild(celula(dataHoraBr(lote.criadoEm)));
		tr.appendChild(celula(lote.quantidadeTransacoes));
		tr.appendChild(celula(lote.quantidadeDuplicadasArquivo));
		tr.appendChild(celula(lote.quantidadePossiveisDuplicadas));
		tr.appendChild(celulaBadge(lote.status, STATUS_LOTE, STATUS_LOTE_CLASSE));
		var acoes = document.createElement("td");
		var link = document.createElement("a");
		link.className = "criati-btn criati-btn-ghost";
		link.href = "/app/financeiro/importacoes/" + encodeURIComponent(lote.id);
		link.textContent = "Ver";
		acoes.appendChild(link);
		tr.appendChild(acoes);
		return tr;
	}

	function carregarHistorico() {
		ocultarMensagem("importacoes-mensagem");
		el("importacoes-carregando").hidden = false;
		el("importacoes-erro").hidden = true;
		el("importacoes-vazio").hidden = true;
		el("importacoes-tabela-wrap").hidden = true;
		return api.importacoesBancarias.listar().then(function (resposta) {
			el("importacoes-carregando").hidden = true;
			var lotes = resposta.data || [];
			if (!lotes.length) {
				el("importacoes-vazio").hidden = false;
				return;
			}
			var tbody = el("importacoes-tbody");
			tbody.replaceChildren();
			lotes.forEach(function (lote) {
				tbody.appendChild(linhaLote(lote));
			});
			el("importacoes-tabela-wrap").hidden = false;
		}).catch(function (erro) {
			el("importacoes-carregando").hidden = true;
			el("importacoes-erro").hidden = false;
			mostrarMensagem("importacoes-mensagem", mensagemErro(erro, "Não foi possível carregar as importações."), "erro");
		});
	}

	function iniciarHistorico() {
		if (!el("importacoes-tabela-wrap") || el("importacoes-tabela-wrap").dataset.inicializado === "true") return;
		el("importacoes-tabela-wrap").dataset.inicializado = "true";
		el("importacoes-tentar-novamente").addEventListener("click", carregarHistorico);
		carregarHistorico();
	}

	// ---- Novo upload ----

	function extensaoValida(nome) {
		return /\.ofx$/i.test(nome || "");
	}

	function iniciarFormulario() {
		if (!el("importacao-form") || el("importacao-form").dataset.inicializado === "true") return;
		el("importacao-form").dataset.inicializado = "true";
		api.contas.listar({ status: "ATIVO" }).then(function (resposta) {
			preencherSelect(el("importacao-conta"), resposta.data || [], "Selecione a conta");
		}).catch(function (erro) {
			mostrarMensagem("importacao-form-mensagem", mensagemErro(erro, "Não foi possível carregar as contas."), "erro");
		});
		el("importacao-form").addEventListener("submit", enviarUpload);
	}

	function enviarUpload(evento) {
		evento.preventDefault();
		ocultarMensagem("importacao-form-mensagem");
		var contaId = el("importacao-conta").value;
		var arquivos = el("importacao-arquivo").files;
		var arquivo = arquivos && arquivos.length ? arquivos[0] : null;
		if (!contaId) {
			mostrarMensagem("importacao-form-mensagem", "Selecione a conta financeira.", "erro");
			return;
		}
		if (!arquivo) {
			mostrarMensagem("importacao-form-mensagem", "Selecione um arquivo .ofx.", "erro");
			return;
		}
		if (!extensaoValida(arquivo.name)) {
			mostrarMensagem("importacao-form-mensagem", "O arquivo deve ter extensão .ofx.", "erro");
			return;
		}
		if (arquivo.size > TAMANHO_MAXIMO_ADVISORIO_BYTES) {
			mostrarMensagem("importacao-form-mensagem",
				"Arquivo maior que o limite padrão (1 MiB). O backend fará a validação definitiva.", "erro");
			return;
		}
		var botao = el("importacao-enviar");
		window.CriatiUI.setButtonLoading(botao, true, "Enviando...");
		api.importacoesBancarias.importarOfx(contaId, arquivo).then(function (resposta) {
			window.location.href = "/app/financeiro/importacoes/" + encodeURIComponent(resposta.data.lote.id) + "?criado=1";
		}).catch(function (erro) {
			mostrarMensagem("importacao-form-mensagem", mensagemErro(erro, "Não foi possível importar o arquivo."), "erro");
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	// ---- Detalhe / pre-visualizacao ----

	function idLoteDaRota() {
		var partes = window.location.pathname.split("/").filter(Boolean);
		return partes[partes.length - 1] || "";
	}

	function iniciarDetalhe(podeGerenciar) {
		if (!el("importacao-detalhe-titulo") || el("importacao-detalhe-titulo").dataset.inicializado === "true") return;
		el("importacao-detalhe-titulo").dataset.inicializado = "true";
		podeGerenciarAtual = Boolean(podeGerenciar);
		if (el("importacao-descartar")) {
			el("importacao-descartar").addEventListener("click", descartarLote);
		}
		var criado = new URLSearchParams(window.location.search).get("criado") === "1";
		carregarDetalhe(criado ? "Arquivo importado com sucesso. Revise a pré-visualização antes de decidir os próximos passos." : null);
	}

	function carregarDetalhe(mensagemSucesso) {
		ocultarMensagem("importacao-detalhe-mensagem");
		el("importacao-detalhe-carregando").hidden = false;
		el("importacao-detalhe-nao-encontrada").hidden = true;
		el("importacao-detalhe-conteudo").hidden = true;
		api.importacoesBancarias.buscar(idLoteDaRota()).then(function (resposta) {
			loteAtual = resposta.data;
			renderizarLote();
			renderizarTransacoes();
			el("importacao-detalhe-carregando").hidden = true;
			el("importacao-detalhe-conteudo").hidden = false;
			if (mensagemSucesso) {
				mostrarMensagem("importacao-detalhe-mensagem", mensagemSucesso, "sucesso");
			}
		}).catch(function (erro) {
			el("importacao-detalhe-carregando").hidden = true;
			if (erro && erro.status === 404) {
				el("importacao-detalhe-nao-encontrada").hidden = false;
				return;
			}
			mostrarMensagem("importacao-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar a importação."), "erro");
		});
	}

	function adicionarDetalhe(lista, rotulo, valor) {
		var dt = document.createElement("dt");
		dt.textContent = rotulo;
		var dd = document.createElement("dd");
		dd.textContent = valor === null || valor === undefined || valor === "" ? "—" : valor;
		lista.append(dt, dd);
	}

	function hashAbreviado(hash) {
		return hash && hash.length > 16 ? hash.slice(0, 16) + "…" : hash;
	}

	function renderizarLote() {
		var lote = loteAtual.lote;
		el("importacao-detalhe-titulo").textContent = "Importação · " + lote.nomeOriginal;
		var lista = el("importacao-dados");
		lista.replaceChildren();
		adicionarDetalhe(lista, "Conta", lote.contaNome);
		adicionarDetalhe(lista, "Arquivo", lote.nomeOriginal);
		adicionarDetalhe(lista, "Formato", lote.formato);
		adicionarDetalhe(lista, "Tamanho", Math.ceil(lote.tamanhoBytes / 1024) + " KB");
		adicionarDetalhe(lista, "Hash do arquivo (SHA-256)", hashAbreviado(lote.hashArquivo));
		adicionarDetalhe(lista, "Transações no arquivo", lote.quantidadeTransacoes);
		adicionarDetalhe(lista, "Duplicadas no arquivo", lote.quantidadeDuplicadasArquivo);
		adicionarDetalhe(lista, "Possíveis duplicidades", lote.quantidadePossiveisDuplicadas);
		adicionarDetalhe(lista, "Situação", STATUS_LOTE[lote.status] || lote.status);
		adicionarDetalhe(lista, "Enviado em", dataHoraBr(lote.criadoEm));
		if (lote.status === "DESCARTADO") {
			adicionarDetalhe(lista, "Descartado em", dataHoraBr(lote.descartadoEm));
		}

		var descartavel = podeGerenciarAtual && lote.status !== "DESCARTADO";
		if (el("importacao-descartar")) {
			el("importacao-descartar").hidden = !descartavel;
		}
	}

	function renderizarTransacoes() {
		var transacoes = loteAtual.transacoes || [];
		el("importacao-transacoes-vazio").hidden = Boolean(transacoes.length);
		el("importacao-transacoes-wrap").hidden = !transacoes.length;
		var tbody = el("importacao-transacoes-tbody");
		tbody.replaceChildren();
		transacoes.forEach(function (transacao) {
			var tr = document.createElement("tr");
			tr.appendChild(celula(transacao.sequencia));
			tr.appendChild(celula(formatacao.dataBr(transacao.data)));
			tr.appendChild(celula(transacao.descricao));
			tr.appendChild(celula(TIPO_BANCARIO_LABEL[transacao.tipoBancario] || transacao.tipoBancario));
			tr.appendChild(celulaMoeda(transacao.valor));
			var alertas = document.createElement("td");
			if (transacao.duplicadaNoArquivo) {
				alertas.appendChild(badge(null, "Duplicada no arquivo", "pendente"));
			}
			if (transacao.possivelmenteJaImportada) {
				alertas.appendChild(badge(null, "Possível duplicidade", "atrasada"));
			}
			if (!transacao.duplicadaNoArquivo && !transacao.possivelmenteJaImportada) {
				alertas.textContent = "—";
			}
			tr.appendChild(alertas);
			tbody.appendChild(tr);
		});
	}

	function descartarLote() {
		if (!window.confirm("Confirma o descarte deste lote de importação? As transações continuam no histórico, apenas marcadas como descartadas.")) {
			return;
		}
		var botao = el("importacao-descartar");
		window.CriatiUI.setButtonLoading(botao, true, "Descartando...");
		api.importacoesBancarias.descartar(loteAtual.lote.id).then(function () {
			carregarDetalhe("Lote descartado com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível descartar o lote."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	window.FinanceiroImportacoes = {
		iniciarHistorico: iniciarHistorico,
		iniciarFormulario: iniciarFormulario,
		iniciarDetalhe: iniciarDetalhe
	};
})(window, document);
