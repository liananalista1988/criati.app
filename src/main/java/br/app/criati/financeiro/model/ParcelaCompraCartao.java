package br.app.criati.financeiro.model;
import java.math.BigDecimal; import java.time.LocalDate; import java.time.OffsetDateTime; import java.util.Objects; import java.util.UUID;
import br.app.criati.empresa.model.Empresa; import br.app.criati.shared.enums.StatusParcelaCartao; import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.*; import lombok.AccessLevel; import lombok.Getter; import lombok.NoArgsConstructor;
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @Entity @Table(name="parcela_compra_cartao",uniqueConstraints=@UniqueConstraint(name="uk_parcela_compra_numero",columnNames={"compra_id","numero"}))
public class ParcelaCompraCartao {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="empresa_id",updatable=false) private Empresa empresa;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="compra_id",updatable=false) private CompraCartao compra;
 @Column(nullable=false) private int numero; @Column(name="total_parcelas",nullable=false) private int totalParcelas;
 @Column(nullable=false,precision=19,scale=2) private BigDecimal valor; @Column(nullable=false) private LocalDate competencia;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatusParcelaCartao status;
 @Column(name="fatura_id") private UUID faturaId;
 @Column(name="criado_em",nullable=false,updatable=false) private OffsetDateTime criadoEm; @Column(name="atualizado_em",nullable=false) private OffsetDateTime atualizadoEm;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="criado_por_usuario_id",updatable=false) private Usuario criadoPor;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="atualizado_por_usuario_id") private Usuario atualizadoPor;
 public ParcelaCompraCartao(Empresa e,CompraCartao c,int n,int total,BigDecimal v,LocalDate comp,Usuario u){empresa=e;compra=c;numero=n;totalParcelas=total;valor=v;competencia=comp;status=StatusParcelaCartao.ABERTA;criadoEm=OffsetDateTime.now();atualizadoEm=criadoEm;criadoPor=u;atualizadoPor=u;}
 public void associarFatura(UUID idFatura,Usuario u){if(status!=StatusParcelaCartao.ABERTA)throw new IllegalStateException("Parcela nao esta aberta");if(faturaId!=null&&!faturaId.equals(idFatura))throw new IllegalStateException("Parcela ja associada a outra fatura");faturaId=Objects.requireNonNull(idFatura);atualizadoPor=Objects.requireNonNull(u);atualizadoEm=OffsetDateTime.now();}
 public void cancelar(Usuario u){status=StatusParcelaCartao.CANCELADA;atualizadoPor=u;atualizadoEm=OffsetDateTime.now();}
 public void estornar(Usuario u){status=StatusParcelaCartao.ESTORNADA;atualizadoPor=u;atualizadoEm=OffsetDateTime.now();}
}
