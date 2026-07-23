package br.app.criati.shared.enums;

/**
 * Configuracao de cobranca de um EmprestimoConcedido. Define, para todo o
 * emprestimo, se e como encargos sao calculados (EncargosEmprestimoService):
 * SEM_JUROS e ALERTA_ATRASO nunca geram encargo (a segunda apenas sinaliza
 * atraso para o usuario); COM_JUROS acresce juros pro-rata desde a concessao,
 * independente de atraso; MULTA_ATRASO acresce multa unica quando a parcela
 * vence sem pagamento; JUROS_MORA_ATRASO acresce juros pro-rata apenas sobre
 * os dias em atraso.
 */
public enum TipoCobrancaEmprestimo {
	SEM_JUROS,
	COM_JUROS,
	ALERTA_ATRASO,
	MULTA_ATRASO,
	JUROS_MORA_ATRASO
}
