package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.springframework.stereotype.Service;

import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;

/**
 * Servico de dominio responsavel por calcular juros e multa de uma parcela de
 * emprestimo concedido, de acordo com a TipoCobrancaEmprestimo configurada no
 * emprestimo. Isolado do controller e dos demais services de aplicacao
 * (RecebimentoParcelaEmprestimoService apenas invoca e persiste o resultado)
 * para manter a regra de calculo em um unico lugar testavel isoladamente.
 *
 * <ul>
 * <li>SEM_JUROS / ALERTA_ATRASO: nunca gera encargo (ALERTA_ATRASO apenas
 * sinaliza atraso via ParcelaEmprestimo#estaAtrasada, sem cobranca).</li>
 * <li>COM_JUROS: juros pro-rata dia a dia desde a data de concessao do
 * emprestimo ate a data de referencia, independentemente de atraso — custo
 * financeiro do emprestimo em si.</li>
 * <li>MULTA_ATRASO: multa unica (percentual sobre o principal da parcela),
 * aplicada apenas quando a data de referencia e posterior ao vencimento.</li>
 * <li>JUROS_MORA_ATRASO: juros pro-rata dia a dia apenas sobre os dias entre
 * o vencimento e a data de referencia (nunca antes do vencimento).</li>
 * </ul>
 */
@Service
public class EncargosEmprestimoService {

	private static final int DIAS_MES_REFERENCIA = 30;

	public BigDecimal calcularJuros(EmprestimoConcedido emprestimo, BigDecimal valorPrincipalParcela,
			LocalDate vencimentoParcela, LocalDate referencia) {
		Objects.requireNonNull(emprestimo, "emprestimo nao pode ser nulo");
		Objects.requireNonNull(valorPrincipalParcela, "valorPrincipalParcela nao pode ser nulo");
		Objects.requireNonNull(vencimentoParcela, "vencimentoParcela nao pode ser nulo");
		Objects.requireNonNull(referencia, "referencia nao pode ser nula");
		TipoCobrancaEmprestimo tipo = emprestimo.getTipoCobranca();
		if (tipo == TipoCobrancaEmprestimo.COM_JUROS) {
			long dias = ChronoUnit.DAYS.between(emprestimo.getDataConcessao(), referencia);
			return jurosProRataDiaria(valorPrincipalParcela, emprestimo.getPercentualJuros(), dias);
		}
		if (tipo == TipoCobrancaEmprestimo.JUROS_MORA_ATRASO) {
			long dias = ChronoUnit.DAYS.between(vencimentoParcela, referencia);
			return jurosProRataDiaria(valorPrincipalParcela, emprestimo.getPercentualJuros(), dias);
		}
		return zero();
	}

	public BigDecimal calcularMulta(EmprestimoConcedido emprestimo, BigDecimal valorPrincipalParcela,
			LocalDate vencimentoParcela, LocalDate referencia) {
		Objects.requireNonNull(emprestimo, "emprestimo nao pode ser nulo");
		Objects.requireNonNull(valorPrincipalParcela, "valorPrincipalParcela nao pode ser nulo");
		Objects.requireNonNull(vencimentoParcela, "vencimentoParcela nao pode ser nulo");
		Objects.requireNonNull(referencia, "referencia nao pode ser nula");
		if (emprestimo.getTipoCobranca() != TipoCobrancaEmprestimo.MULTA_ATRASO) {
			return zero();
		}
		if (!referencia.isAfter(vencimentoParcela)) {
			return zero();
		}
		return valorPrincipalParcela.multiply(emprestimo.getPercentualMulta())
				.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal jurosProRataDiaria(BigDecimal base, BigDecimal percentualMensal, long dias) {
		if (dias <= 0 || percentualMensal == null) {
			return zero();
		}
		BigDecimal taxaDiaria = percentualMensal.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
				.divide(BigDecimal.valueOf(DIAS_MES_REFERENCIA), 10, RoundingMode.HALF_UP);
		return base.multiply(taxaDiaria).multiply(BigDecimal.valueOf(dias)).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal zero() {
		return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	}
}
