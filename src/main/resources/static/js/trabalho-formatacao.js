/* Criati Trabalho - formatacao de datas (pt-BR) para exibicao. O backend
   sempre trafega datas ISO (AAAA-MM-DD) e instantes ISO com offset; a
   formatacao regional acontece somente aqui, na interface. Modulo
   independente do Financeiro: nao reaproveita financeiro-formatacao.js. */
(function (window) {
	"use strict";

	function dataBr(isoDate) {
		if (!isoDate) {
			return "-";
		}
		var partes = isoDate.split("-");
		if (partes.length !== 3) {
			return isoDate;
		}
		return partes[2] + "/" + partes[1] + "/" + partes[0];
	}

	function dataHoraBr(isoDateTime) {
		if (!isoDateTime) {
			return "-";
		}
		var data = new Date(isoDateTime);
		if (Number.isNaN(data.getTime())) {
			return isoDateTime;
		}
		return data.toLocaleDateString("pt-BR") + " " + data.toLocaleTimeString("pt-BR", {
			hour: "2-digit", minute: "2-digit"
		});
	}

	var ROTULOS_SITUACAO = {
		ABERTO: "Aberto", EM_ANDAMENTO: "Em andamento", CONCLUIDO: "Concluído", CANCELADO: "Cancelado",
		PENDENTE: "Pendente", CONCLUIDA: "Concluída", CANCELADA: "Cancelada"
	};

	var ROTULOS_PRIORIDADE = { BAIXA: "Baixa", MEDIA: "Média", ALTA: "Alta", URGENTE: "Urgente" };

	var ROTULOS_EVENTO = {
		CRIACAO: "Criação", ALTERACAO_RESPONSAVEL: "Responsável alterado",
		ALTERACAO_SITUACAO: "Situação alterada", ALTERACAO_PRAZO: "Prazo alterado",
		CONCLUSAO: "Conclusão", REABERTURA: "Reabertura", INATIVACAO: "Inativação"
	};

	function rotuloSituacao(valor) {
		return ROTULOS_SITUACAO[valor] || valor || "-";
	}

	function rotuloPrioridade(valor) {
		return ROTULOS_PRIORIDADE[valor] || valor || "-";
	}

	function rotuloEvento(valor) {
		return ROTULOS_EVENTO[valor] || valor || "-";
	}

	window.TrabalhoFormatacao = {
		dataBr: dataBr,
		dataHoraBr: dataHoraBr,
		rotuloSituacao: rotuloSituacao,
		rotuloPrioridade: rotuloPrioridade,
		rotuloEvento: rotuloEvento
	};
})(window);
