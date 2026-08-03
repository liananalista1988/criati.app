/* Criati Trabalho - cadastro e edicao de tarefa. */
(function (window, document) {
	"use strict";
	function el(id) { return document.getElementById(id); }

	function processoIdDaQuery() {
		var parametros = new URLSearchParams(window.location.search);
		return parametros.get("processoId") || "";
	}

	function iniciar() {
		var form = el("trabalho-tarefa-form");
		if (!form) return;
		var tarefaId = el("trabalho-tarefa-id").value;

		Promise.all([carregarResponsaveis(), carregarProcessos()]).then(function () {
			if (tarefaId) {
				el("trabalho-tarefa-form-titulo").textContent = "Editar tarefa";
				window.TrabalhoApi.tarefas.buscar(tarefaId).then(function (resposta) {
					preencher(resposta.data);
				}).catch(function () {
					window.CriatiUI.showToast("erro", "Não foi possível carregar esta tarefa.");
				});
			} else {
				var processoPreSelecionado = processoIdDaQuery();
				if (processoPreSelecionado) {
					el("trabalho-tarefa-processo").value = processoPreSelecionado;
				}
			}
		});

		form.addEventListener("submit", function (evento) {
			evento.preventDefault();
			if (!window.CriatiUI.validarObrigatorios(form)) return;
			var botao = el("trabalho-tarefa-salvar");
			var responsavelId = el("trabalho-tarefa-responsavel").value || null;
			var dados = {
				processoId: el("trabalho-tarefa-processo").value || null,
				titulo: el("trabalho-tarefa-titulo").value,
				descricao: el("trabalho-tarefa-descricao").value || null,
				responsavelId: responsavelId,
				prioridade: el("trabalho-tarefa-prioridade").value,
				prazo: el("trabalho-tarefa-prazo").value || null
			};
			window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
			var chamada = tarefaId
				? window.TrabalhoApi.tarefas.editar(tarefaId, dados)
					.then(function (resposta) {
						return window.TrabalhoApi.tarefas.atribuirResponsavel(tarefaId, responsavelId)
							.catch(function () { return resposta; });
					})
				: window.TrabalhoApi.tarefas.criar(dados);
			chamada.then(function (resposta) {
				window.CriatiUI.showToast("sucesso", "Tarefa salva com sucesso.");
				window.location.href = "/app/trabalho/tarefas/" + (resposta.data ? resposta.data.id : tarefaId);
			}).catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível salvar a tarefa.");
			}).finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
		});
	}

	function carregarResponsaveis() {
		return window.TrabalhoApi.integrantes().then(function (resposta) {
			var select = el("trabalho-tarefa-responsavel");
			(resposta.data || []).forEach(function (integrante) {
				var opcao = document.createElement("option");
				opcao.value = integrante.id;
				opcao.textContent = integrante.nome;
				select.appendChild(opcao);
			});
		}).catch(function () { /* usuario sem permissao de listar integrantes: mantem so "sem responsavel" */ });
	}

	function carregarProcessos() {
		return window.TrabalhoApi.processos.listar({ status: "ATIVO", tamanho: 100 }).then(function (resposta) {
			var select = el("trabalho-tarefa-processo");
			((resposta.data && resposta.data.itens) || []).forEach(function (processo) {
				var opcao = document.createElement("option");
				opcao.value = processo.id;
				opcao.textContent = processo.titulo;
				select.appendChild(opcao);
			});
		}).catch(function () { /* mantem apenas "sem processo vinculado" */ });
	}

	function preencher(tarefa) {
		el("trabalho-tarefa-processo").value = tarefa.processoId || "";
		el("trabalho-tarefa-titulo").value = tarefa.titulo;
		el("trabalho-tarefa-descricao").value = tarefa.descricao || "";
		el("trabalho-tarefa-responsavel").value = tarefa.responsavelId || "";
		el("trabalho-tarefa-prioridade").value = tarefa.prioridade;
		el("trabalho-tarefa-prazo").value = tarefa.prazo || "";
	}

	window.TrabalhoTarefaForm = { iniciar: iniciar };
})(window, document);
