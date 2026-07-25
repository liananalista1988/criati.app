(function () {
	"use strict";

	var api = window.CriatiApi;
	var formatacao = window.FinanceiroFormatacao;
	var cartoes = [];

	function el(id) {
		return document.getElementById(id);
	}

	function td(texto) {
		var celula = document.createElement("td");
		celula.textContent = texto == null || texto === "" ? "—" : texto;
		return celula;
	}

	function option(valor, texto) {
		var item = document.createElement("option");
		item.value = valor;
		item.textContent = texto;
		return item;
	}

	function preencherSelect(id, itens, rotuloInicial) {
		var campo = el(id);
		campo.replaceChildren(option("", rotuloInicial || "Selecione"));
		itens.forEach(function (item) {
			campo.appendChild(option(item.id, item.nome));
		});
	}

	function statusLabel(status) {
		return {
			ATIVA: "Ativa",
			CANCELADA: "Cancelada",
			ESTORNADA: "Estornada"
		}[status] || status;
	}

	function mensagem(texto, erro) {
		var aviso = el("compras-mensagem");
		aviso.hidden = false;
		aviso.textContent = texto;
		aviso.className = "criati-alert " + (erro ? "criati-alert-error" : "criati-alert-success");
	}

	function carregarCadastros() {
		return Promise.all([
			api.get("/api/contexto/financeiro/cartoes?status=ATIVO"),
			api.get("/api/contexto/financeiro/pessoas?status=ATIVO"),
			api.get("/api/contexto/financeiro/categorias?status=ATIVO&tipo=DESPESA")
		]).then(function (resultados) {
			cartoes = resultados[0].data || [];
			preencherSelect("compra-cartao", cartoes, "Selecione");
			preencherSelect("compra-pessoa", resultados[1].data || [], "Selecione");
			preencherSelect("compra-categoria", resultados[2].data || [], "Selecione");
		});
	}

	function mostrarLimiteAtual() {
		var cartao = cartoes.find(function (item) {
			return item.id === el("compra-cartao").value;
		});
		el("impacto-limite").textContent = cartao
			? "Limite informado pelo backend: " + formatacao.moeda(cartao.limiteTotal)
				+ " · comprometido: " + formatacao.moeda(cartao.limiteComprometido)
				+ " · disponível: " + formatacao.moeda(cartao.limiteDisponivel)
			: "";
	}

	function filtros() {
		var query = new URLSearchParams();
		if (el("filtro-busca").value) query.set("busca", el("filtro-busca").value);
		if (el("filtro-status").value) query.set("status", el("filtro-status").value);
		if (el("filtro-competencia").value) query.set("competencia", el("filtro-competencia").value + "-01");
		return query.toString();
	}

	function botao(texto, acao) {
		var item = document.createElement("button");
		item.type = "button";
		item.className = "criati-btn criati-btn-ghost";
		item.textContent = texto;
		item.addEventListener("click", acao);
		return item;
	}

	function renderizarCompras(compras) {
		var corpo = el("compras-tbody");
		corpo.replaceChildren();
		compras.forEach(function (compra) {
			var linha = document.createElement("tr");
			linha.appendChild(td(formatacao.dataBr(compra.dataCompra)));
			linha.appendChild(td(compra.descricao));
			linha.appendChild(td(compra.cartaoNome));
			linha.appendChild(td(compra.pessoaResponsavelNome));
			linha.appendChild(td(compra.parteFinanceiraNome ? "Terceiro: " + compra.parteFinanceiraNome : "Residência"));
			linha.appendChild(td(formatacao.moeda(compra.valorTotal)));
			linha.appendChild(td(String(compra.quantidadeParcelas)));
			linha.appendChild(td(statusLabel(compra.status)));
			var acoes = document.createElement("td");
			acoes.className = "criati-table-acoes";
			acoes.appendChild(botao("Detalhes", function () { abrirDetalhes(compra); }));
			if (compra.status === "ATIVA") {
				acoes.appendChild(botao("Cancelar", function () { alterarStatus(compra, "cancelar"); }));
				acoes.appendChild(botao("Estornar", function () { alterarStatus(compra, "estornar"); }));
			}
			linha.appendChild(acoes);
			corpo.appendChild(linha);
		});
		el("compras-carregando").hidden = true;
		el("compras-vazio").hidden = compras.length > 0;
		el("compras-tabela-wrap").hidden = compras.length === 0;
	}

	function listar() {
		el("compras-carregando").hidden = false;
		var query = filtros();
		return api.get("/api/contexto/financeiro/compras-cartao" + (query ? "?" + query : ""))
			.then(function (resposta) {
				renderizarCompras(resposta.data || []);
				return atualizarResumo();
			})
			.catch(function (erro) {
				el("compras-carregando").hidden = true;
				mensagem(erro.message || "Não foi possível carregar as compras.", true);
			});
	}

	function atualizarResumo() {
		return api.get("/api/contexto/financeiro/compras-cartao/resumo").then(function (resposta) {
			var resumo = resposta.data || {};
			el("resumo-total").textContent = formatacao.moeda(resumo.totalComprado);
			el("resumo-limite").textContent = formatacao.moeda(resumo.limiteTotalConsolidado);
			el("resumo-comprometido").textContent = formatacao.moeda(resumo.limiteComprometido);
			el("resumo-disponivel").textContent = formatacao.moeda(resumo.limiteDisponivel);
		});
	}

	function dadosFormulario() {
		return {
			cartaoId: el("compra-cartao").value,
			pessoaResponsavelId: el("compra-pessoa").value,
			categoriaId: el("compra-categoria").value,
			descricao: el("compra-descricao").value,
			dataCompra: el("compra-data").value,
			valorTotal: Number(el("compra-valor").value),
			quantidadeParcelas: Number(el("compra-parcelas").value),
			observacao: el("compra-observacao").value || null
		};
	}

	function salvar(evento) {
		evento.preventDefault();
		var botaoSalvar = el("compra-salvar");
		window.CriatiUI.setButtonLoading(botaoSalvar, true, "Registrando...");
		api.post("/api/contexto/financeiro/compras-cartao", dadosFormulario())
			.then(function (resposta) {
				var resultado = resposta.data || {};
				mensagem(resultado.alertaLimiteSaudavel
					? "Compra registrada. Atenção: o limite saudável foi ultrapassado."
					: "Compra registrada com sucesso.", false);
				evento.target.reset();
				el("compra-parcelas").value = "1";
				el("compra-data").value = new Date().toISOString().slice(0, 10);
				mostrarLimiteAtual();
				return Promise.all([carregarCadastros(), listar()]);
			})
			.catch(function (erro) {
				mensagem(erro.message || "Não foi possível registrar a compra.", true);
			})
			.finally(function () {
				window.CriatiUI.setButtonLoading(botaoSalvar, false);
			});
	}

	function alterarStatus(compra, acao) {
		var verbo = acao === "cancelar" ? "cancelar" : "estornar";
		if (!window.confirm("Deseja " + verbo + " a compra “" + compra.descricao + "”? O histórico e as parcelas serão preservados.")) {
			return;
		}
		var motivo = window.prompt("Informe o motivo:");
		if (motivo == null) return;
		if (!motivo.trim()) {
			mensagem("Informe um motivo para concluir a operação.", true);
			return;
		}
		api.post("/api/contexto/financeiro/compras-cartao/" + compra.id + "/" + acao, { motivo: motivo.trim() })
			.then(function () {
				mensagem(acao === "cancelar" ? "Compra cancelada." : "Compra estornada.", false);
				return Promise.all([carregarCadastros(), listar()]);
			})
			.catch(function (erro) {
				mensagem(erro.message || "Não foi possível concluir a operação.", true);
			});
	}

	function adicionarDetalhe(lista, rotulo, valor) {
		var termo = document.createElement("dt");
		termo.textContent = rotulo;
		var descricao = document.createElement("dd");
		descricao.textContent = valor == null || valor === "" ? "—" : valor;
		lista.append(termo, descricao);
	}

	function abrirDetalhes(compra) {
		var lista = el("compra-detalhes-lista");
		lista.replaceChildren();
		adicionarDetalhe(lista, "Descrição", compra.descricao);
		adicionarDetalhe(lista, "Data", formatacao.dataBr(compra.dataCompra));
		adicionarDetalhe(lista, "Cartão", compra.cartaoNome);
		adicionarDetalhe(lista, "Responsável", compra.pessoaResponsavelNome);
		adicionarDetalhe(lista, "Categoria", compra.categoriaNome);
		adicionarDetalhe(lista, "Destino", compra.parteFinanceiraNome ? "Terceiro: " + compra.parteFinanceiraNome : "Residência");
		adicionarDetalhe(lista, "Valor total", formatacao.moeda(compra.valorTotal));
		adicionarDetalhe(lista, "Status", statusLabel(compra.status));
		adicionarDetalhe(lista, "Observação", compra.observacao);
		adicionarDetalhe(lista, "Motivo do cancelamento", compra.motivoCancelamento);
		adicionarDetalhe(lista, "Motivo do estorno", compra.motivoEstorno);

		var corpo = el("compra-detalhes-parcelas");
		corpo.replaceChildren();
		compra.parcelas.forEach(function (parcela) {
			var linha = document.createElement("tr");
			linha.appendChild(td(parcela.numero + "/" + parcela.totalParcelas));
			linha.appendChild(td(formatacao.moeda(parcela.valor)));
			linha.appendChild(td(formatacao.dataBr(parcela.competencia)));
			linha.appendChild(td(statusLabel(parcela.status)));
			corpo.appendChild(linha);
		});
		el("compra-detalhes-modal").hidden = false;
	}

	function iniciar() {
		el("compra-form").addEventListener("submit", salvar);
		el("compra-cartao").addEventListener("change", mostrarLimiteAtual);
		el("filtrar").addEventListener("click", listar);
		el("compra-detalhes-fechar").addEventListener("click", function () {
			el("compra-detalhes-modal").hidden = true;
		});
		el("compra-data").value = new Date().toISOString().slice(0, 10);
		Promise.all([carregarCadastros(), listar()]).catch(function (erro) {
			mensagem(erro.message || "Não foi possível carregar os dados da tela.", true);
		});
	}

	window.FinanceiroComprasCartao = { iniciar: iniciar };
}());
