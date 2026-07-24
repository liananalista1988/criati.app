/* Telas operacionais de emprestimos concedidos.
   Valores, saldos, encargos, atrasos e situacoes sao sempre exibidos conforme
   retornados pelo backend; este arquivo nao executa calculos financeiros. */
(function (window, document) {
	"use strict";

	var emprestimoAtual = null;
	var parcelaAtual = null;
	var parcelasAtuais = [];
	var podeGerenciarAtual = false;

	var STATUS_EMPRESTIMO = {
		ATIVO: "Ativo",
		QUITADO: "Concluído",
		CANCELADO: "Cancelado"
	};
	var STATUS_PARCELA = {
		PENDENTE: "Pendente",
		PARCIALMENTE_PAGO: "Pagamento parcial",
		PAGO: "Quitada",
		CANCELADO: "Cancelada"
	};
	var SITUACAO_PARCELA = {
		PENDENTE: "Pendente",
		ATRASADO: "Vencida",
		PARCIALMENTE_PAGO: "Pagamento parcial",
		PAGO: "Quitada",
		CANCELADO: "Cancelada"
	};
	var FORMA_EMPRESTIMO = { UNICO: "Pagamento único", PARCELADO: "Parcelado" };
	var COBRANCA = {
		SEM_JUROS: "Sem juros",
		COM_JUROS: "Com juros",
		JUROS_MORA_ATRASO: "Juros por atraso",
		MULTA_ATRASO: "Multa por atraso",
		ALERTA_ATRASO: "Somente alerta de atraso"
	};

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

	function proximaParcela(parcelas) {
		var candidatas = parcelas.filter(function (parcela) {
			return parcela.status !== "PAGO" && parcela.status !== "CANCELADO";
		});
		candidatas.sort(function (a, b) {
			return String(a.vencimento).localeCompare(String(b.vencimento));
		});
		return candidatas[0] || null;
	}

	function agruparParcelas(parcelas) {
		var grupos = {};
		parcelas.forEach(function (parcela) {
			if (!grupos[parcela.emprestimoId]) {
				grupos[parcela.emprestimoId] = [];
			}
			grupos[parcela.emprestimoId].push(parcela);
		});
		return grupos;
	}

	function iniciarLista() {
		el("emprestimos-filtrar").addEventListener("click", carregarLista);
		el("emprestimos-tentar-novamente").addEventListener("click", carregarLista);
		carregarResumo();
		carregarLista();
		carregarAlertas();
	}

	function carregarResumo() {
		window.FinanceiroApi.parcelasEmprestimo.resumo().then(function (resposta) {
			var resumo = resposta.data;
			el("resumo-principal").textContent = window.FinanceiroFormatacao.moeda(resumo.totalPrincipal);
			el("resumo-recebido").textContent = window.FinanceiroFormatacao.moeda(resumo.totalRecebido);
			el("resumo-saldo").textContent = window.FinanceiroFormatacao.moeda(resumo.saldoAReceber);
			el("resumo-pendentes").textContent = resumo.quantidadePendente;
			el("resumo-vencidas").textContent = resumo.quantidadeVencida;
		}).catch(function (erro) {
			mostrarMensagem("emprestimos-mensagem", mensagemErro(erro, "Não foi possível carregar o resumo."), "erro");
		});
	}

	function carregarLista() {
		ocultarMensagem("emprestimos-mensagem");
		el("emprestimos-carregando").hidden = false;
		el("emprestimos-erro").hidden = true;
		el("emprestimos-vazio").hidden = true;
		el("emprestimos-tabela-wrap").hidden = true;
		var filtros = {
			status: el("emprestimos-filtro-status").value,
			busca: el("emprestimos-filtro-busca").value.trim()
		};
		Promise.all([
			window.FinanceiroApi.emprestimos.listar(filtros),
			window.FinanceiroApi.parcelasEmprestimo.listar({})
		]).then(function (respostas) {
			el("emprestimos-carregando").hidden = true;
			var emprestimos = respostas[0].data || [];
			var grupos = agruparParcelas(respostas[1].data || []);
			if (!emprestimos.length) {
				el("emprestimos-vazio").hidden = false;
				return;
			}
			var tbody = el("emprestimos-tbody");
			tbody.textContent = "";
			emprestimos.forEach(function (emprestimo) {
				tbody.appendChild(linhaEmprestimo(emprestimo, grupos[emprestimo.id] || []));
			});
			el("emprestimos-tabela-wrap").hidden = false;
		}).catch(function (erro) {
			el("emprestimos-carregando").hidden = true;
			el("emprestimos-erro").hidden = false;
			mostrarMensagem("emprestimos-mensagem", mensagemErro(erro, "Não foi possível carregar os empréstimos."), "erro");
		});
	}

	function linhaEmprestimo(emprestimo, parcelas) {
		var tr = document.createElement("tr");
		var proxima = proximaParcela(parcelas);
		tr.appendChild(celula(emprestimo.parteFinanceiraNome));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(emprestimo.valorPrincipal)));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(emprestimo.dataConcessao)));
		tr.appendChild(celula(FORMA_EMPRESTIMO[emprestimo.formaPagamento] || emprestimo.formaPagamento));
		tr.appendChild(celula("Ver parcelas"));
		tr.appendChild(celula("Ver parcelas"));
		tr.appendChild(badge(emprestimo.status, STATUS_EMPRESTIMO));
		tr.appendChild(celula(proxima ? window.FinanceiroFormatacao.dataBr(proxima.vencimento) : "—"));
		tr.appendChild(celula(proxima && proxima.atrasada ? proxima.diasEmAtraso + " dia(s)" : "Em dia"));
		var acoes = document.createElement("td");
		acoes.className = "criati-table-acoes";
		var link = document.createElement("a");
		link.className = "criati-btn criati-btn-ghost";
		link.href = "/app/financeiro/emprestimos/" + encodeURIComponent(emprestimo.id);
		link.textContent = "Detalhes";
		acoes.appendChild(link);
		tr.appendChild(acoes);
		return tr;
	}

	function carregarAlertas() {
		Promise.all([
			window.FinanceiroApi.parcelasEmprestimo.vencidas(),
			window.FinanceiroApi.parcelasEmprestimo.proximas(7)
		]).then(function (respostas) {
			renderizarAlertas("vencidas", respostas[0].data || []);
			renderizarAlertas("proximas", respostas[1].data || []);
		}).catch(function (erro) {
			el("emprestimos-vencidas-carregando").textContent = "Não foi possível carregar.";
			el("emprestimos-proximas-carregando").textContent = "Não foi possível carregar.";
			mostrarMensagem("emprestimos-mensagem", mensagemErro(erro, "Não foi possível carregar os vencimentos."), "erro");
		});
	}

	function renderizarAlertas(tipo, parcelas) {
		el("emprestimos-" + tipo + "-carregando").hidden = true;
		if (!parcelas.length) {
			el("emprestimos-" + tipo + "-vazio").hidden = false;
			return;
		}
		var lista = el("emprestimos-" + tipo + "-lista");
		lista.textContent = "";
		parcelas.forEach(function (parcela) {
			var item = document.createElement("li");
			var texto = document.createElement("span");
			texto.textContent = "Parcela " + parcela.numero + "/" + parcela.totalParcelas
				+ " · " + window.FinanceiroFormatacao.dataBr(parcela.vencimento);
			var valor = document.createElement("strong");
			valor.textContent = window.FinanceiroFormatacao.moeda(parcela.saldoPendente);
			item.appendChild(texto);
			item.appendChild(valor);
			lista.appendChild(item);
		});
		lista.hidden = false;
	}

	function iniciarFormulario() {
		el("emprestimo-data").value = hojeIso();
		el("emprestimo-forma").addEventListener("change", atualizarCamposForma);
		el("emprestimo-cobranca").addEventListener("change", atualizarCamposCobranca);
		el("emprestimo-form").addEventListener("submit", salvarEmprestimo);
		atualizarCamposForma();
		atualizarCamposCobranca();
		Promise.all([
			window.FinanceiroApi.contatos.listar({ status: "ATIVO" }),
			window.FinanceiroApi.categorias.listar({ status: "ATIVO", tipo: "RECEITA" })
		]).then(function (respostas) {
			preencherSelect(el("emprestimo-devedor"), respostas[0].data || [], "Selecione o devedor");
			preencherSelect(el("emprestimo-categoria"), respostas[1].data || [], "Selecione a categoria");
		}).catch(function (erro) {
			mostrarMensagem("emprestimo-form-mensagem", mensagemErro(erro, "Não foi possível carregar os cadastros auxiliares."), "erro");
		});
	}

	function atualizarCamposForma() {
		var parcelado = el("emprestimo-forma").value === "PARCELADO";
		el("emprestimo-parcelas").value = parcelado ? Math.max(Number(el("emprestimo-parcelas").value) || 2, 2) : 1;
		el("emprestimo-parcelas").readOnly = !parcelado;
	}

	function atualizarCamposCobranca() {
		var tipo = el("emprestimo-cobranca").value;
		var usaJuros = tipo === "COM_JUROS" || tipo === "JUROS_MORA_ATRASO";
		var usaMulta = tipo === "MULTA_ATRASO";
		el("emprestimo-juros-campo").hidden = !usaJuros;
		el("emprestimo-multa-campo").hidden = !usaMulta;
		el("emprestimo-juros").required = usaJuros;
		el("emprestimo-multa").required = usaMulta;
		if (!usaJuros) {
			el("emprestimo-juros").value = "";
		}
		if (!usaMulta) {
			el("emprestimo-multa").value = "";
		}
	}

	function salvarEmprestimo(evento) {
		evento.preventDefault();
		ocultarMensagem("emprestimo-form-mensagem");
		var button = el("emprestimo-salvar");
		var dados = {
			parteFinanceiraId: el("emprestimo-devedor").value,
			categoriaId: el("emprestimo-categoria").value,
			descricao: el("emprestimo-descricao").value.trim() || null,
			valorPrincipal: el("emprestimo-valor").value,
			dataConcessao: el("emprestimo-data").value,
			tipoCobranca: el("emprestimo-cobranca").value,
			percentualJuros: el("emprestimo-juros").value || null,
			percentualMulta: el("emprestimo-multa").value || null,
			formaPagamento: el("emprestimo-forma").value,
			quantidadeParcelas: Number(el("emprestimo-parcelas").value)
		};
		window.CriatiUI.setButtonLoading(button, true, "Cadastrando...");
		window.FinanceiroApi.emprestimos.criar(dados).then(function (resposta) {
			window.location.href = "/app/financeiro/emprestimos/" + encodeURIComponent(resposta.data.id) + "?criado=1";
		}).catch(function (erro) {
			mostrarMensagem("emprestimo-form-mensagem", mensagemErro(erro, "Não foi possível cadastrar o empréstimo."), "erro");
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	function idEmprestimoDaRota() {
		var partes = window.location.pathname.split("/").filter(Boolean);
		return partes[partes.length - 1] || "";
	}

	function iniciarDetalhe(podeGerenciar) {
		podeGerenciarAtual = Boolean(podeGerenciar);
		if (el("emprestimo-cancelar")) {
			el("emprestimo-cancelar").addEventListener("click", cancelarEmprestimo);
		}
		el("data-prometida-fechar").addEventListener("click", fecharDataPrometida);
		el("data-prometida-form").addEventListener("submit", salvarDataPrometida);
		el("recebimento-fechar").addEventListener("click", fecharRecebimento);
		el("recebimento-tipo").addEventListener("change", atualizarTipoRecebimento);
		el("recebimento-form").addEventListener("submit", salvarRecebimento);
		var criado = new URLSearchParams(window.location.search).get("criado") === "1";
		carregarDetalhe(criado ? "Empréstimo cadastrado com sucesso." : null);
	}

	function carregarDetalhe(mensagemSucesso) {
		ocultarMensagem("emprestimo-detalhe-mensagem");
		el("emprestimo-detalhe-carregando").hidden = false;
		el("emprestimo-detalhe-nao-encontrado").hidden = true;
		el("emprestimo-detalhe-conteudo").hidden = true;
		var id = idEmprestimoDaRota();
		Promise.all([
			window.FinanceiroApi.emprestimos.buscar(id),
			window.FinanceiroApi.emprestimos.parcelas(id),
			window.FinanceiroApi.contas.listar({ status: "ATIVO" })
		]).then(function (respostas) {
			emprestimoAtual = respostas[0].data;
			parcelasAtuais = respostas[1].data || [];
			renderizarEmprestimo();
			renderizarParcelas(podeGerenciarAtual);
			preencherSelect(el("recebimento-conta"), respostas[2].data || [], "Selecione a conta");
			el("emprestimo-detalhe-carregando").hidden = true;
			el("emprestimo-detalhe-conteudo").hidden = false;
			if (mensagemSucesso) {
				mostrarMensagem("emprestimo-detalhe-mensagem", mensagemSucesso, "sucesso");
			}
		}).catch(function (erro) {
			el("emprestimo-detalhe-carregando").hidden = true;
			if (erro && erro.status === 404) {
				el("emprestimo-detalhe-nao-encontrado").hidden = false;
				return;
			}
			mostrarMensagem("emprestimo-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar o empréstimo."), "erro");
		});
	}

	function renderizarEmprestimo() {
		el("emprestimo-detalhe-titulo").textContent = emprestimoAtual.parteFinanceiraNome;
		var dados = [
			["Devedor", emprestimoAtual.parteFinanceiraNome],
			["Categoria", emprestimoAtual.categoriaNome],
			["Valor principal", window.FinanceiroFormatacao.moeda(emprestimoAtual.valorPrincipal)],
			["Data da concessão", window.FinanceiroFormatacao.dataBr(emprestimoAtual.dataConcessao)],
			["Forma de pagamento", FORMA_EMPRESTIMO[emprestimoAtual.formaPagamento] || emprestimoAtual.formaPagamento],
			["Quantidade de parcelas", emprestimoAtual.quantidadeParcelas],
			["Configuração de cobrança", COBRANCA[emprestimoAtual.tipoCobranca] || emprestimoAtual.tipoCobranca],
			["Percentual de juros", emprestimoAtual.percentualJuros === null ? "Não aplicável" : emprestimoAtual.percentualJuros + "%"],
			["Percentual de multa", emprestimoAtual.percentualMulta === null ? "Não aplicável" : emprestimoAtual.percentualMulta + "%"],
			["Situação", STATUS_EMPRESTIMO[emprestimoAtual.status] || emprestimoAtual.status],
			["Descrição", emprestimoAtual.descricao || "—"]
		];
		var lista = el("emprestimo-dados");
		lista.textContent = "";
		dados.forEach(function (item) {
			var dt = document.createElement("dt");
			var dd = document.createElement("dd");
			dt.textContent = item[0];
			dd.textContent = item[1];
			lista.appendChild(dt);
			lista.appendChild(dd);
		});
		if (el("emprestimo-cancelar")) {
			el("emprestimo-cancelar").hidden = emprestimoAtual.status !== "ATIVO";
		}
	}

	function renderizarParcelas(podeGerenciar) {
		el("emprestimo-parcelas-vazio").hidden = Boolean(parcelasAtuais.length);
		el("emprestimo-parcelas-wrap").hidden = !parcelasAtuais.length;
		var tbody = el("emprestimo-parcelas-tbody");
		tbody.textContent = "";
		parcelasAtuais.forEach(function (parcela) {
			tbody.appendChild(linhaParcela(parcela, podeGerenciar));
		});
	}

	function linhaParcela(parcela, podeGerenciar) {
		var tr = document.createElement("tr");
		tr.appendChild(celula(parcela.numero + "/" + parcela.totalParcelas));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(parcela.vencimento)));
		tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(parcela.dataPrometida)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(parcela.valorPrincipal)));
		tr.appendChild(celula("Juros " + window.FinanceiroFormatacao.moeda(parcela.juros)
			+ " · Multa " + window.FinanceiroFormatacao.moeda(parcela.multa)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(parcela.valorTotal)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(parcela.valorRecebido)));
		tr.appendChild(celula(window.FinanceiroFormatacao.moeda(parcela.saldoPendente)));
		tr.appendChild(badge(parcela.situacao, SITUACAO_PARCELA));
		var acoes = document.createElement("td");
		acoes.className = "criati-table-acoes";
		acoes.appendChild(botao("Histórico", function () { carregarHistorico(parcela); }));
		if (podeGerenciar && parcela.status !== "CANCELADO") {
			acoes.appendChild(botao("Data prometida", function () { abrirDataPrometida(parcela); }));
		}
		if (podeGerenciar && parcela.status !== "PAGO" && parcela.status !== "CANCELADO") {
			acoes.appendChild(botao("Receber", function () { abrirRecebimento(parcela); }, "criati-btn criati-btn-primary"));
		}
		tr.appendChild(acoes);
		return tr;
	}

	function cancelarEmprestimo() {
		var motivo = window.prompt("Informe o motivo do cancelamento (opcional):", "");
		if (motivo === null || !window.confirm("Confirma o cancelamento deste empréstimo?")) {
			return;
		}
		var button = el("emprestimo-cancelar");
		window.CriatiUI.setButtonLoading(button, true, "Cancelando...");
		window.FinanceiroApi.emprestimos.cancelar(emprestimoAtual.id, motivo.trim() || null).then(function () {
			carregarDetalhe("Empréstimo cancelado com sucesso.");
		}).catch(function (erro) {
			mostrarMensagem("emprestimo-detalhe-mensagem", mensagemErro(erro, "Não foi possível cancelar o empréstimo."), "erro");
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	function abrirDataPrometida(parcela) {
		parcelaAtual = parcela;
		el("data-prometida").value = parcela.dataPrometida || "";
		el("data-prometida-modal").hidden = false;
	}

	function fecharDataPrometida() {
		el("data-prometida-modal").hidden = true;
		parcelaAtual = null;
	}

	function salvarDataPrometida(evento) {
		evento.preventDefault();
		var button = el("data-prometida-salvar");
		window.CriatiUI.setButtonLoading(button, true, "Salvando...");
		window.FinanceiroApi.parcelasEmprestimo.dataPrometida(parcelaAtual.id, el("data-prometida").value)
			.then(function () {
				fecharDataPrometida();
				carregarDetalhe("Data prometida atualizada.");
			}).catch(function (erro) {
				window.CriatiUI.showToast("erro",
					mensagemErro(erro, "Não foi possível atualizar a data prometida."));
			}).finally(function () {
				window.CriatiUI.setButtonLoading(button, false);
			});
	}

	function abrirRecebimento(parcela) {
		parcelaAtual = parcela;
		el("recebimento-form").reset();
		el("recebimento-tipo").value = "INTEGRAL";
		el("recebimento-data").value = hojeIso();
		el("recebimento-parcela-resumo").textContent = "Parcela " + parcela.numero + "/" + parcela.totalParcelas
			+ " · saldo informado pelo backend: " + window.FinanceiroFormatacao.moeda(parcela.saldoPendente);
		atualizarTipoRecebimento();
		el("recebimento-modal").hidden = false;
	}

	function fecharRecebimento() {
		el("recebimento-modal").hidden = true;
		parcelaAtual = null;
	}

	function atualizarTipoRecebimento() {
		var parcial = el("recebimento-tipo").value === "PARCIAL";
		el("recebimento-valor-campo").hidden = !parcial;
		el("recebimento-valor").required = parcial;
		if (!parcial) {
			el("recebimento-valor").value = "";
		}
	}

	function salvarRecebimento(evento) {
		evento.preventDefault();
		if (!window.confirm("Confirma o registro deste recebimento?")) {
			return;
		}
		var button = el("recebimento-salvar");
		var dados = {
			contaId: el("recebimento-conta").value,
			dataRecebimento: el("recebimento-data").value,
			formaPagamento: el("recebimento-forma").value || null,
			observacao: el("recebimento-observacao").value.trim() || null
		};
		var parcial = el("recebimento-tipo").value === "PARCIAL";
		if (parcial) {
			dados.valor = el("recebimento-valor").value;
		}
		window.CriatiUI.setButtonLoading(button, true, "Registrando...");
		var chamada = parcial
			? window.FinanceiroApi.parcelasEmprestimo.receberParcial(parcelaAtual.id, dados)
			: window.FinanceiroApi.parcelasEmprestimo.receberIntegral(parcelaAtual.id, dados);
		chamada.then(function () {
			fecharRecebimento();
			carregarDetalhe("Recebimento registrado com sucesso.");
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro",
				mensagemErro(erro, "Não foi possível registrar o recebimento."));
		}).finally(function () {
			window.CriatiUI.setButtonLoading(button, false);
		});
	}

	function carregarHistorico(parcela) {
		el("emprestimo-historico-secao").hidden = false;
		el("emprestimo-historico-carregando").hidden = false;
		el("emprestimo-historico-vazio").hidden = true;
		el("emprestimo-historico-wrap").hidden = true;
		el("emprestimo-historico-titulo").textContent = "Histórico da parcela " + parcela.numero;
		window.FinanceiroApi.parcelasEmprestimo.recebimentos(parcela.id).then(function (resposta) {
			el("emprestimo-historico-carregando").hidden = true;
			var recebimentos = resposta.data || [];
			if (!recebimentos.length) {
				el("emprestimo-historico-vazio").hidden = false;
				return;
			}
			var tbody = el("emprestimo-historico-tbody");
			tbody.textContent = "";
			recebimentos.forEach(function (recebimento) {
				var tr = document.createElement("tr");
				tr.appendChild(celula(window.FinanceiroFormatacao.dataBr(recebimento.dataRecebimento)));
				tr.appendChild(celula(window.FinanceiroFormatacao.moeda(recebimento.valor)));
				tr.appendChild(celula(recebimento.contaNome));
				tr.appendChild(celula(recebimento.formaPagamento));
				tr.appendChild(celula(recebimento.status === "ATIVO" ? "Ativo" : "Estornado"));
				tr.appendChild(celula(recebimento.lancamentoFinanceiroId ? "Registrado" : "—"));
				tr.appendChild(celula(recebimento.observacao));
				tbody.appendChild(tr);
			});
			el("emprestimo-historico-wrap").hidden = false;
		}).catch(function (erro) {
			el("emprestimo-historico-carregando").hidden = true;
			mostrarMensagem("emprestimo-detalhe-mensagem", mensagemErro(erro, "Não foi possível carregar os recebimentos."), "erro");
		});
	}

	window.FinanceiroEmprestimos = {
		iniciarLista: iniciarLista,
		iniciarFormulario: iniciarFormulario,
		iniciarDetalhe: iniciarDetalhe
	};
})(window, document);
