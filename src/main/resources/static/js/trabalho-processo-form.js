/* Criati Trabalho - cadastro e edicao de processo. */
(function (window, document) {
	"use strict";
	function el(id) { return document.getElementById(id); }

	function iniciar() {
		var form = el("trabalho-processo-form");
		if (!form) return;
		var processoId = el("trabalho-processo-id").value;

		carregarResponsaveis().then(function () {
			if (processoId) {
				el("trabalho-processo-form-titulo").textContent = "Editar processo";
				window.TrabalhoApi.processos.buscar(processoId).then(function (resposta) {
					preencher(resposta.data);
				}).catch(function () {
					window.CriatiUI.showToast("erro", "Não foi possível carregar este processo.");
				});
			}
		});

		form.addEventListener("submit", function (evento) {
			evento.preventDefault();
			if (!window.CriatiUI.validarObrigatorios(form)) return;
			var botao = el("trabalho-processo-salvar");
			var responsavelId = el("trabalho-processo-responsavel").value || null;
			var dados = {
				titulo: el("trabalho-processo-titulo").value,
				descricao: el("trabalho-processo-descricao").value || null,
				responsavelId: responsavelId,
				prioridade: el("trabalho-processo-prioridade").value,
				prazo: el("trabalho-processo-prazo").value || null
			};
			window.CriatiUI.setButtonLoading(botao, true, "Salvando...");
			// editar() nao altera responsavel (acao dedicada, mesmo contrato da
			// CRIATI-WRK-001): em modo edicao, encadeia as duas chamadas ja
			// aprovadas em vez de expandir o backend.
			var chamada = processoId
				? window.TrabalhoApi.processos.editar(processoId, dados)
					.then(function (resposta) {
						return window.TrabalhoApi.processos.atribuirResponsavel(processoId, responsavelId)
							.catch(function () { return resposta; });
					})
				: window.TrabalhoApi.processos.criar(dados);
			chamada.then(function (resposta) {
				window.CriatiUI.showToast("sucesso", "Processo salvo com sucesso.");
				window.location.href = "/app/trabalho/processos/" + (resposta.data ? resposta.data.id : processoId);
			}).catch(function (erro) {
				window.CriatiUI.showToast("erro", (erro && erro.message) || "Não foi possível salvar o processo.");
			}).finally(function () {
				window.CriatiUI.setButtonLoading(botao, false);
			});
		});
	}

	function carregarResponsaveis() {
		return window.TrabalhoApi.integrantes().then(function (resposta) {
			var select = el("trabalho-processo-responsavel");
			(resposta.data || []).forEach(function (integrante) {
				var opcao = document.createElement("option");
				opcao.value = integrante.id;
				opcao.textContent = integrante.nome;
				select.appendChild(opcao);
			});
		}).catch(function () {
			// Sem permissao (nao ADMINISTRADOR) ou falha de rede: mantem apenas
			// a opcao "sem responsavel", sem quebrar o restante do formulario.
		});
	}

	function preencher(processo) {
		el("trabalho-processo-titulo").value = processo.titulo;
		el("trabalho-processo-descricao").value = processo.descricao || "";
		el("trabalho-processo-responsavel").value = processo.responsavelId || "";
		el("trabalho-processo-prioridade").value = processo.prioridade;
		el("trabalho-processo-prazo").value = processo.prazo || "";
	}

	window.TrabalhoProcessoForm = { iniciar: iniciar };
})(window, document);
