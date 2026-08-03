/* Criati Trabalho - detalhes da tarefa: dados, historico e acoes. O
   responsavel pela tarefa pode atualizar o proprio andamento mesmo sem ser
   ADMINISTRADOR (CRIATI-WRK-001, TarefaEmpresarialService.exigirEscritaOuResponsavel). */
(function (window, document) {
	"use strict";
	function el(id) { return document.getElementById(id); }

	function iniciar() {
		var idInput = el("trabalho-tarefa-id");
		if (!idInput) return;
		var tarefaId = idInput.value;
		var podeEscrever = el("trabalho-pode-escrever").value === "true";
		var usuarioEmpresaAtualId = el("trabalho-usuario-empresa-atual-id").value;
		carregar(tarefaId, podeEscrever, usuarioEmpresaAtualId);
	}

	function carregar(tarefaId, podeEscrever, usuarioEmpresaAtualId) {
		el("trabalho-tarefa-detalhe-carregando").hidden = false;
		el("trabalho-tarefa-detalhe-erro").hidden = true;
		el("trabalho-tarefa-detalhe-conteudo").hidden = true;
		Promise.all([
			window.TrabalhoApi.tarefas.buscar(tarefaId),
			window.TrabalhoApi.tarefas.historico(tarefaId)
		]).then(function (respostas) {
			var tarefa = respostas[0].data;
			var historico = respostas[1].data || [];
			el("trabalho-tarefa-detalhe-carregando").hidden = true;
			el("trabalho-tarefa-detalhe-conteudo").hidden = false;
			document.title = tarefa.titulo + " · Trabalho · Criati";
			el("trabalho-tarefa-detalhe-titulo").textContent = tarefa.titulo;
			renderDados(tarefa);
			var souResponsavel = Boolean(tarefa.responsavelId) && tarefa.responsavelId === usuarioEmpresaAtualId;
			renderAcoes(tarefa, podeEscrever, souResponsavel);
			renderHistorico(historico);
		}).catch(function () {
			el("trabalho-tarefa-detalhe-carregando").hidden = true;
			el("trabalho-tarefa-detalhe-erro").hidden = false;
		});
	}

	function linhaDetalhe(container, rotulo, valor, link) {
		var linha = document.createElement("div");
		linha.className = "criati-trabalho-detalhe-linha";
		var rotuloEl = document.createElement("span"); rotuloEl.textContent = rotulo;
		var valorEl;
		if (link) {
			valorEl = document.createElement("a"); valorEl.href = link;
		} else {
			valorEl = document.createElement("span");
		}
		valorEl.textContent = valor;
		linha.appendChild(rotuloEl); linha.appendChild(valorEl);
		container.appendChild(linha);
	}

	function renderDados(tarefa) {
		var container = el("trabalho-tarefa-detalhe-dados");
		container.innerHTML = "";
		linhaDetalhe(container, "Descrição", tarefa.descricao || "-");
		if (tarefa.processoId) {
			linhaDetalhe(container, "Processo", tarefa.processoTitulo, "/app/trabalho/processos/" + tarefa.processoId);
		} else {
			linhaDetalhe(container, "Processo", "Sem processo vinculado");
		}
		linhaDetalhe(container, "Responsável", tarefa.responsavelNome || "Sem responsável");
		linhaDetalhe(container, "Situação", window.TrabalhoFormatacao.rotuloSituacao(tarefa.situacao)
			+ (tarefa.atrasada ? " · Atrasada" : ""));
		linhaDetalhe(container, "Prioridade", window.TrabalhoFormatacao.rotuloPrioridade(tarefa.prioridade));
		linhaDetalhe(container, "Prazo", window.TrabalhoFormatacao.dataBr(tarefa.prazo));
		linhaDetalhe(container, "Conclusão", window.TrabalhoFormatacao.dataHoraBr(tarefa.dataConclusao));
		linhaDetalhe(container, "Status do cadastro", tarefa.status === "ATIVO" ? "Ativo" : "Inativo");
	}

	function botaoAcao(rotulo, aoClicar, classe) {
		var botao = document.createElement("button");
		botao.type = "button";
		botao.className = "criati-btn " + (classe || "criati-btn-ghost");
		botao.textContent = rotulo;
		botao.addEventListener("click", aoClicar);
		return botao;
	}

	function renderAcoes(tarefa, podeEscrever, souResponsavel) {
		var container = el("trabalho-tarefa-detalhe-acoes");
		container.innerHTML = "";
		if (tarefa.status !== "ATIVO") return;

		if (podeEscrever) {
			container.appendChild(botaoAcao("Editar", function () {
				window.location.href = "/app/trabalho/tarefas/" + tarefa.id + "/editar";
			}));
		}

		var podeAtualizarAndamento = podeEscrever || souResponsavel;
		if (podeAtualizarAndamento) {
			if (tarefa.situacao === "PENDENTE") {
				container.appendChild(botaoAcao("Iniciar", function () { executar(window.TrabalhoApi.tarefas.iniciar(tarefa.id)); }, "criati-btn-primary"));
			}
			if (tarefa.situacao === "PENDENTE" || tarefa.situacao === "EM_ANDAMENTO") {
				container.appendChild(botaoAcao("Concluir", function () { executar(window.TrabalhoApi.tarefas.concluir(tarefa.id)); }, "criati-btn-primary"));
				container.appendChild(botaoAcao("Cancelar", function () {
					if (window.confirm("Deseja cancelar esta tarefa?")) executar(window.TrabalhoApi.tarefas.cancelar(tarefa.id));
				}));
			}
			if (tarefa.situacao === "CONCLUIDA" || tarefa.situacao === "CANCELADA") {
				container.appendChild(botaoAcao("Reabrir", function () { executar(window.TrabalhoApi.tarefas.reabrir(tarefa.id)); }));
			}
		}

		if (podeEscrever) {
			container.appendChild(botaoAcao("Inativar", function () {
				if (window.confirm("A tarefa continuará visível no histórico. Deseja inativá-la?")) {
					executar(window.TrabalhoApi.tarefas.inativar(tarefa.id));
				}
			}));
		}
	}

	var executandoAcao = false;

	// Trava todos os botoes de acao enquanto a chamada esta em voo: evita duplo
	// clique disparar a mesma transicao antes da resposta.
	function executar(promessa) {
		if (executandoAcao) return;
		executandoAcao = true;
		var botoes = el("trabalho-tarefa-detalhe-acoes").querySelectorAll("button");
		botoes.forEach(function (botao) { botao.disabled = true; });
		promessa.then(function () {
			window.CriatiUI.showToast("sucesso", "Tarefa atualizada com sucesso.");
			window.location.reload();
		}).catch(function (erro) {
			window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível atualizar a tarefa.");
			executandoAcao = false;
			botoes.forEach(function (botao) { botao.disabled = false; });
		});
	}

	function renderHistorico(historico) {
		var lista = el("trabalho-tarefa-detalhe-historico");
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

	window.TrabalhoTarefaDetalhe = { iniciar: iniciar };
})(window, document);
