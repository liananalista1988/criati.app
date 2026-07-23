(function () {
	"use strict";

	var api = window.CriatiApi;
	var formatacao = window.FinanceiroFormatacao;
	var comprasCarregadas = [];

	function el(id) {
		return document.getElementById(id);
	}

	function option(valor, texto) {
		var item = document.createElement("option");
		item.value = valor;
		item.textContent = texto;
		return item;
	}

	function td(texto) {
		var celula = document.createElement("td");
		celula.textContent = texto == null || texto === "" ? "—" : texto;
		return celula;
	}

	function statusLabel(status) {
		return {
			ABERTA: "Aberta",
			CANCELADA: "Cancelada",
			ESTORNADA: "Estornada"
		}[status] || status;
	}

	function preencherFiltro(id, itens) {
		var campo = el(id);
		itens.forEach(function (item) {
			campo.appendChild(option(item.id, item.nome));
		});
	}

	function mostrarErro(erro) {
		var mensagem = el("parcelas-mensagem");
		mensagem.textContent = erro && erro.message ? erro.message : "Não foi possível carregar as parcelas.";
		mensagem.hidden = false;
	}

	function limparErro() {
		el("parcelas-mensagem").hidden = true;
	}

	function carregarCadastros() {
		return Promise.all([
			api.get("/api/contexto/financeiro/cartoes?status=ATIVO"),
			api.get("/api/contexto/financeiro/pessoas?status=ATIVO")
		]).then(function (resultados) {
			preencherFiltro("parcelas-filtro-cartao", resultados[0].data || []);
			preencherFiltro("parcelas-filtro-pessoa", resultados[1].data || []);
		});
	}

	function filtrosCompra() {
		var query = new URLSearchParams();
		var cartao = el("parcelas-filtro-cartao").value;
		var pessoa = el("parcelas-filtro-pessoa").value;
		var competencia = el("parcelas-filtro-competencia").value;
		if (cartao) query.set("cartaoId", cartao);
		if (pessoa) query.set("titularId", pessoa);
		if (competencia) query.set("competencia", competencia + "-01");
		return query.toString();
	}

	function parcelasVisiveis() {
		var status = el("parcelas-filtro-status").value;
		var competencia = el("parcelas-filtro-competencia").value;
		return comprasCarregadas.flatMap(function (compra) {
			return compra.parcelas
				.filter(function (parcela) {
					return (!status || parcela.status === status)
						&& (!competencia || parcela.competencia.slice(0, 7) === competencia);
				})
				.map(function (parcela) {
					return { compra: compra, parcela: parcela };
				});
		}).sort(function (a, b) {
			return a.parcela.competencia.localeCompare(b.parcela.competencia)
				|| a.compra.descricao.localeCompare(b.compra.descricao);
		});
	}

	function botaoDetalhes(item) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn criati-btn-ghost";
		botao.textContent = "Detalhes";
		botao.addEventListener("click", function () {
			abrirDetalhes(item);
		});
		return botao;
	}

	function renderizar() {
		var itens = parcelasVisiveis();
		var corpo = el("parcelas-tbody");
		corpo.replaceChildren();
		itens.forEach(function (item) {
			var compra = item.compra;
			var parcela = item.parcela;
			var linha = document.createElement("tr");
			linha.appendChild(td(formatacao.dataBr(parcela.competencia)));
			linha.appendChild(td(compra.descricao));
			linha.appendChild(td(compra.cartaoNome));
			linha.appendChild(td(compra.pessoaResponsavelNome));
			linha.appendChild(td(parcela.numero + "/" + parcela.totalParcelas));
			linha.appendChild(td(formatacao.moeda(parcela.valor)));
			linha.appendChild(td(compra.parteFinanceiraNome ? "Terceiro: " + compra.parteFinanceiraNome : "Residência"));
			linha.appendChild(td(statusLabel(parcela.status)));
			var acoes = document.createElement("td");
			acoes.appendChild(botaoDetalhes(item));
			linha.appendChild(acoes);
			corpo.appendChild(linha);
		});
		el("parcelas-carregando").hidden = true;
		el("parcelas-vazio").hidden = itens.length > 0;
		el("parcelas-tabela-wrap").hidden = itens.length === 0;
	}

	function adicionarDetalhe(lista, rotulo, valor) {
		var termo = document.createElement("dt");
		termo.textContent = rotulo;
		var descricao = document.createElement("dd");
		descricao.textContent = valor == null || valor === "" ? "—" : valor;
		lista.append(termo, descricao);
	}

	function abrirDetalhes(item) {
		var compra = item.compra;
		var parcela = item.parcela;
		var lista = el("parcela-detalhes-lista");
		lista.replaceChildren();
		adicionarDetalhe(lista, "Compra", compra.descricao);
		adicionarDetalhe(lista, "Cartão", compra.cartaoNome);
		adicionarDetalhe(lista, "Responsável", compra.pessoaResponsavelNome);
		adicionarDetalhe(lista, "Destino", compra.parteFinanceiraNome ? "Terceiro: " + compra.parteFinanceiraNome : "Residência");
		adicionarDetalhe(lista, "Parcela", parcela.numero + " de " + parcela.totalParcelas);
		adicionarDetalhe(lista, "Valor", formatacao.moeda(parcela.valor));
		adicionarDetalhe(lista, "Competência", formatacao.dataBr(parcela.competencia));
		adicionarDetalhe(lista, "Status", statusLabel(parcela.status));
		adicionarDetalhe(lista, "Valor total da compra", formatacao.moeda(compra.valorTotal));
		el("parcela-detalhes-modal").hidden = false;
	}

	function carregar() {
		limparErro();
		el("parcelas-carregando").hidden = false;
		var sufixo = filtrosCompra();
		return api.get("/api/contexto/financeiro/compras-cartao" + (sufixo ? "?" + sufixo : ""))
			.then(function (resposta) {
				comprasCarregadas = resposta.data || [];
				renderizar();
			})
			.catch(function (erro) {
				el("parcelas-carregando").hidden = true;
				el("parcelas-tabela-wrap").hidden = true;
				el("parcelas-vazio").hidden = true;
				mostrarErro(erro);
			});
	}

	function iniciar() {
		el("parcelas-filtrar").addEventListener("click", carregar);
		el("parcelas-filtro-status").addEventListener("change", renderizar);
		el("parcela-detalhes-fechar").addEventListener("click", function () {
			el("parcela-detalhes-modal").hidden = true;
		});
		carregarCadastros().then(carregar).catch(mostrarErro);
	}

	window.FinanceiroParcelasCartao = { iniciar: iniciar };
}());
