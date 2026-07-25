/* Telas operacionais de compras para terceiros e seus ressarcimentos.
   Valores, saldos e situacoes sao sempre exibidos conforme retornados pelo
   backend; este arquivo nao executa calculos financeiros (nunca soma ou
   subtrai valores monetarios em JavaScript). Ressarcimento nunca gera
   receita, despesa, categoria financeira ou LancamentoFinanceiro — apenas
   movimenta o saldo da conta selecionada (CRIATI-FIN-013A). */
(function (window, document) {
	"use strict";

	var compraAtual = null;
	var valorAtual = null;
	var valoresAtuais = [];
	var podeGerenciarAtual = false;

	var STATUS_COMPRA = { ATIVA: "Ativa", CANCELADA: "Cancelada", ESTORNADA: "Estornada" };
	var SITUACAO_VALOR = {
		PENDENTE: "Pendente",
		PARCIALMENTE_RESSARCIDA: "Ressarcimento parcial",
		ATRASADA: "Atrasada",
		RESSARCIDA: "Ressarcida",
		CANCELADA: "Cancelada"
	};
	var STATUS_RESSARCIMENTO = { ATIVO: "Ativo", ESTORNADO: "Estornado" };

	function el(id) {
		return document.getElementById(id);
	}

	function hojeIso() {
		var hoje = new Date();
		var mes = String(hoje.getMonth() + 1).padStart(2, "0");
		var dia = String(hoje.getDate()).padStart(2, "0");
		return hoje.getFullYear() + "-" + mes + "-" + dia;
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

	function mensagemErro(erro, fallback) {
		return erro && erro.message && erro.message !== "Erro inesperado." ? erro.message : fallback;
	}

	function celula(valor) {
		var td = document.createElement("td");
		td.textContent = valor === null || valor === undefined || valor === "" ? "—" : String(valor);
		return td;
	}

	function badge(valor, labels) {
		var td = document.createElement("td");
		var span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + String(valor || "").toLowerCase().replaceAll("_", "-");
		span.textContent = labels[valor] || valor || "—";
		td.appendChild(span);
		return td;
	}

	function botao(texto, acao, classe) {
		var button = document.createElement("button");
		button.type = "button";
		button.className = classe || "criati-btn criati-btn-ghost";
		button.textContent = texto;
		button.addEventListener("click", acao);
		return button;
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

	/* ==================== LISTA ==================== */

	function iniciarLista() {
		el("compras-terceiros-filtrar").addEventListener("click", carregarLista);
		el("compras-terceiros-tentar-novamente").addEventListener("click", carregarLista);
		window.FinanceiroApi.contatos.listar({ status: "ATIVO" }).then(function (resposta) {
			preencherSelect(el("compras-terceiros-filtro-parte"), resposta.data || [], "Todos");
		}).catch(function () {
			// filtro por terceiro e apenas conveniencia; a listagem funciona mesmo sem ele.
		});
		carregarResumo();
		carregarLista();
	}

	function carregarResumo() {
		window.FinanceiroApi.valoresAReceberCartao.resumo().then(function (resposta) {
			var resumo = resposta.data;
			el("resumo-principal").textContent = window.FinanceiroFormatacao.moeda(resumo.totalPrincipal);
			el("resumo-ressarcido").textContent = window.FinanceiroFormatacao.moeda(resumo.totalRessarcido);
			el("resumo-saldo").textContent = window.FinanceiroFormatacao.moeda(resumo.saldoAReceber);
			el("resumo-pendentes").textContent = resumo.quantidadePendente;
			el("resumo-vencidas").textContent = resumo.quantidadeVencida;
		}).catch(function (erro) {
			mostrarMensagem("compras-terceiros-mensagem", mensagemErro(erro, "Não foi possível carregar o resumo."), "erro");
		});
	}

	function carregarLista() {
		ocultarMensagem("compras-terceiros-mensagem");
		el("compras-terceiros-carregando").hidden = false;
		el("compras-terceiros-erro").hidden = true;
		el("compras-terceiros-vazio").hidden = true;
		el("compras-terceiros-tabela-wrap").hidden = true;
		var filtros = {
			parteId: el("compras-terceiros-filtro-parte").value,
			busca: el("compras-terceiros-filtro-busca").value.trim()
		};
		window.FinanceiroApi.comprasTerceiros.listar(filtros).then(function (resposta) {
			el("compras-terceiros-carregando").hidden = true;
			var compras = resposta.data || [];
			if (!compras.length) {
				el("compras-terceiros-vazio").hidden = false;
				return;
			}
			var tbody = el("compras-terceiros-tbody");
			tbody.textContent = "";
			compras.forEach(function (compra) {
				tbody.appendChild(linhaCompra(compra));
			});
			el("compras-terceiros-tabela-wrap").hidden = false;
		}).catch(function (erro) {
			el("compras-terceiros-carregando").hidden = true;
			el("compras-terceiros-erro").hidden = false;
			mostrarMensagem("compras-terceiros-mensagem",
				mensagemErro(erro, "Não foi possível carregar as compras para terceiros."), "erro");
		});
	}

	function linhaCompra(compra) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(compra.parteFinanceiraNome));
		tr.appendChild(celula(compra.descricao));
		tr.appendChild(celula(compra.cartaoNome));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(compra.valorTotal)));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(compra.dataCompra)));
		tr.appendChild(celula(String(compra.quantidadeParcelas)));
		tr.appendChild(badge(compra.status, STATUS_COMPRA));
		var acoes = document.createElement("td");
		acoes.className = "criati-table-acoes";
		var link = document.createElement("a");
		link.className = "criati-btn criati-btn-ghost";
		link.href = "/app/financeiro/compras-terceiros/" + encodeURIComponent(compra.id);
		link.textContent = "Detalhes";
		acoes.appendChild(link);
		tr.appendChild(acoes);
		return tr;
	}

	/* ==================== FORMULARIO ==================== */

	function iniciarFormulario() {
		el("compra-terceiro-data").value = hojeIso();
		el("compra-terceiro-cartao").addEventListener("change", mostrarLimiteAtual);
		el("compra-terceiro-form").addEventListener("submit", salvarCompra);
		var cartoesCarregados = [];
		Promise.all([
			window.FinanceiroApi.cartoes.listar({ status: "ATIVO" }),
			window.FinanceiroApi.pessoas.listar({ status: "ATIVO" }),
			window.FinanceiroApi.categorias.listar({ status: "ATIVO", tipo: "DESPESA" }),
			window.FinanceiroApi.contatos.listar({ status: "ATIVO" })
		]).then(function (respostas) {
			cartoesCarregados = respostas[0].data || [];
			preencherSelect(el("compra-terceiro-cartao"), cartoesCarregados, "Selecione o cartão");
			preencherSelect(el("compra-terceiro-pessoa"), respostas[1].data || [], "Selecione o responsável");
			preencherSelect(el("compra-terceiro-categoria"), respostas[2].data || [], "Selecione a categoria");
			preencherSelect(el("compra-terceiro-parte"), respostas[3].data || [], "Selecione o terceiro");
		}).catch(function (erro) {
			mostrarMensagem("compra-terceiro-form-mensagem",
				mensagemErro(erro, "Não foi possível carregar os cadastros auxiliares."), "erro");
		});

		function mostrarLimiteAtual() {
			var cartao = cartoesCarregados.find(function (item) {
				return item.id === el("compra-terceiro-cartao").value;
			});
			el("compra-terceiro-impacto-limite").textContent = cartao
				? "Limite informado pelo backend: " + window.FinanceiroFormatacao.moeda(cartao.limiteTotal)
					+ " · comprometido: " + window.FinanceiroFormatacao.moeda(cartao.limiteComprometido)
					+ " · disponível: " + window.FinanceiroFormatacao.moeda(cartao.limiteDisponivel)
				: "";
		}
	}

	function salvarCompra(evento) {
		evento.preventDefault();
		ocultarMensagem("compra-terceiro-form-mensagem");
		var button = el("compra-terceiro-salvar");
		var dados = {
			cartaoId: el("compra-terceiro-cartao").value,
			pessoaResponsavelId: el("compra-terceiro-pessoa").value,
			categoriaId: el("compra-terceiro-categoria").value,
			parteFinanceiraId: el("compra-terceiro-parte").value,
			descricao: el("compra-terceiro-descricao").value.trim(),
			dataCompra: el("compra-terceiro-data").value,
			valorTotal: el("compra-terceiro-valor").value,
			quantidadeParcelas: Number(el("compra-terceiro-parcelas").value),
			observacao: el("compra-terceiro-observacao").value.trim() || null
		};
		window.CriatiUI.setButtonLoading(button, true, "Registrando...");
		window.FinanceiroApi.comprasTerceiros.criar(dados).then(function (resposta) {
			window.location.href = "/app/financeiro/compras-terceiros/"
				+ encodeURIComponent(resposta.data.compra.id) + "?criado=1";
		}).catch(function (erro) {
			mostrarMensagem("compra-terceiro-form-mensagem",
				mensagemErro(erro, "Não foi possível registrar a compra."), "erro");
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	/* ==================== DETALHE ==================== */

	function idCompraDaRota() {
		var partes = window.location.pathname.split("/").filter(Boolean);
		return partes[partes.length - 1] || "";
	}

	function iniciarDetalhe(podeGerenciar) {
		podeGerenciarAtual = Boolean(podeGerenciar);
		if (el("compra-terceiro-cancelar")) {
			el("compra-terceiro-cancelar").addEventListener("click", cancelarCompra);
		}
		el("ct-data-prometida-fechar").addEventListener("click", fecharDataPrometida);
		el("ct-data-prometida-form").addEventListener("submit", salvarDataPrometida);
		el("ct-ressarcimento-fechar").addEventListener("click", fecharRessarcimento);
		el("ct-ressarcimento-tipo").addEventListener("change", atualizarTipoRessarcimento);
		el("ct-ressarcimento-form").addEventListener("submit", salvarRessarcimento);
		var criado = new URLSearchParams(window.location.search).get("criado") === "1";
		carregarDetalhe(criado ? "Compra cadastrada com sucesso." : null);
	}

	function carregarDetalhe(mensagemSucesso) {
		ocultarMensagem("compra-terceiro-detalhe-mensagem");
		el("compra-terceiro-detalhe-carregando").hidden = false;
		el("compra-terceiro-detalhe-nao-encontrado").hidden = true;
		el("compra-terceiro-detalhe-conteudo").hidden = true;
		var id = idCompraDaRota();
		Promise.all([
			window.FinanceiroApi.comprasTerceiros.buscar(id),
			window.FinanceiroApi.contas.listar({ status: "ATIVO" })
		]).then(function (respostas) {
			compraAtual = respostas[0].data.compra;
			valoresAtuais = respostas[0].data.valoresAReceber || [];
			renderizarCompra();
			renderizarValores(podeGerenciarAtual);
			preencherSelect(el("ct-ressarcimento-conta"), respostas[1].data || [], "Selecione a conta");
			el("compra-terceiro-detalhe-carregando").hidden = true;
			el("compra-terceiro-detalhe-conteudo").hidden = false;
			el("compra-terceiro-historico-secao").hidden = true;
			if (mensagemSucesso) {
				mostrarMensagem("compra-terceiro-detalhe-mensagem", mensagemSucesso, "sucesso");
			}
		}).catch(function (erro) {
			el("compra-terceiro-detalhe-carregando").hidden = true;
			if (erro && erro.status === 400 || erro && erro.status === 404) {
				el("compra-terceiro-detalhe-nao-encontrado").hidden = false;
				return;
			}
			mostrarMensagem("compra-terceiro-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar a compra."), "erro");
		});
	}

	function renderizarCompra() {
		el("compra-terceiro-detalhe-titulo").textContent = compraAtual.descricao;
		var dados = [
			["Terceiro a ressarcir", compraAtual.parteFinanceiraNome],
			["Responsável pela compra", compraAtual.pessoaResponsavelNome],
			["Categoria", compraAtual.categoriaNome],
			["Cartão", compraAtual.cartaoNome],
			["Data da compra", window.FinanceiroFormatacao.dataBr(compraAtual.dataCompra)],
			["Valor total", window.FinanceiroFormatacao.moeda(compraAtual.valorTotal)],
			["Quantidade de parcelas", compraAtual.quantidadeParcelas],
			["Status", STATUS_COMPRA[compraAtual.status] || compraAtual.status],
			["Observação", compraAtual.observacao || "—"],
			["Motivo do cancelamento", compraAtual.motivoCancelamento || "—"]
		];
		var lista = el("compra-terceiro-dados");
		lista.textContent = "";
		dados.forEach(function (item) {
			var dt = document.createElement("dt");
			var dd = document.createElement("dd");
			dt.textContent = item[0];
			dd.textContent = item[1];
			lista.appendChild(dt);
			lista.appendChild(dd);
		});
		if (el("compra-terceiro-cancelar")) {
			el("compra-terceiro-cancelar").hidden = compraAtual.status !== "ATIVA";
		}
	}

	function renderizarValores(podeGerenciar) {
		el("compra-terceiro-valores-vazio").hidden = Boolean(valoresAtuais.length);
		el("compra-terceiro-valores-wrap").hidden = !valoresAtuais.length;
		var tbody = el("compra-terceiro-valores-tbody");
		tbody.textContent = "";
		valoresAtuais.forEach(function (valor) {
			tbody.appendChild(linhaValor(valor, podeGerenciar));
		});
	}

	function linhaValor(valor, podeGerenciar) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(valor.numero + "/" + valor.totalParcelas));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(valor.vencimento)));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(valor.dataPrometida)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(valor.valorTotal)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(valor.valorRecebido)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(valor.saldoPendente)));
		tr.appendChild(badge(valor.situacao, SITUACAO_VALOR));
		var acoes = document.createElement("td");
		acoes.className = "criati-table-acoes";
		acoes.appendChild(botao("Histórico", function () { carregarHistorico(valor); }));
		if (podeGerenciar && valor.status !== "CANCELADA") {
			acoes.appendChild(botao("Data prometida", function () { abrirDataPrometida(valor); }));
		}
		if (podeGerenciar && valor.status !== "RESSARCIDA" && valor.status !== "CANCELADA") {
			acoes.appendChild(botao("Receber", function () { abrirRessarcimento(valor); }, "criati-btn criati-btn-primary"));
		}
		tr.appendChild(acoes);
		return tr;
	}

	function cancelarCompra() {
		var motivo = window.prompt("Informe o motivo do cancelamento:", "");
		if (motivo === null) {
			return;
		}
		if (!motivo.trim()) {
			window.CriatiUI.showToast("erro", "Informe um motivo para cancelar a compra.");
			return;
		}
		if (!window.confirm("Confirma o cancelamento desta compra? Valores já ressarcidos são preservados.")) {
			return;
		}
		var button = el("compra-terceiro-cancelar");
		window.CriatiUI.setButtonLoading(button, true, "Cancelando...");
		window.FinanceiroApi.comprasTerceiros.cancelar(compraAtual.id, motivo.trim()).then(function () {
			carregarDetalhe("Compra cancelada com sucesso.");
		}).catch(function (erro) {
			mostrarMensagem("compra-terceiro-detalhe-mensagem", mensagemErro(erro, "Não foi possível cancelar a compra."), "erro");
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	function abrirDataPrometida(valor) {
		valorAtual = valor;
		el("ct-data-prometida").value = valor.dataPrometida || "";
		el("ct-data-prometida-modal").hidden = false;
	}

	function fecharDataPrometida() {
		el("ct-data-prometida-modal").hidden = true;
		valorAtual = null;
	}

	function salvarDataPrometida(evento) {
		evento.preventDefault();
		var button = el("ct-data-prometida-salvar");
		window.CriatiUI.setButtonLoading(button, true, "Salvando...");
		window.FinanceiroApi.valoresAReceberCartao.dataPrometida(valorAtual.id, el("ct-data-prometida").value)
			.then(function () {
				fecharDataPrometida();
				carregarDetalhe("Data prometida atualizada.");
			}).catch(function (erro) {
				window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível atualizar a data prometida."));
			}).finally(function () {
				window.CriatiUI.setButtonLoading(button, false);
			});
	}

	function abrirRessarcimento(valor) {
		valorAtual = valor;
		el("ct-ressarcimento-form").reset();
		el("ct-ressarcimento-tipo").value = "INTEGRAL";
		el("ct-ressarcimento-data").value = hojeIso();
		el("ct-ressarcimento-valor-resumo").textContent = "Parcela " + valor.numero + "/" + valor.totalParcelas
			+ " · saldo pendente informado pelo backend: " + window.FinanceiroFormatacao.moeda(valor.saldoPendente);
		atualizarTipoRessarcimento();
		el("ct-ressarcimento-modal").hidden = false;
	}

	function fecharRessarcimento() {
		el("ct-ressarcimento-modal").hidden = true;
		valorAtual = null;
	}

	function atualizarTipoRessarcimento() {
		var parcial = el("ct-ressarcimento-tipo").value === "PARCIAL";
		el("ct-ressarcimento-valor-campo").hidden = !parcial;
		el("ct-ressarcimento-valor").required = parcial;
		if (!parcial) {
			el("ct-ressarcimento-valor").value = "";
		}
	}

	function salvarRessarcimento(evento) {
		evento.preventDefault();
		if (!window.confirm("Confirma o registro deste ressarcimento?")) {
			return;
		}
		var button = el("ct-ressarcimento-salvar");
		var dados = {
			contaId: el("ct-ressarcimento-conta").value,
			dataRessarcimento: el("ct-ressarcimento-data").value,
			formaPagamento: el("ct-ressarcimento-forma").value || null,
			observacao: el("ct-ressarcimento-observacao").value.trim() || null
		};
		var parcial = el("ct-ressarcimento-tipo").value === "PARCIAL";
		if (parcial) {
			dados.valor = el("ct-ressarcimento-valor").value;
		}
		window.CriatiUI.setButtonLoading(button, true, "Registrando...");
		var chamada = parcial
			? window.FinanceiroApi.valoresAReceberCartao.receberParcial(valorAtual.id, dados)
			: window.FinanceiroApi.valoresAReceberCartao.receberIntegral(valorAtual.id, dados);
		chamada.then(function () {
			fecharRessarcimento();
			carregarDetalhe("Ressarcimento registrado com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível registrar o ressarcimento."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	function carregarHistorico(valor) {
		el("compra-terceiro-historico-secao").hidden = false;
		el("compra-terceiro-historico-carregando").hidden = false;
		el("compra-terceiro-historico-vazio").hidden = true;
		el("compra-terceiro-historico-wrap").hidden = true;
		el("compra-terceiro-historico-titulo").textContent = "Histórico da parcela " + valor.numero;
		window.FinanceiroApi.valoresAReceberCartao.ressarcimentos(valor.id).then(function (resposta) {
			el("compra-terceiro-historico-carregando").hidden = true;
			var ressarcimentos = resposta.data || [];
			if (!ressarcimentos.length) {
				el("compra-terceiro-historico-vazio").hidden = false;
				return;
			}
			var tbody = el("compra-terceiro-historico-tbody");
			tbody.textContent = "";
			ressarcimentos.forEach(function (ressarcimento) {
				tbody.appendChild(linhaRessarcimento(ressarcimento, valor));
			});
			el("compra-terceiro-historico-wrap").hidden = false;
		}).catch(function (erro) {
			el("compra-terceiro-historico-carregando").hidden = true;
			mostrarMensagem("compra-terceiro-detalhe-mensagem",
				mensagemErro(erro, "Não foi possível carregar os ressarcimentos."), "erro");
		});
	}

	function linhaRessarcimento(ressarcimento, valor) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(ressarcimento.dataRessarcimento)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(ressarcimento.valor)));
		tr.appendChild(celula(ressarcimento.contaNome));
		tr.appendChild(celula(ressarcimento.formaPagamento));
		tr.appendChild(badge(ressarcimento.status, STATUS_RESSARCIMENTO));
		tr.appendChild(celula(ressarcimento.observacao));
		var acoes = document.createElement("td");
		acoes.className = "criati-table-acoes";
		if (podeGerenciarAtual && ressarcimento.status === "ATIVO") {
			acoes.appendChild(botao("Estornar", function () { estornarRessarcimento(ressarcimento, valor); }));
		}
		tr.appendChild(acoes);
		return tr;
	}

	function estornarRessarcimento(ressarcimento, valor) {
		var motivo = window.prompt("Informe o motivo do estorno (opcional):", "");
		if (motivo === null) {
			return;
		}
		if (!window.confirm("Confirma o estorno deste ressarcimento? O saldo pendente volta a ser cobrado.")) {
			return;
		}
		window.FinanceiroApi.valoresAReceberCartao.estornar(valor.id, ressarcimento.id, motivo.trim() || null)
			.then(function () {
				carregarHistorico(valor);
				carregarDetalhe("Ressarcimento estornado com sucesso.");
			}).catch(function (erro) {
				window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível estornar o ressarcimento."));
			});
	}

	window.FinanceiroComprasTerceiros = {
		iniciarLista: iniciarLista,
		iniciarFormulario: iniciarFormulario,
		iniciarDetalhe: iniciarDetalhe
	};
})(window, document);
