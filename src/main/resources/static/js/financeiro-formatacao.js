/* Criati Financeiro - formatacao de moeda (BRL) e datas (pt-BR) para exibicao.
   O backend sempre trafega valores numericos (BigDecimal) e datas ISO
   (AAAA-MM-DD); a formatacao regional acontece somente aqui, na interface. */
(function (window) {
	"use strict";

	var FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

	function moeda(valor) {
		var numero = Number(valor);
		if (Number.isNaN(numero)) {
			return FORMATADOR_MOEDA.format(0);
		}
		return FORMATADOR_MOEDA.format(numero);
	}

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

	function competenciaLabel(aaaaMm) {
		if (!aaaaMm) {
			return "-";
		}
		var partes = aaaaMm.split("-");
		if (partes.length !== 2) {
			return aaaaMm;
		}
		var meses = [
			"Janeiro", "Fevereiro", "Marco", "Abril", "Maio", "Junho",
			"Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
		];
		var indice = parseInt(partes[1], 10) - 1;
		var nomeMes = meses[indice] || partes[1];
		return nomeMes + " de " + partes[0];
	}

	function competenciaAtual() {
		var agora = new Date();
		var mes = String(agora.getMonth() + 1).padStart(2, "0");
		return agora.getFullYear() + "-" + mes;
	}

	function aplicarSemantica(elemento, valor, natureza) {
		if (!elemento) {
			return;
		}
		var numero = Number(valor);
		elemento.classList.remove("criati-valor-positivo", "criati-valor-negativo", "criati-valor-neutro");
		var classe = "criati-valor-neutro";
		if (!Number.isNaN(numero) && numero !== 0) {
			if (natureza === "ENTRADA") {
				classe = "criati-valor-positivo";
			} else if (natureza === "SAIDA") {
				classe = "criati-valor-negativo";
			} else if (natureza === "SALDO") {
				classe = numero > 0 ? "criati-valor-positivo" : "criati-valor-negativo";
			}
		}
		elemento.classList.add(classe);
	}

	function renderMoeda(elemento, valor, natureza) {
		elemento.textContent = moeda(valor);
		aplicarSemantica(elemento, valor, natureza);
	}

	window.FinanceiroFormatacao = {
		moeda: moeda,
		dataBr: dataBr,
		competenciaLabel: competenciaLabel,
		competenciaAtual: competenciaAtual,
		aplicarSemantica: aplicarSemantica,
		renderMoeda: renderMoeda
	};
})(window);
