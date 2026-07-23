package br.app.criati.financeiro.service;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal; import java.time.LocalDate; import org.junit.jupiter.api.*;
class ParcelamentoCartaoServiceTests {
 private final ParcelamentoCartaoService service=new ParcelamentoCartaoService();
 @Test void compraAVistaGeraUmaParcela(){assertThat(service.dividir(new BigDecimal("50.00"),1)).containsExactly(new BigDecimal("50.00"));}
 @Test void divisaoExataMantemEscala(){assertThat(service.dividir(new BigDecimal("90.00"),3)).containsExactly(new BigDecimal("30.00"),new BigDecimal("30.00"),new BigDecimal("30.00"));}
 @Test void residualFicaNaUltimaParcela(){assertThat(service.dividir(new BigDecimal("100.00"),3)).containsExactly(new BigDecimal("33.33"),new BigDecimal("33.33"),new BigDecimal("33.34"));}
 @Test void somaDasParcelasEIgualAoTotal(){assertThat(service.dividir(new BigDecimal("10.01"),6).stream().reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("10.01");}
 @Test void compraNoFechamentoEntraNaCompetenciaCorrente(){assertThat(service.primeiraCompetencia(LocalDate.of(2026,7,5),5,12)).isEqualTo(LocalDate.of(2026,7,12));}
 @Test void compraDepoisDoFechamentoEntraNaCompetenciaSeguinte(){assertThat(service.primeiraCompetencia(LocalDate.of(2026,7,6),5,12)).isEqualTo(LocalDate.of(2026,8,12));}
 @Test void fechamentoInexistenteUsaUltimoDiaDoMes(){assertThat(service.primeiraCompetencia(LocalDate.of(2026,2,28),31,31)).isEqualTo(LocalDate.of(2026,2,28));}
 @Test void competenciasPreservamDiaOuUltimoValido(){assertThat(service.competencias(LocalDate.of(2026,1,31),3)).containsExactly(LocalDate.of(2026,1,31),LocalDate.of(2026,2,28),LocalDate.of(2026,3,31));}
}
