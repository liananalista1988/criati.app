/* Criati Trabalho - detalhes do processo: dados, tarefas vinculadas, historico e acoes. */
(function (window, document) {
	"use strict";
	function el(id) { return document.getElementById(id); }

	function iniciar() {
		var idInput = el("trabalho-processo-id");
		if (!idInput) return;
		var processoId = idInput.value;
		var podeEscrever = el("trabalho-pode-escrever").value === "true";
		carregar(processoId, podeEscrever);
	}

	function carregar(processoId, podeEscrever) {
		el("trabalho-processo-detalhe-carregando").hidden = false;
		el("trabalho-processo-detalhe-erro").hidden = true;
		el("trabalho-processo-detalhe-conteudo").hidden = true;
		Promise.all([
			window.TrabalhoApi.processos.buscar(processoId),
			window.TrabalhoApi.tarefas.listar({ processoId: processoId, tamanho: 100 }),
			window.TrabalhoApi.processos.historico(processoId)
		]).then(function (respostas) {
			var processo = respostas[0].data;
			var tarefas = (respostas[1].data && respostas[1].data.itens) || [];
			var historico = respostas[2].data || [];
			el("trabalho-processo-detalhe-carregando").hidden = true;
			el("trabalho-processo-detalhe-conteudo").hidden = false;
			document.title = processo.titulo + " · Trabalho · Criati";
			el("trabalho-processo-detalhe-titulo").textContent = processo.titulo;
			renderDados(processo);
			renderAcoes(processo, podeEscrever);
			renderTarefas(tarefas);
			renderHistorico(historico);
		}).catch(function () {
			el("trabalho-processo-detalhe-carregando").hidden = true;
			el("trabalho-processo-detalhe-erro").hidden = false;
		});
	}

	function linhaDetalhe(container, rotulo, valor) {
		var linha = document.createElement("div");
		linha.className = "criati-trabalho-detalhe-linha";
		var rotuloEl = document.createElement("span"); rotuloEl.textContent = rotulo;
		var valorEl = document.createElement("span"); valorEl.textContent = valor;
		linha.appendChild(rotuloEl); linha.appendChild(valorEl);
		container.appendChild(linha);
	}

	function renderDados(processo) {
		var container = el("trabalho-processo-detalhe-dados");
		container.innerHTML = "";
		linhaDetalhe(container, "Descrição", processo.descricao || "-");
		linhaDetalhe(container, "Responsável", processo.responsavelNome || "Sem responsável");
		linhaDetalhe(container, "Situação", window.TrabalhoFormatacao.rotuloSituacao(processo.situacao)
			+ (processo.atrasado ? " · Atrasado" : ""));
		linhaDetalhe(container, "Prioridade", window.TrabalhoFormatacao.rotuloPrioridade(processo.prioridade));
		linhaDetalhe(container, "Abertura", window.TrabalhoFormatacao.dataBr(processo.dataAbertura));
		linhaDetalhe(container, "Prazo", window.TrabalhoFormatacao.dataBr(processo.prazo));
		linhaDetalhe(container, "Conclusão", window.TrabalhoFormatacao.dataHoraBr(processo.dataConclusao));
		linhaDetalhe(container, "Tarefas concluídas", processo.quantidadeTarefasConcluidas + " de " + processo.quantidadeTarefas);
		linhaDetalhe(container, "Status do cadastro", processo.status === "ATIVO" ? "Ativo" : "Inativo");
	}

	function botaoAcao(rotulo, aoClicar, classe) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn " + (classe || "criati-btn-ghost");
		botao.textContent = rotulo;
		botao.addEventListener("click", aoClicar);
		return botao;
	}

	function renderAcoes(processo, podeEscrever) {
		var container = el("trabalho-processo-detalhe-acoes");
		container.innerHTML = "";
		if (!podeEscrever) return;

		if (processo.status === "ATIVO") {
			container.appendChild(botaoAcao("Editar", function () {
				window.location.href = "/app/trabalho/processos/" + processo.id + "/editar";
			}));
			if (processo.situacao === "ABERTO") {
				container.appendChild(botaoAcao("Iniciar", function () { executar(window.TrabalhoApi.processos.iniciar(processo.id)); }, "criati-btn-primary"));
			}
			if (processo.situacao === "ABERTO" || processo.situacao === "EM_ANDAMENTO") {
				container.appendChild(botaoAcao("Concluir", function () { executar(window.TrabalhoApi.processos.concluir(processo.id)); }, "criati-btn-primary"));
				container.appendChild(botaoAcao("Cancelar", function () {
					if (window.confirm("Deseja cancelar este processo?")) executar(window.TrabalhoApi.processos.cancelar(processo.id));
				}));
			}
			if (processo.situacao === "CONCLUIDO" || processo.situacao === "CANCELADO") {
				container.appendChild(botaoAcao("Reabrir", function () { executar(window.TrabalhoApi.processos.reabrir(processo.id)); }));
			}
			container.appendChild(botaoAcao("Nova tarefa vinculada", function () {
				window.location.href = "/app/trabalho/tarefas/nova?processoId=" + processo.id;
			}));
			container.appendChild(botaoAcao("Inativar", function () {
				if (window.confirm("O processo continuará visível no histórico. Deseja inativá-lo?")) {
					executar(window.TrabalhoApi.processos.inativar(processo.id));
				}
			}));
		}
	}

	var executandoAcao = false;

	// Trava todos os botoes de acao enquanto a chamada esta em voo: evita duplo
	// clique disparar a mesma transicao (ex.: dois "Concluir") antes da resposta.
	function executar(promessa) {
		if (executandoAcao) return;
		executandoAcao = true;
		var botoes = el("trabalho-processo-detalhe-acoes").querySelectorAll("button");
		botoes.forEach(function (botao) { botao.disabled = true; });
		promessa.then(function () {
			window.CriatiUI.showToast("sucesso", "Processo atualizado com sucesso.");
			window.location.reload();
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível atualizar o processo.");
			executandoAcao = false;
			botoes.forEach(function (botao) { botao.disabled = false; });
		});
	}

	function renderTarefas(tarefas) {
		var corpo = el("trabalho-processo-detalhe-tarefas");
		var vazio = el("trabalho-processo-detalhe-tarefas-vazio");
		corpo.innerHTML = "";
		vazio.hidden = tarefas.length !== 0;
		tarefas.forEach(function (tarefa) {
			var linha = document.createElement("tr");
			var celulaTitulo = document.createElement("td");
			var link = document.createElement("a"); link.href = "/app/trabalho/tarefas/" + tarefa.id; link.textContent = tarefa.titulo;
			celulaTitulo.appendChild(link); linha.appendChild(celulaTitulo);

			var celulaResp = document.createElement("td"); celulaResp.textContent = tarefa.responsavelNome || "Sem responsável";
			linha.appendChild(celulaResp);

			var celulaSituacao = document.createElement("td");
			var badge = document.createElement("span");
			badge.className = "criati-badge criati-badge-" + tarefa.situacao.toLowerCase();
			badge.textContent = window.TrabalhoFormatacao.rotuloSituacao(tarefa.situacao);
			celulaSituacao.appendChild(badge); linha.appendChild(celulaSituacao);

			var celulaPrazo = document.createElement("td"); celulaPrazo.textContent = window.TrabalhoFormatacao.dataBr(tarefa.prazo);
			linha.appendChild(celulaPrazo);

			var celulaAcoes = document.createElement("td"); celulaAcoes.className = "criati-table-acoes";
			var detalhes = document.createElement("a"); detalhes.className = "criati-btn criati-btn-ghost";
			detalhes.href = "/app/trabalho/tarefas/" + tarefa.id; detalhes.textContent = "Detalhes";
			celulaAcoes.appendChild(detalhes); linha.appendChild(celulaAcoes);

			corpo.appendChild(linha);
		});
	}

	function renderHistorico(historico) {
		var lista = el("trabalho-processo-detalhe-historico");
		lista.innerHTML = "";
		if (!historico.length) {
			var vazio = document.createElement("li"); vazio.textContent = "Nenhum evento registrado.";
			lista.appendChild(vazio);
			return;
		}
		historico.forEach(function (evento) {
			var item = document.createElement("li");
			item.className = "criati-trabalho-detalhe-linha";
			var texto = document.createElement("span");
			texto.textContent = window.TrabalhoFormatacao.rotuloEvento(evento.tipoEvento)
				+ (evento.descricao ? " — " + evento.descricao : "") + " · " + evento.autorNome;
			var quando = document.createElement("span");
			quando.textContent = window.TrabalhoFormatacao.dataHoraBr(evento.ocorridoEm);
			item.appendChild(texto); item.appendChild(quando);
			lista.appendChild(item);
		});
	}

	window.TrabalhoProcessoDetalhe = { iniciar: iniciar };
})(window, document);
