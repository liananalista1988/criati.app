/* Telas operacionais de faturas de cartao (CRIATI-FIN-015).
   Valores, saldos, encargos, situacao e operacoes disponiveis vem sempre do
   backend financeiro (FaturaCartaoService/PagamentoFaturaCartaoService); este
   arquivo nao executa calculos financeiros, apenas exibe e filtra o que a
   API ja retorna. */
(function (window, document) {
	"use strict";

	var api = window.FinanceiroApi;
	var formatacao = window.FinanceiroFormatacao;

	var faturasCarregadas = [];
	var cartoesPorId = {};
	var faturaAtual = null;
	var cartaoAtual = null;
	var podeGerenciarAtual = false;

	var STATUS_FATURA = {
		ABERTA: "Aberta",
		FECHADA: "Fechada",
		PARCIALMENTE_PAGA: "Parcialmente paga",
		PAGA: "Paga",
		ATRASADA: "Atrasada"
	};
	var STATUS_FATURA_CLASSE = {
		ABERTA: "ativo",
		FECHADA: "pendente",
		PARCIALMENTE_PAGA: "pendente",
		PAGA: "pago",
		ATRASADA: "atrasada"
	};
	var STATUS_PARCELA = { ABERTA: "Aberta", CANCELADA: "Cancelada", ESTORNADA: "Estornada" };
	var STATUS_PARCELA_CLASSE = { ABERTA: "ativo", CANCELADA: "cancelada", ESTORNADA: "estornada" };
	var TIPO_PAGAMENTO = { INTEGRAL: "Integral", PARCIAL: "Parcial", MINIMO: "Mínimo" };

	// Fatura FECHADA, PARCIALMENTE_PAGA e ATRASADA sao os unicos estados
	// pagaveis (mesma regra de FaturaCartao#exigirPagavel no backend) - usado
	// para decidir quando mostrar "Registrar pagamento" e "Aplicar encargos".
	var STATUS_PAGAVEIS = ["FECHADA", "PARCIALMENTE_PAGA", "ATRASADA"];

	function el(id) {
		return document.getElementById(id);
	}

	function ocultarSeExistir(id, oculto) {
		var elemento = el(id);
		if (elemento) {
			elemento.hidden = Boolean(oculto);
		}
	}

	function hojeIso() {
		var hoje = new Date();
		var mes = String(hoje.getMonth() + 1).padStart(2, "0");
		var dia = String(hoje.getDate()).padStart(2, "0");
		return hoje.getFullYear() + "-" + mes + "-" + dia;
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
		ocultarSeExistir(id, true);
	}

	function celula(valor) {
		var td = document.createElement("td");
		td.textContent = valor === null || valor === undefined || valor === "" ? "—" : String(valor);
		return td;
	}

	function celulaMoeda(valor, natureza) {
		var td = celula(formatacao.moeda(valor));
		formatacao.aplicarSemantica(td, valor, natureza);
		return td;
	}

	function badge(valor, labels, classes) {
		var td = document.createElement("td");
		var span = document.createElement("span");
		span.className = "criati-badge criati-badge-" + (classes[valor] || "cancelado");
		span.textContent = labels[valor] || valor || "—";
		td.appendChild(span);
		return td;
	}

	function competenciaCurta(competenciaIso) {
		return formatacao.competenciaLabel(competenciaIso ? competenciaIso.slice(0, 7) : null);
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

	// ---- Lista ----

	function carregarCartoesPrincipais() {
		return api.cartoes.listar({ tipo: "FISICO" }).then(function (resposta) {
			var cartoes = resposta.data || [];
			cartoesPorId = {};
			cartoes.forEach(function (cartao) {
				cartoesPorId[cartao.id] = cartao;
			});
			preencherSelect(el("faturas-filtro-cartao"), cartoes, "Todos");
			var abrirSelect = el("fatura-abrir-cartao");
			if (abrirSelect) {
				abrirSelect.textContent = "";
				cartoes.filter(function (cartao) {
					return cartao.status === "ATIVO";
				}).forEach(function (cartao) {
					var option = document.createElement("option");
					option.value = cartao.id;
					option.textContent = cartao.nome;
					abrirSelect.appendChild(option);
				});
			}
		});
	}

	function filtrosLista() {
		return {
			cartaoPrincipalId: el("faturas-filtro-cartao").value || undefined,
			status: el("faturas-filtro-status").value || undefined
		};
	}

	function faturasVisiveis() {
		var competencia = el("faturas-filtro-competencia").value;
		if (!competencia) {
			return faturasCarregadas;
		}
		return faturasCarregadas.filter(function (fatura) {
			return fatura.competencia && fatura.competencia.slice(0, 7) === competencia;
		});
	}

	function linhaFatura(fatura) {
		var tr = document.createElement("tr");
		var cartao = cartoesPorId[fatura.cartaoPrincipalId];
		tr.appendChild(celula(cartao ? cartao.nome : "—"));
		tr.appendChild(celula(competenciaCurta(fatura.competencia)));
		tr.appendChild(celula(formatacao.dataBr(fatura.dataVencimento)));
		tr.appendChild(celula(formatacao.moeda(fatura.valorDevido)));
		tr.appendChild(celulaMoeda(fatura.valorPago, "ENTRADA"));
		tr.appendChild(celula(formatacao.moeda(fatura.saldoDevido)));
		tr.appendChild(badge(fatura.status, STATUS_FATURA, STATUS_FATURA_CLASSE));
		var acoes = document.createElement("td");
		var link = document.createElement("a");
		link.className = "criati-btn criati-btn-ghost";
		link.href = "/app/financeiro/faturas/" + encodeURIComponent(fatura.id);
		link.textContent = "Detalhes";
		acoes.appendChild(link);
		tr.appendChild(acoes);
		return tr;
	}

	function renderizarLista() {
		var itens = faturasVisiveis();
		var corpo = el("faturas-tbody");
		corpo.replaceChildren();
		itens.forEach(function (fatura) {
			corpo.appendChild(linhaFatura(fatura));
		});
		el("faturas-carregando").hidden = true;
		el("faturas-vazio").hidden = itens.length > 0;
		el("faturas-tabela-wrap").hidden = itens.length === 0;
	}

	function carregarLista() {
		ocultarMensagem("faturas-mensagem");
		el("faturas-carregando").hidden = false;
		el("faturas-erro").hidden = true;
		el("faturas-vazio").hidden = true;
		el("faturas-tabela-wrap").hidden = true;
		return api.faturas.listar(filtrosLista()).then(function (resposta) {
			faturasCarregadas = resposta.data || [];
			renderizarLista();
		}).catch(function (erro) {
			el("faturas-carregando").hidden = true;
			el("faturas-erro").hidden = false;
			mostrarMensagem("faturas-mensagem", mensagemErro(erro, "Não foi possível carregar as faturas."), "erro");
		});
	}

	function abrirModalAbrirFatura() {
		el("fatura-abrir-form").reset();
		el("fatura-abrir-modal").hidden = false;
	}

	function fecharModalAbrirFatura() {
		el("fatura-abrir-modal").hidden = true;
	}

	function salvarAberturaFatura(evento) {
		evento.preventDefault();
		var competencia = el("fatura-abrir-competencia").value;
		var dados = {
			cartaoId: el("fatura-abrir-cartao").value,
			competencia: competencia ? competencia + "-01" : null
		};
		var botao = el("fatura-abrir-salvar");
		window.CriatiUI.setButtonLoading(botao, true, "Abrindo...");
		api.faturas.abrir(dados).then(function (resposta) {
			window.location.href = "/app/financeiro/faturas/" + encodeURIComponent(resposta.data.id);
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível abrir a fatura."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	function iniciarLista(podeGerenciar) {
		if (!el("faturas-filtrar") || el("faturas-filtrar").dataset.inicializado === "true") return;
		el("faturas-filtrar").dataset.inicializado = "true";
		el("faturas-filtrar").addEventListener("click", carregarLista);
		el("faturas-tentar-novamente").addEventListener("click", carregarLista);
		if (podeGerenciar && el("faturas-abrir-botao")) {
			el("faturas-abrir-botao").addEventListener("click", abrirModalAbrirFatura);
			el("fatura-abrir-fechar").addEventListener("click", fecharModalAbrirFatura);
			el("fatura-abrir-form").addEventListener("submit", salvarAberturaFatura);
		}
		carregarCartoesPrincipais().then(carregarLista).catch(function (erro) {
			el("faturas-carregando").hidden = true;
			mostrarMensagem("faturas-mensagem", mensagemErro(erro, "Não foi possível carregar os cartões."), "erro");
		});
	}

	// ---- Detalhe ----

	function idFaturaDaRota() {
		var partes = window.location.pathname.split("/").filter(Boolean);
		return partes[partes.length - 1] || "";
	}

	function iniciarDetalhe(podeGerenciar) {
		if (!el("fatura-detalhe-titulo") || el("fatura-detalhe-titulo").dataset.inicializado === "true") return;
		el("fatura-detalhe-titulo").dataset.inicializado = "true";
		podeGerenciarAtual = Boolean(podeGerenciar);
		if (el("fatura-recompor")) {
			el("fatura-recompor").addEventListener("click", recomporFatura);
		}
		if (el("fatura-fechar")) {
			el("fatura-fechar").addEventListener("click", fecharFatura);
		}
		if (el("fatura-pagar-abrir")) {
			el("fatura-pagar-abrir").addEventListener("click", abrirModalPagamento);
			el("fatura-pagar-fechar").addEventListener("click", fecharModalPagamento);
			el("fatura-pagar-tipo").addEventListener("change", atualizarTipoPagamento);
			el("fatura-pagar-form").addEventListener("submit", salvarPagamento);
		}
		if (el("fatura-encargos-abrir")) {
			el("fatura-encargos-abrir").addEventListener("click", abrirModalEncargos);
			el("fatura-encargos-fechar").addEventListener("click", fecharModalEncargos);
			el("fatura-encargos-form").addEventListener("submit", salvarEncargos);
		}
		carregarDetalhe(null);
	}

	function carregarDetalhe(mensagemSucesso) {
		ocultarMensagem("fatura-detalhe-mensagem");
		el("fatura-detalhe-carregando").hidden = false;
		el("fatura-detalhe-nao-encontrada").hidden = true;
		el("fatura-detalhe-conteudo").hidden = true;
		var id = idFaturaDaRota();
		api.faturas.buscar(id).then(function (resposta) {
			faturaAtual = resposta.data;
			return Promise.all([
				api.cartoes.buscar(faturaAtual.cartaoPrincipalId),
				api.contas.listar({ status: "ATIVO" })
			]);
		}).then(function (resultados) {
			cartaoAtual = resultados[0].data;
			renderizarFatura();
			renderizarParcelas();
			var contaSelect = el("fatura-pagar-conta");
			if (contaSelect) {
				preencherSelect(contaSelect, resultados[1].data || [], "Selecione a conta");
			}
			el("fatura-detalhe-carregando").hidden = true;
			el("fatura-detalhe-conteudo").hidden = false;
			if (mensagemSucesso) {
				mostrarMensagem("fatura-detalhe-mensagem", mensagemSucesso, "sucesso");
			}
			carregarPagamentos();
		}).catch(function (erro) {
			el("fatura-detalhe-carregando").hidden = true;
			if (erro && erro.status === 404) {
				el("fatura-detalhe-nao-encontrada").hidden = false;
				return;
			}
			mostrarMensagem("fatura-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar a fatura."), "erro");
		});
	}

	function adicionarDetalhe(lista, rotulo, valor) {
		var dt = document.createElement("dt");
		dt.textContent = rotulo;
		var dd = document.createElement("dd");
		dd.textContent = valor === null || valor === undefined || valor === "" ? "—" : valor;
		lista.append(dt, dd);
	}

	function renderizarFatura() {
		el("fatura-detalhe-titulo").textContent = "Fatura " + (cartaoAtual ? cartaoAtual.nome : "")
			+ " · " + competenciaCurta(faturaAtual.competencia);
		var lista = el("fatura-dados");
		lista.replaceChildren();
		adicionarDetalhe(lista, "Cartão", cartaoAtual ? cartaoAtual.nome : "—");
		adicionarDetalhe(lista, "Competência", competenciaCurta(faturaAtual.competencia));
		adicionarDetalhe(lista, "Período", formatacao.dataBr(faturaAtual.periodoInicial) + " a " + formatacao.dataBr(faturaAtual.periodoFinal));
		adicionarDetalhe(lista, "Fechamento", formatacao.dataBr(faturaAtual.dataFechamento));
		adicionarDetalhe(lista, "Vencimento", formatacao.dataBr(faturaAtual.dataVencimento));
		adicionarDetalhe(lista, "Valor das parcelas", formatacao.moeda(faturaAtual.valorTotal));
		adicionarDetalhe(lista, "Saldo financiado anterior", formatacao.moeda(faturaAtual.saldoFinanciadoAnterior));
		adicionarDetalhe(lista, "Juros", formatacao.moeda(faturaAtual.juros));
		adicionarDetalhe(lista, "Multa", formatacao.moeda(faturaAtual.multa));
		adicionarDetalhe(lista, "Valor devido", formatacao.moeda(faturaAtual.valorDevido));
		adicionarDetalhe(lista, "Valor pago", formatacao.moeda(faturaAtual.valorPago));
		adicionarDetalhe(lista, "Saldo devido", formatacao.moeda(faturaAtual.saldoDevido));
		adicionarDetalhe(lista, "Situação", STATUS_FATURA[faturaAtual.status] || faturaAtual.status);
		adicionarDetalhe(lista, "Fechado em", faturaAtual.fechadoEm ? formatacao.dataBr(faturaAtual.fechadoEm.slice(0, 10)) : "—");

		var aberta = faturaAtual.status === "ABERTA";
		var pagavel = STATUS_PAGAVEIS.indexOf(faturaAtual.status) !== -1;
		ocultarSeExistir("fatura-recompor", !aberta);
		ocultarSeExistir("fatura-fechar", !aberta);
		ocultarSeExistir("fatura-pagar-abrir", !pagavel);
		ocultarSeExistir("fatura-encargos-abrir", !pagavel);
	}

	function renderizarParcelas() {
		var parcelas = faturaAtual.parcelas || [];
		el("fatura-parcelas-vazio").hidden = Boolean(parcelas.length);
		el("fatura-parcelas-wrap").hidden = !parcelas.length;
		var tbody = el("fatura-parcelas-tbody");
		tbody.replaceChildren();
		parcelas.forEach(function (parcela) {
			var tr = document.createElement("tr");
			tr.appendChild(celula(formatacao.dataBr(parcela.competencia)));
			tr.appendChild(celula(parcela.numero + "/" + parcela.totalParcelas));
			tr.appendChild(celula(formatacao.moeda(parcela.valor)));
			tr.appendChild(badge(parcela.status, STATUS_PARCELA, STATUS_PARCELA_CLASSE));
			tbody.appendChild(tr);
		});
	}

	function carregarPagamentos() {
		el("fatura-pagamentos-carregando").hidden = false;
		el("fatura-pagamentos-vazio").hidden = true;
		el("fatura-pagamentos-wrap").hidden = true;
		api.faturas.pagamentos(faturaAtual.id).then(function (resposta) {
			el("fatura-pagamentos-carregando").hidden = true;
			var pagamentos = resposta.data || [];
			if (!pagamentos.length) {
				el("fatura-pagamentos-vazio").hidden = false;
				return;
			}
			var tbody = el("fatura-pagamentos-tbody");
			tbody.replaceChildren();
			pagamentos.forEach(function (pagamento) {
				var tr = document.createElement("tr");
				tr.appendChild(celula(formatacao.dataBr(pagamento.dataPagamento)));
				tr.appendChild(celulaMoeda(pagamento.valor, "ENTRADA"));
				tr.appendChild(celula(TIPO_PAGAMENTO[pagamento.tipo] || pagamento.tipo));
				tr.appendChild(celula(pagamento.contaPagamentoNome));
				tr.appendChild(celula(pagamento.lancamentoFinanceiroId ? "Registrado" : "—"));
				tbody.appendChild(tr);
			});
			el("fatura-pagamentos-wrap").hidden = false;
		}).catch(function (erro) {
			el("fatura-pagamentos-carregando").hidden = true;
			mostrarMensagem("fatura-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar os pagamentos."), "erro");
		});
	}

	function recomporFatura() {
		var botao = el("fatura-recompor");
		window.CriatiUI.setButtonLoading(botao, true, "Recompondo...");
		api.faturas.recompor(faturaAtual.id).then(function () {
			carregarDetalhe("Fatura recomposta com as parcelas elegíveis mais recentes.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível recompor a fatura."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	function fecharFatura() {
		if (!window.confirm("Confirma o fechamento desta fatura? Esta ação não pode ser desfeita.")) {
			return;
		}
		var botao = el("fatura-fechar");
		window.CriatiUI.setButtonLoading(botao, true, "Fechando...");
		api.faturas.fechar(faturaAtual.id).then(function () {
			carregarDetalhe("Fatura fechada com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível fechar a fatura."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	function abrirModalPagamento() {
		el("fatura-pagar-form").reset();
		el("fatura-pagar-tipo").value = "INTEGRAL";
		el("fatura-pagar-data").value = hojeIso();
		el("fatura-pagar-resumo").textContent = "Saldo devido informado pelo backend: " + formatacao.moeda(faturaAtual.saldoDevido);
		atualizarTipoPagamento();
		el("fatura-pagar-modal").hidden = false;
	}

	function fecharModalPagamento() {
		el("fatura-pagar-modal").hidden = true;
	}

	function atualizarTipoPagamento() {
		var precisaValor = el("fatura-pagar-tipo").value !== "INTEGRAL";
		el("fatura-pagar-valor-campo").hidden = !precisaValor;
		el("fatura-pagar-valor").required = precisaValor;
		if (!precisaValor) {
			el("fatura-pagar-valor").value = "";
		}
	}

	function salvarPagamento(evento) {
		evento.preventDefault();
		if (!window.confirm("Confirma o registro deste pagamento?")) {
			return;
		}
		var tipo = el("fatura-pagar-tipo").value;
		var dados = {
			contaPagamentoId: el("fatura-pagar-conta").value,
			dataPagamento: el("fatura-pagar-data").value,
			tipo: tipo,
			formaPagamento: el("fatura-pagar-forma").value || null
		};
		if (tipo !== "INTEGRAL") {
			dados.valor = el("fatura-pagar-valor").value;
		}
		var botao = el("fatura-pagar-salvar");
		window.CriatiUI.setButtonLoading(botao, true, "Registrando...");
		api.faturas.registrarPagamento(faturaAtual.id, dados).then(function () {
			fecharModalPagamento();
			carregarDetalhe("Pagamento registrado com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível registrar o pagamento."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	function abrirModalEncargos() {
		el("fatura-encargos-form").reset();
		el("fatura-encargos-juros").value = faturaAtual.juros || "";
		el("fatura-encargos-multa").value = faturaAtual.multa || "";
		el("fatura-encargos-modal").hidden = false;
	}

	function fecharModalEncargos() {
		el("fatura-encargos-modal").hidden = true;
	}

	function salvarEncargos(evento) {
		evento.preventDefault();
		var dados = {
			juros: el("fatura-encargos-juros").value || null,
			multa: el("fatura-encargos-multa").value || null
		};
		var botao = el("fatura-encargos-salvar");
		window.CriatiUI.setButtonLoading(botao, true, "Aplicando...");
		api.faturas.aplicarEncargos(faturaAtual.id, dados).then(function () {
			fecharModalEncargos();
			carregarDetalhe("Encargos atualizados com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", mensagemErro(erro, "Não foi possível aplicar os encargos."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(botao, false);
		});
	}

	window.FinanceiroFaturasCartao = {
		iniciarLista: iniciarLista,
		iniciarDetalhe: iniciarDetalhe
	};
})(window, document);
