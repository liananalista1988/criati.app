package br.app.criati.financeiro.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.StatusCompraCartao;
import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @Entity @Table(name = "compra_cartao")
public class CompraCartao {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="empresa_id",updatable=false) private Empresa empresa;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cartao_id",updatable=false) private CartaoCredito cartao;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="cartao_principal_id",updatable=false) private CartaoCredito cartaoPrincipal;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="pessoa_responsavel_id",updatable=false) private PessoaFinanceira pessoaResponsavel;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="categoria_id",updatable=false) private CategoriaFinanceira categoria;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parte_financeira_id",updatable=false) private ParteFinanceira parteFinanceira;
 @Column(nullable=false,length=200) private String descricao;
 @Column(name="data_compra",nullable=false) private LocalDate dataCompra;
 @Column(name="valor_total",nullable=false,precision=19,scale=2) private BigDecimal valorTotal;
 @Column(name="quantidade_parcelas",nullable=false) private int quantidadeParcelas;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private StatusCompraCartao status;
 @Column(length=500) private String observacao;
 @Column(name="motivo_cancelamento",length=500) private String motivoCancelamento;
 @Column(name="cancelado_em") private OffsetDateTime canceladoEm;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cancelado_por_usuario_id") private Usuario canceladoPor;
 @Column(name="motivo_estorno",length=500) private String motivoEstorno;
 @Column(name="estornado_em") private OffsetDateTime estornadoEm;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="estornado_por_usuario_id") private Usuario estornadoPor;
 @Column(name="criado_em",nullable=false,updatable=false) private OffsetDateTime criadoEm;
 @Column(name="atualizado_em",nullable=false) private OffsetDateTime atualizadoEm;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="criado_por_usuario_id",updatable=false) private Usuario criadoPor;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="atualizado_por_usuario_id") private Usuario atualizadoPor;
 @OneToMany(mappedBy="compra",cascade=CascadeType.PERSIST,orphanRemoval=false) @OrderBy("numero") private List<ParcelaCompraCartao> parcelas=new ArrayList<>();
 public CompraCartao(Empresa e,CartaoCredito c,CartaoCredito p,PessoaFinanceira r,CategoriaFinanceira cat,ParteFinanceira parte,String d,LocalDate data,BigDecimal valor,int qtd,String obs,Usuario autor){empresa=Objects.requireNonNull(e);cartao=Objects.requireNonNull(c);cartaoPrincipal=Objects.requireNonNull(p);pessoaResponsavel=Objects.requireNonNull(r);categoria=Objects.requireNonNull(cat);parteFinanceira=parte;descricao=Objects.requireNonNull(d);dataCompra=Objects.requireNonNull(data);valorTotal=Objects.requireNonNull(valor);quantidadeParcelas=qtd;observacao=obs;status=StatusCompraCartao.ATIVA;criadoEm=OffsetDateTime.now();atualizadoEm=criadoEm;criadoPor=autor;atualizadoPor=autor;}
 public void adicionarParcela(ParcelaCompraCartao p){parcelas.add(p);}
 public void cancelar(String motivo,Usuario autor){exigirAtiva();status=StatusCompraCartao.CANCELADA;motivoCancelamento=motivo;canceladoEm=OffsetDateTime.now();canceladoPor=autor;parcelas.forEach(x->x.cancelar(autor));alterar(autor);}
 public void estornar(String motivo,Usuario autor){exigirAtiva();status=StatusCompraCartao.ESTORNADA;motivoEstorno=motivo;estornadoEm=OffsetDateTime.now();estornadoPor=autor;parcelas.forEach(x->x.estornar(autor));alterar(autor);}
 private void exigirAtiva(){if(status!=StatusCompraCartao.ATIVA)throw new IllegalStateException("Compra nao esta ativa");}
 private void alterar(Usuario u){atualizadoPor=u;atualizadoEm=OffsetDateTime.now();}
}
