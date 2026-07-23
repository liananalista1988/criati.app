package br.app.criati.financeiro.service;
import java.math.*; import java.time.*; import java.util.*; import org.springframework.stereotype.Service;
@Service public class ParcelamentoCartaoService {
 public List<BigDecimal> dividir(BigDecimal total,int quantidade){if(total==null||total.signum()<=0||quantidade<1)throw new IllegalArgumentException("Parcelamento invalido");BigDecimal valor=total.setScale(2,RoundingMode.HALF_UP);BigDecimal base=valor.divide(BigDecimal.valueOf(quantidade),2,RoundingMode.DOWN);List<BigDecimal> r=new ArrayList<>();for(int i=1;i<quantidade;i++)r.add(base);r.add(valor.subtract(base.multiply(BigDecimal.valueOf(quantidade-1))).setScale(2,RoundingMode.HALF_UP));return List.copyOf(r);}
 public LocalDate primeiraCompetencia(LocalDate compra,int diaFechamento,int diaVencimento){YearMonth ciclo=YearMonth.from(compra);LocalDate fechamento=ciclo.atDay(Math.min(diaFechamento,ciclo.lengthOfMonth()));if(compra.isAfter(fechamento))ciclo=ciclo.plusMonths(1);return ciclo.atDay(Math.min(diaVencimento,ciclo.lengthOfMonth()));}
 public List<LocalDate> competencias(LocalDate primeira,int quantidade){List<LocalDate> r=new ArrayList<>();YearMonth inicio=YearMonth.from(primeira);int dia=primeira.getDayOfMonth();for(int i=0;i<quantidade;i++){YearMonth mes=inicio.plusMonths(i);r.add(mes.atDay(Math.min(dia,mes.lengthOfMonth())));}return List.copyOf(r);}
}
