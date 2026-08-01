/* Telas operacionais de importacao bancaria OFX/CSV/XLSX
   (CRIATI-FIN-016/FIN-020/FIN-022) e confirmacao de transacoes
   (CRIATI-FIN-018). Upload (com escolha de formato), listagem,
   pre-visualizacao, descarte logico e confirmacao/ignorar transacoes - uma
   unica jornada para os tres formatos, sem paginas duplicadas. Valores,
   contadores, status, tipo financeiro (receita/despesa) e sinalizacao de
   duplicidade vem sempre do backend
   (ImportacaoBancariaService/ConfirmacaoImportacaoBancariaService); este
   arquivo nao recalcula hash, duplicidade nem nenhum valor ou natureza
   financeira, e nunca cria LancamentoFinanceiro diretamente - apenas envia
   a selecao e os dados editaveis (categoria/descricao) para os endpoints
   existentes. */
(function (window, document) {
	"use strict";

	var api = window.FinanceiroApi;
	var formatacao = window.FinanceiroFormatacao;

	// Rotulo/instrucao/extensao/limite por formato - o unico efeito real no
	// upload e qual endpoint e chamado (FinanceiroApi.importacoesBancarias.
	// importar); parser, limites e validacao continuam exclusivamente no
	// backend. tamanhoMaximoAdvisorioBytes so espelha o padrao configuravel
	// (application.properties / CRIATI_IMPORTACAO_*_TAMANHO_MAXIMO_BYTES) como
	// aviso leve antes do upload.
	var FORMATO_INFO = {
		OFX: {
			extensao: ".ofx",
			rotulo: "Arquivo OFX",
			dica: "Somente arquivos .ofx, até 1&nbsp;MiB (limite padrão; o backend é sempre a validação final).",
			tamanhoMaximoAdvisorioBytes: 1048576
		},
		CSV: {
			extensao: ".csv",
			rotulo: "Arquivo CSV",
			dica: "Somente arquivos .csv (separado por vírgula ou ponto e vírgula, UTF-8), até 1&nbsp;MiB (limite padrão; o backend é sempre a validação final).",
			tamanhoMaximoAdvisorioBytes: 1048576
		},
		XLSX: {
			extensao: ".xlsx",
			rotulo: "Arquivo XLSX",
			dica: "Somente arquivos .xlsx (não .xls), até 2&nbsp;MiB (limite padrão; o backend é sempre a validação final). Planilhas com fórmulas, abas ocultas ou macros são rejeitadas.",
			tamanhoMaximoAdvisorioBytes: 2097152
		}
	};

	var loteAtual = null;
	var resumoAtual = null;
	var podeGerenciarAtual = false;
	var categoriasAtivas = [];
	var selecionadas = new Set();

	var STATUS_LOTE = {
		PREVIA_DISPONIVEL: "Pré-visualização disponível",
		DESCARTADO: "Descartado"
	};
	var STATUS_LOTE_CLASSE = {
		PREVIA_DISPONIVEL: "ativo",
		DESCARTADO: "cancelado"
	};
	var SITUACAO_TRANSACAO = {
		PENDENTE: "Pendente",
		CONFIRMADA: "Confirmada",
		IGNORADA: "Ignorada"
	};
	var SITUACAO_TRANSACAO_CLASSE = {
		PENDENTE: "pendente",
		CONFIRMADA: "pago",
		IGNORADA: "cancelado"
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

	function celula(valor, rotulo) {
		var td = document.createElement("td");
		td.textContent = valor === null || valor === undefined || valor === "" ? "—" : String(valor);
		if (rotulo) {
			td.dataset.label = rotulo;
		}
		return td;
	}

	function celulaMoeda(valor, rotulo) {
		var td = celula(formatacao.moeda(valor), rotulo);
		formatacao.aplicarSemantica(td, valor, "SALDO");
		return td;
	}

	function badge(valor, texto, classe) {
		var span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + classe;
		span.textContent = texto;
		return span;
	}

	function celulaBadge(valor, labels, classes, rotulo) {
		var td = document.createElement("td");
		if (rotulo) {
			td.dataset.label = rotulo;
		}
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
		tr.appendChild(celula(lote.formato));
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

	function extensaoValida(nome, formato) {
		var extensao = (FORMATO_INFO[formato] || FORMATO_INFO.OFX).extensao;
		var regex = new RegExp("\\" + extensao + "$", "i");
		return regex.test(nome || "");
	}

	function formatoSelecionado() {
		var radios = document.getElementsByName("importacao-formato");
		for (var i = 0; i < radios.length; i++) {
			if (radios[i].checked) {
				return radios[i].value;
			}
		}
		return "OFX";
	}

	function atualizarFormatoSelecionado() {
		var info = FORMATO_INFO[formatoSelecionado()] || FORMATO_INFO.OFX;
		if (el("importacao-arquivo-label")) {
			el("importacao-arquivo-label").textContent = info.rotulo;
		}
		if (el("importacao-arquivo")) {
			el("importacao-arquivo").accept = info.extensao;
			// Uma selecao feita no formato anterior pode nao valer mais (ex.:
			// .ofx escolhido, depois o usuario troca para CSV); limpar evita
			// enviar um arquivo com extensao incompativel com o formato atual.
			el("importacao-arquivo").value = "";
		}
		if (el("importacao-arquivo-info")) {
			el("importacao-arquivo-info").innerHTML = info.dica;
		}
	}

	function iniciarFormulario() {
		if (!el("importacao-form") || el("importacao-form").dataset.inicializado === "true") return;
		el("importacao-form").dataset.inicializado = "true";
		api.contas.listar({ status: "ATIVO" }).then(function (resposta) {
			preencherSelect(el("importacao-conta"), resposta.data || [], "Selecione a conta");
		}).catch(function (erro) {
			mostrarMensagem("importacao-form-mensagem", mensagemErro(erro, "Não foi possível carregar as contas."), "erro");
		});
		Array.prototype.forEach.call(document.getElementsByName("importacao-formato"), function (radio) {
			radio.addEventListener("change", atualizarFormatoSelecionado);
		});
		atualizarFormatoSelecionado();
		el("importacao-form").addEventListener("submit", enviarUpload);
	}

	function enviarUpload(evento) {
		evento.preventDefault();
		ocultarMensagem("importacao-form-mensagem");
		var formato = formatoSelecionado();
		var info = FORMATO_INFO[formato] || FORMATO_INFO.OFX;
		var contaId = el("importacao-conta").value;
		var arquivos = el("importacao-arquivo").files;
		var arquivo = arquivos && arquivos.length ? arquivos[0] : null;
		if (!contaId) {
			mostrarMensagem("importacao-form-mensagem", "Selecione a conta financeira.", "erro");
			return;
		}
		if (!arquivo) {
			mostrarMensagem("importacao-form-mensagem", "Selecione um arquivo " + info.extensao + ".", "erro");
			return;
		}
		if (!extensaoValida(arquivo.name, formato)) {
			mostrarMensagem("importacao-form-mensagem", "O arquivo deve ter extensão " + info.extensao + ".", "erro");
			return;
		}
		if (arquivo.size > info.tamanhoMaximoAdvisorioBytes) {
			var limiteMiB = info.tamanhoMaximoAdvisorioBytes / 1048576;
			mostrarMensagem("importacao-form-mensagem",
				"Arquivo maior que o limite padrão (" + limiteMiB + " MiB). O backend fará a validação definitiva.", "erro");
			return;
		}
		var botao = el("importacao-enviar");
		window.CriatiUI.setButtonLoading(botao, true, "Enviando...");
		api.importacoesBancarias.importar(contaId, arquivo, formato).then(function (resposta) {
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
		if (el("importacao-confirmar-selecionadas")) {
			el("importacao-confirmar-selecionadas").addEventListener("click", confirmarSelecionadas);
		}
		if (el("importacao-selecionar-todas")) {
			el("importacao-selecionar-todas").addEventListener("change", alternarSelecionarTodas);
		}
		var criado = new URLSearchParams(window.location.search).get("criado") === "1";
		var mensagemInicial = criado
			? "Arquivo importado com sucesso. Revise a pré-visualização antes de decidir os próximos passos."
			: null;
		if (podeGerenciarAtual) {
			// Categorias so sao necessarias para quem pode confirmar transacoes;
			// usuarios somente leitura nao fazem essa chamada extra.
			api.categorias.listar({ status: "ATIVO" }).then(function (resposta) {
				categoriasAtivas = resposta.data || [];
			}).catch(function () {
				categoriasAtivas = [];
				window.CriatiUI.showToast("erro", "Não foi possível carregar as categorias financeiras.");
			}).then(function () {
				carregarDetalhe(mensagemInicial);
			});
		} else {
			carregarDetalhe(mensagemInicial);
		}
	}

	function carregarDetalhe(mensagemSucesso) {
		ocultarMensagem("importacao-detalhe-mensagem");
		el("importacao-detalhe-carregando").hidden = false;
		el("importacao-detalhe-nao-encontrada").hidden = true;
		el("importacao-detalhe-conteudo").hidden = true;
		var id = idLoteDaRota();
		Promise.all([api.importacoesBancarias.buscar(id), api.importacoesBancarias.resumo(id)]).then(function (respostas) {
			loteAtual = respostas[0].data;
			resumoAtual = respostas[1].data;
			renderizarLote();
			renderizarResumo();
			renderizarTransacoes();
			renderizarLinksUteis();
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

	function renderizarResumo() {
		var painel = el("importacao-resumo");
		if (!painel || !resumoAtual) {
			return;
		}
		painel.hidden = false;
		el("resumo-importacao-pendentes").textContent = String(resumoAtual.pendentes);
		el("resumo-importacao-confirmadas").textContent = String(resumoAtual.confirmadas);
		el("resumo-importacao-ignoradas").textContent = String(resumoAtual.ignoradas);
		el("resumo-importacao-duplicadas").textContent = String(resumoAtual.duplicadas);
	}

	function renderizarLinksUteis() {
		var secao = el("importacao-links-uteis");
		if (!secao) {
			return;
		}
		secao.hidden = !(resumoAtual && resumoAtual.confirmadas > 0);
	}

	function linhaTransacao(transacao) {
		var lote = loteAtual.lote;
		var podeSelecionar = podeGerenciarAtual && transacao.situacao === "PENDENTE" && lote.status !== "DESCARTADO";
		var sinalizada = transacao.duplicadaNoArquivo || transacao.possivelmenteJaImportada;

		var tr = document.createElement("tr");
		tr.dataset.transacaoId = transacao.id;
		tr.dataset.sequencia = String(transacao.sequencia);

		var celulaSelecao = celula(null, "Selecionar");
		celulaSelecao.textContent = "";
		if (podeSelecionar) {
			var checkSelecao = document.createElement("input");
			checkSelecao.type = "checkbox";
			checkSelecao.className = "importacao-transacao-selecionar";
			checkSelecao.setAttribute("aria-label", "Selecionar transação " + transacao.sequencia);
			checkSelecao.addEventListener("change", function () {
				if (checkSelecao.checked) {
					selecionadas.add(transacao.id);
				} else {
					selecionadas.delete(transacao.id);
				}
				atualizarBarraSelecao();
			});
			celulaSelecao.appendChild(checkSelecao);
		} else {
			celulaSelecao.textContent = "—";
		}
		tr.appendChild(celulaSelecao);

		tr.appendChild(celula(transacao.sequencia, "#"));
		tr.appendChild(celula(formatacao.dataBr(transacao.data), "Data"));
		tr.appendChild(celula(transacao.descricao, "Descrição do banco"));
		tr.appendChild(celula(TIPO_BANCARIO_LABEL[transacao.tipoBancario] || transacao.tipoBancario, "Tipo bancário"));
		tr.appendChild(celulaMoeda(transacao.valor, "Valor"));
		tr.appendChild(celulaBadge(transacao.situacao, SITUACAO_TRANSACAO, SITUACAO_TRANSACAO_CLASSE, "Situação"));

		var alertas = celula(null, "Alertas");
		alertas.textContent = "";
		if (transacao.duplicadaNoArquivo) {
			alertas.appendChild(badge(null, "Duplicada no arquivo", "pendente"));
		}
		if (transacao.possivelmenteJaImportada) {
			alertas.appendChild(badge(null, "Possível duplicidade", "atrasada"));
		}
		if (sinalizada && podeSelecionar) {
			var rotuloDuplicidade = document.createElement("label");
			rotuloDuplicidade.className = "financeiro-campo-duplicidade";
			var checkDuplicidade = document.createElement("input");
			checkDuplicidade.type = "checkbox";
			checkDuplicidade.className = "importacao-transacao-confirmar-duplicidade";
			rotuloDuplicidade.appendChild(checkDuplicidade);
			rotuloDuplicidade.appendChild(document.createTextNode(" Confirmo que não é duplicidade"));
			alertas.appendChild(rotuloDuplicidade);
		}
		if (!sinalizada) {
			alertas.textContent = "—";
		}
		tr.appendChild(alertas);

		var celulaCategoria = celula(null, "Categoria");
		celulaCategoria.textContent = "";
		if (podeSelecionar) {
			var selectCategoria = document.createElement("select");
			selectCategoria.className = "criati-select importacao-transacao-categoria";
			selectCategoria.setAttribute("aria-label", "Categoria da transação " + transacao.sequencia);
			var opcaoVazia = document.createElement("option");
			opcaoVazia.value = "";
			opcaoVazia.textContent = "Selecione a categoria";
			selectCategoria.appendChild(opcaoVazia);
			categoriasAtivas.forEach(function (categoria) {
				var opcao = document.createElement("option");
				opcao.value = categoria.id;
				opcao.textContent = categoria.nome + " — " + (categoria.tipo === "RECEITA" ? "Receita" : "Despesa");
				selectCategoria.appendChild(opcao);
			});
			celulaCategoria.appendChild(selectCategoria);
		} else {
			celulaCategoria.textContent = transacao.situacao === "CONFIRMADA" ? "Lançada" : "—";
		}
		tr.appendChild(celulaCategoria);

		var celulaDescricaoFinal = celula(null, "Descrição final");
		celulaDescricaoFinal.textContent = "";
		if (podeSelecionar) {
			var inputDescricao = document.createElement("input");
			inputDescricao.type = "text";
			inputDescricao.className = "criati-input importacao-transacao-descricao";
			inputDescricao.maxLength = 200;
			inputDescricao.value = transacao.descricao || "";
			inputDescricao.setAttribute("aria-label", "Descrição final da transação " + transacao.sequencia);
			celulaDescricaoFinal.appendChild(inputDescricao);
		} else {
			celulaDescricaoFinal.textContent = "—";
		}
		tr.appendChild(celulaDescricaoFinal);

		var celulaAcoes = celula(null, "Ações");
		celulaAcoes.textContent = "";
		celulaAcoes.className = "criati-table-acoes";
		if (podeSelecionar) {
			var botaoIgnorar = document.createElement("button");
			botaoIgnorar.type = "button";
			botaoIgnorar.className = "criati-btn criati-btn-ghost";
			botaoIgnorar.textContent = "Ignorar";
			botaoIgnorar.addEventListener("click", function () {
				ignorarTransacao(transacao);
			});
			celulaAcoes.appendChild(botaoIgnorar);
		} else {
			celulaAcoes.textContent = "—";
		}
		tr.appendChild(celulaAcoes);

		return tr;
	}

	function renderizarTransacoes() {
		var transacoes = loteAtual.transacoes || [];
		el("importacao-transacoes-vazio").hidden = Boolean(transacoes.length);
		el("importacao-transacoes-wrap").hidden = !transacoes.length;
		selecionadas.clear();
		var tbody = el("importacao-transacoes-tbody");
		tbody.replaceChildren();
		transacoes.forEach(function (transacao) {
			tbody.appendChild(linhaTransacao(transacao));
		});
		if (el("importacao-selecionar-todas")) {
			el("importacao-selecionar-todas").checked = false;
		}
		var existePendenteSelecionavel = podeGerenciarAtual && loteAtual.lote.status !== "DESCARTADO"
			&& transacoes.some(function (t) { return t.situacao === "PENDENTE"; });
		if (el("importacao-transacoes-toolbar")) {
			el("importacao-transacoes-toolbar").hidden = !existePendenteSelecionavel;
		}
		atualizarBarraSelecao();
	}

	function alternarSelecionarTodas() {
		var marcado = el("importacao-selecionar-todas").checked;
		document.querySelectorAll("#importacao-transacoes-tbody .importacao-transacao-selecionar").forEach(function (checkbox) {
			checkbox.checked = marcado;
			var linha = checkbox.closest("tr");
			var transacaoId = linha ? linha.dataset.transacaoId : null;
			if (!transacaoId) {
				return;
			}
			if (marcado) {
				selecionadas.add(transacaoId);
			} else {
				selecionadas.delete(transacaoId);
			}
		});
		atualizarBarraSelecao();
	}

	function atualizarBarraSelecao() {
		var contador = el("importacao-selecionadas-contador");
		if (contador) {
			contador.textContent = String(selecionadas.size);
		}
		var botaoConfirmar = el("importacao-confirmar-selecionadas");
		if (botaoConfirmar) {
			botaoConfirmar.disabled = selecionadas.size === 0;
		}
	}

	function linhaDaTransacao(transacaoId) {
		return document.querySelector('#importacao-transacoes-tbody tr[data-transacao-id="' + transacaoId + '"]');
	}

	// Monta os comandos a partir do que o usuario preencheu em cada linha
	// selecionada; nenhuma natureza financeira (receita/despesa) e decidida
	// aqui - isso cabe exclusivamente a ConfirmacaoImportacaoBancariaService,
	// que compara o sinal do valor com o tipo da categoria escolhida.
	function confirmarSelecionadas() {
		var comandos = [];
		var erroValidacao = null;
		selecionadas.forEach(function (transacaoId) {
			if (erroValidacao) {
				return;
			}
			var linha = linhaDaTransacao(transacaoId);
			if (!linha) {
				return;
			}
			var sequencia = linha.dataset.sequencia;
			var selectCategoria = linha.querySelector(".importacao-transacao-categoria");
			var inputDescricao = linha.querySelector(".importacao-transacao-descricao");
			var checkDuplicidade = linha.querySelector(".importacao-transacao-confirmar-duplicidade");
			var categoriaId = selectCategoria ? selectCategoria.value : "";
			var descricaoFinal = inputDescricao ? inputDescricao.value.trim() : "";
			if (!categoriaId) {
				erroValidacao = "Selecione a categoria da transação #" + sequencia + " antes de confirmar.";
				return;
			}
			if (!descricaoFinal) {
				erroValidacao = "Informe a descrição final da transação #" + sequencia + " antes de confirmar.";
				return;
			}
			if (checkDuplicidade && !checkDuplicidade.checked) {
				erroValidacao = "Confirme explicitamente a duplicidade sinalizada da transação #" + sequencia + " antes de confirmar.";
				return;
			}
			comandos.push({
				transacaoId: transacaoId,
				categoriaId: categoriaId,
				descricaoFinal: descricaoFinal,
				confirmarDuplicidade: Boolean(checkDuplicidade && checkDuplicidade.checked)
			});
		});
		if (erroValidacao) {
			window.CriatiUI.showToast("erro", erroValidacao);
			return;
		}
		if (!comandos.length) {
			return;
		}
		var mensagemConfirmacao = comandos.length === 1
			? "Confirmar 1 transação selecionada? Um lançamento financeiro correspondente será criado."
			: "Confirmar " + comandos.length + " transações selecionadas? Um lançamento financeiro será criado para cada uma.";
		if (!window.confirm(mensagemConfirmacao)) {
			return;
		}
		var botao = el("importacao-confirmar-selecionadas");
		window.CriatiUI.setButtonLoading(botao, true, "Confirmando...");
		api.importacoesBancarias.confirmar(loteAtual.lote.id, comandos).then(function () {
			carregarDetalhe(comandos.length === 1 ? "Transação confirmada com sucesso." : "Transações confirmadas com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível confirmar as transações selecionadas."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	function ignorarTransacao(transacao) {
		if (!window.confirm("Ignorar a transação #" + transacao.sequencia + "? Ela deixará de estar pendente e não poderá mais ser confirmada.")) {
			return;
		}
		api.importacoesBancarias.ignorar(loteAtual.lote.id, transacao.id).then(function () {
			carregarDetalhe("Transação ignorada com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível ignorar a transação."));
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
