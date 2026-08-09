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
	var faturasEmAberto = [];
	var nomeCartaoPorId = {};
	var selecionadas = new Set();
	var OPCAO_NOVA_CATEGORIA = "__nova__";
	var selectCategoriaAlvo = null;
	var ENCAMINHAMENTO_LABEL = {
		FATURA_CARTAO: "possível pagamento de fatura de cartão",
		EMPRESTIMO_RECEBIDO: "possível recebimento de empréstimo",
		EMPRESTIMO_CONCEDIDO: "possível empréstimo concedido",
		EMPRESTIMO_RECEBIMENTO_PARCELA: "possível recebimento de parcela de empréstimo",
		EMPRESTIMO_PAGAMENTO_PARCELA: "possível pagamento de parcela de empréstimo"
	};

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
		var classe = tipo === "sucesso" ? "criati-alert-sucesso" : tipo === "aviso" ? "criati-alert-aviso" : "criati-alert-erro";
		elemento.className = "criati-alert " + classe;
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

	// Monta as opcoes de categoria de uma linha, separando Receitas e
	// Despesas em grupos visuais distintos (CRIATI-FIN-014, ponto 9), sempre
	// com a opcao "Cadastrar categoria" ao final; preserva o valor
	// selecionado quando ainda existir na lista atualizada de categoriasAtivas.
	function montarOpcoesCategoria(select, valorSelecionado) {
		select.textContent = "";
		var opcaoVazia = document.createElement("option");
		opcaoVazia.value = "";
		opcaoVazia.textContent = "Selecione a categoria";
		select.appendChild(opcaoVazia);
		[
			{ tipo: "RECEITA", rotulo: "Receitas" },
			{ tipo: "DESPESA", rotulo: "Despesas" }
		].forEach(function (grupo) {
			var itensGrupo = categoriasAtivas.filter(function (c) { return c.tipo === grupo.tipo; });
			if (!itensGrupo.length) {
				return;
			}
			var optgroup = document.createElement("optgroup");
			optgroup.label = grupo.rotulo;
			itensGrupo.forEach(function (categoria) {
				var opcao = document.createElement("option");
				opcao.value = categoria.id;
				opcao.textContent = categoria.nome;
				optgroup.appendChild(opcao);
			});
			select.appendChild(optgroup);
		});
		var opcaoNova = document.createElement("option");
		opcaoNova.value = OPCAO_NOVA_CATEGORIA;
		opcaoNova.textContent = "+ Cadastrar categoria";
		select.appendChild(opcaoNova);
		select.value = valorSelecionado && categoriasAtivas.some(function (c) { return c.id === valorSelecionado; })
			? valorSelecionado : "";
	}

	// Reconstroi as opcoes de categoria de todas as linhas pendentes apos
	// cadastrar uma categoria nova, preservando a selecao ja feita em cada
	// linha (exceto na linha de origem, que recebe a categoria recem-criada);
	// nunca recarrega a importacao inteira, para nao perder descricoes e
	// selecoes ja preenchidas pelo usuario nas demais linhas.
	function atualizarTodosSelectsCategoria(categoriaIdParaAlvo) {
		document.querySelectorAll(".importacao-transacao-categoria").forEach(function (select) {
			var valorAtual = select === selectCategoriaAlvo ? categoriaIdParaAlvo : select.value;
			montarOpcoesCategoria(select, valorAtual);
		});
	}

	function abrirCategoriaRapida(selectOrigem) {
		selectCategoriaAlvo = selectOrigem;
		el("categoria-rapida-form").reset();
		el("categoria-rapida-modal").hidden = false;
		el("categoria-rapida-nome").focus();
	}

	function fecharCategoriaRapida() {
		el("categoria-rapida-modal").hidden = true;
		if (selectCategoriaAlvo) {
			selectCategoriaAlvo.value = selectCategoriaAlvo.dataset.valorAnterior || "";
		}
		selectCategoriaAlvo = null;
	}

	function salvarCategoriaRapida(evento) {
		evento.preventDefault();
		if (!window.CriatiUI.validarObrigatorios(el("categoria-rapida-form"))) { return; }
		var botao = el("categoria-rapida-salvar");
		window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
		api.categorias.criar({ nome: el("categoria-rapida-nome").value, tipo: el("categoria-rapida-tipo").value })
			.then(function (resposta) {
				categoriasAtivas.push(resposta.data);
				var alvo = selectCategoriaAlvo;
				el("categoria-rapida-modal").hidden = true;
				el("categoria-rapida-form").reset();
				atualizarTodosSelectsCategoria(resposta.data.id);
				selectCategoriaAlvo = null;
				if (alvo) { alvo.dataset.valorAnterior = alvo.value; }
				window.CriatiUI.showToast("sucesso", "Categoria cadastrada com sucesso.");
			})
			.catch(function (erro) {
				window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível cadastrar a categoria."));
			})
			.finally(function () { window.CriatiUI.setButtonLoading(botao, false); });
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
		tr.appendChild(celula(lote.contaNome || "Pendente de definição"));
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
			preencherSelect(el("importacao-conta"), resposta.data || [], "Selecionar depois (opcional)");
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
		// Conta e sempre opcional (CRIATI-IMP-FEAT-004/FIX-007): sem ela, o
		// lote fica pendente de resolucao na tela de revisao - nunca bloqueado
		// aqui no formulario.
		var contaId = el("importacao-conta").value;
		var arquivos = el("importacao-arquivo").files;
		var arquivo = arquivos && arquivos.length ? arquivos[0] : null;
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
		if (el("importacao-conta-resolver-botao")) {
			el("importacao-conta-resolver-botao").addEventListener("click", resolverContaPendente);
		}
		if (el("importacao-selecionar-todas")) {
			el("importacao-selecionar-todas").addEventListener("change", alternarSelecionarTodas);
		}
		el("categoria-rapida-cancelar").addEventListener("click", fecharCategoriaRapida);
		el("categoria-rapida-form").addEventListener("submit", salvarCategoriaRapida);
		var criado = new URLSearchParams(window.location.search).get("criado") === "1";
		var mensagemInicial = criado
			? "Arquivo importado com sucesso. Revise a pré-visualização antes de decidir os próximos passos."
			: null;
		if (podeGerenciarAtual) {
			// Categorias e faturas em aberto so sao necessarias para quem pode
			// confirmar transacoes; usuarios somente leitura nao fazem essas
			// chamadas extras.
			Promise.all([
				api.categorias.listar({ status: "ATIVO" }),
				api.faturas.listar({ status: "ABERTA" }),
				api.cartoes.listar({ status: "ATIVO" })
			]).then(function (respostas) {
				categoriasAtivas = respostas[0].data || [];
				faturasEmAberto = respostas[1].data || [];
				nomeCartaoPorId = {};
				(respostas[2].data || []).forEach(function (cartao) { nomeCartaoPorId[cartao.id] = cartao.nome; });
			}).catch(function () {
				categoriasAtivas = categoriasAtivas.length ? categoriasAtivas : [];
				faturasEmAberto = [];
				nomeCartaoPorId = {};
				window.CriatiUI.showToast("erro", "Não foi possível carregar categorias, cartões e/ou faturas em aberto.");
			}).then(function () {
				carregarDetalhe(mensagemInicial);
			});
		} else {
			carregarDetalhe(mensagemInicial);
		}
	}

	function carregarDetalhe(mensagemSucesso, tipoMensagem) {
		ocultarMensagem("importacao-detalhe-mensagem");
		el("importacao-detalhe-carregando").hidden = false;
		el("importacao-detalhe-nao-encontrada").hidden = true;
		el("importacao-detalhe-conteudo").hidden = true;
		var id = idLoteDaRota();
		Promise.all([api.importacoesBancarias.buscar(id), api.importacoesBancarias.resumo(id)]).then(function (respostas) {
			loteAtual = respostas[0].data;
			resumoAtual = respostas[1].data;
			renderizarLote();
			renderizarContaPendente();
			renderizarResumo();
			renderizarTransacoes();
			renderizarLinksUteis();
			el("importacao-detalhe-carregando").hidden = true;
			el("importacao-detalhe-conteudo").hidden = false;
			if (mensagemSucesso) {
				mostrarMensagem("importacao-detalhe-mensagem", mensagemSucesso, tipoMensagem || "sucesso");
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

	// "Dados do lote" deixou de ser um bloco proprio (CRIATI-IMP-002, ponto 8):
	// campos so uteis para auditoria/suporte (hash SHA-256, tamanho em bytes)
	// continuam disponiveis pela API, mas nao agregam valor operacional para
	// quem esta revisando a importacao - so o resumo essencial aparece,
	// direto no cabecalho da pagina.
	function renderizarLote() {
		var lote = loteAtual.lote;
		el("importacao-detalhe-titulo").textContent = "Importação · " + lote.nomeOriginal;
		var partesMeta = [
			lote.contaId ? "Conta: " + lote.contaNome : "Conta: pendente de definição",
			"Formato " + lote.formato,
			STATUS_LOTE[lote.status] || lote.status,
			"Enviado em " + dataHoraBr(lote.criadoEm)
		];
		if (lote.status === "DESCARTADO") {
			partesMeta.push("Descartado em " + dataHoraBr(lote.descartadoEm));
		}
		if (lote.quantidadePossiveisDuplicadas > 0) {
			partesMeta.push(lote.quantidadePossiveisDuplicadas + " possível(is) duplicidade(s)");
		}
		el("importacao-detalhe-meta").textContent = partesMeta.join(" · ");

		var descartavel = podeGerenciarAtual && lote.status !== "DESCARTADO";
		if (el("importacao-descartar")) {
			el("importacao-descartar").hidden = !descartavel;
		}

		// Aviso persistente e sempre visivel (nao depende do usuario ter visto
		// o toast no momento da resolucao) - CRIATI-IMP-FIX-007, item 4: nunca
		// bloqueia, so avisa quando a conta definida diverge da sugestao OFX.
		var divergencia = el("importacao-conta-divergencia");
		if (divergencia) {
			var divergente = Boolean(lote.contaId) && Boolean(lote.contaSugeridaId) && lote.contaId !== lote.contaSugeridaId;
			divergencia.hidden = !divergente;
			if (divergente) {
				divergencia.textContent = "Atenção: esta importação foi associada à conta \"" + lote.contaNome
					+ "\", diferente da conta sugerida automaticamente pelos dados do arquivo (\"" + lote.contaSugeridaNome
					+ "\"). Confira se está correto antes de confirmar as transações.";
			}
		}
	}

	// Bloco de resolucao de conta (CRIATI-IMP-FIX-007): so aparece para quem
	// pode gerenciar, enquanto o lote nao tiver conta e nao estiver
	// descartado. Sugestao automatica (contaSugeridaId), quando existir, vem
	// pre-selecionada - o usuario sempre pode trocar por outra conta antes de
	// confirmar (selecao manual nunca e bloqueada).
	function renderizarContaPendente() {
		var secao = el("importacao-conta-pendente");
		if (!secao) {
			return;
		}
		var lote = loteAtual.lote;
		var pendente = podeGerenciarAtual && !lote.contaId && lote.status !== "DESCARTADO";
		secao.hidden = !pendente;
		if (!pendente) {
			return;
		}
		var textoSugestao = el("importacao-conta-sugestao-texto");
		if (lote.contaSugeridaId) {
			textoSugestao.textContent = "Sugestão automática pelos dados bancários do arquivo: \"" + lote.contaSugeridaNome
				+ "\". Confirme se está correta ou escolha outra conta antes de definir.";
		} else if (lote.identificacaoBancoId || lote.identificacaoAgencia || lote.identificacaoNumeroConta) {
			textoSugestao.textContent = "Não foi possível sugerir automaticamente uma conta compatível com os dados bancários deste arquivo. Selecione manualmente.";
		} else {
			textoSugestao.textContent = "Selecione a conta financeira desta importação. Nenhuma transação pode ser confirmada antes disso.";
		}
		api.contas.listar({ status: "ATIVO" }).then(function (resposta) {
			preencherSelect(el("importacao-conta-resolver"), resposta.data || [], "Selecione a conta");
			if (lote.contaSugeridaId) {
				el("importacao-conta-resolver").value = lote.contaSugeridaId;
			}
		}).catch(function () {
			window.CriatiUI.showToast("erro", "Não foi possível carregar as contas.");
		});
	}

	function resolverContaPendente() {
		var select = el("importacao-conta-resolver");
		var contaId = select ? select.value : "";
		if (!contaId) {
			window.CriatiUI.showToast("erro", "Selecione a conta financeira.");
			return;
		}
		var lote = loteAtual.lote;
		var divergente = Boolean(lote.contaSugeridaId) && contaId !== lote.contaSugeridaId;
		var botao = el("importacao-conta-resolver-botao");
		window.CriatiUI.setButtonLoading(botao, true, "Definindo...");
		api.importacoesBancarias.resolverConta(lote.id, contaId).then(function () {
			var mensagem = divergente
				? "Conta financeira definida. Atenção: você escolheu uma conta diferente da sugerida automaticamente pelo arquivo."
				: "Conta financeira definida com sucesso.";
			carregarDetalhe(mensagem, divergente ? "aviso" : "sucesso");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível definir a conta financeira."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
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
		// Sem conta resolvida o backend rejeita qualquer confirmacao
		// (ConfirmacaoImportacaoBancariaService.exigirContaResolvida) - a
		// revisao por linha so fica disponivel depois que a conta e definida
		// (ver renderizarContaPendente / CRIATI-IMP-FIX-007).
		var podeSelecionar = podeGerenciarAtual && transacao.situacao === "PENDENTE" && lote.status !== "DESCARTADO"
			&& Boolean(lote.contaId);
		var sinalizada = transacao.duplicadaNoArquivo || transacao.possivelmenteJaImportada;

		var tr = document.createElement("tr");
		tr.dataset.transacaoId = transacao.id;
		tr.dataset.sequencia = String(transacao.sequencia);
		// Regra sugerida (se houver) so e considerada "efetivamente usada" pelo
		// backend se o que for confirmado ainda bater com o que ela sugeriu
		// (ConfirmacaoImportacaoBancariaService) - o front so precisa informar
		// qual regra gerou a sugestao, nunca decidir isso sozinho.
		tr.dataset.regraSugeridaId = transacao.regraSugeridaId || "";

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
			montarOpcoesCategoria(selectCategoria, "");
			selectCategoria.addEventListener("change", function () {
				if (selectCategoria.value === OPCAO_NOVA_CATEGORIA) {
					abrirCategoriaRapida(selectCategoria);
					return;
				}
				selectCategoria.dataset.valorAnterior = selectCategoria.value;
			});
			celulaCategoria.appendChild(selectCategoria);

			// Fatura de cartao (CRIATI-IMP-002A): via alternativa a categoria -
			// quando escolhida, a transacao e confirmada como pagamento de
			// fatura (PagamentoFaturaCartaoService), nunca como lancamento
			// generico. Nunca vem pre-selecionada automaticamente, mesmo
			// quando uma regra sugere isso - so um aviso em texto; a fatura
			// especifica e sempre escolha explicita do usuario.
			var selectFatura = document.createElement("select");
			selectFatura.className = "criati-select importacao-transacao-fatura";
			selectFatura.setAttribute("aria-label", "Pagamento de fatura de cartão (alternativa à categoria) da transação " + transacao.sequencia);
			var opcaoSemFatura = document.createElement("option");
			opcaoSemFatura.value = "";
			opcaoSemFatura.textContent = "— Não é pagamento de fatura —";
			selectFatura.appendChild(opcaoSemFatura);
			faturasEmAberto.forEach(function (fatura) {
				var opcao = document.createElement("option");
				opcao.value = fatura.id;
				opcao.textContent = (nomeCartaoPorId[fatura.cartaoPrincipalId] || "Cartão") + " — "
					+ formatacao.dataBr(fatura.competencia) + " — saldo " + formatacao.moeda(fatura.saldoDevido);
				selectFatura.appendChild(opcao);
			});
			selectFatura.addEventListener("change", function () {
				selectCategoria.disabled = Boolean(selectFatura.value);
			});
			celulaCategoria.appendChild(selectFatura);

			if (transacao.aplicacaoSugestao) {
				var aviso = document.createElement("p");
				aviso.className = "financeiro-sugestao-regra";
				if (transacao.encaminhamentoSugerido) {
					aviso.textContent = "Sugestão (regra): " + (ENCAMINHAMENTO_LABEL[transacao.encaminhamentoSugerido] || transacao.encaminhamentoSugerido)
						+ " — selecione a fatura acima se aplicável.";
				} else {
					aviso.textContent = (transacao.aplicacaoSugestao === "AUTOMATICA" ? "Preenchido automaticamente" : "Sugestão")
						+ " pela regra: " + transacao.categoriaSugeridaNome;
					selectCategoria.value = transacao.categoriaSugeridaId;
					selectCategoria.dataset.valorAnterior = selectCategoria.value;
				}
				celulaCategoria.appendChild(aviso);
			}
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
			&& Boolean(loteAtual.lote.contaId) && transacoes.some(function (t) { return t.situacao === "PENDENTE"; });
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
			var selectFatura = linha.querySelector(".importacao-transacao-fatura");
			var inputDescricao = linha.querySelector(".importacao-transacao-descricao");
			var checkDuplicidade = linha.querySelector(".importacao-transacao-confirmar-duplicidade");
			var faturaId = selectFatura ? selectFatura.value : "";
			var categoriaId = (!faturaId && selectCategoria) ? selectCategoria.value : "";
			var descricaoFinal = inputDescricao ? inputDescricao.value.trim() : "";
			if (!faturaId && !categoriaId) {
				erroValidacao = "Selecione a categoria (ou a fatura, se for pagamento de cartão) da transação #" + sequencia + " antes de confirmar.";
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
				categoriaId: categoriaId || null,
				faturaId: faturaId || null,
				regraClassificacaoId: linha.dataset.regraSugeridaId || null,
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
