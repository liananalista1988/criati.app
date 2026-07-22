# Implementação LES-F3-001 — Cartões de crédito, limites e cartões virtuais

## Diagnóstico e reaproveitamento

Commit de referência (`c4671a0`, LES-F2-007): módulo financeiro com conta, categoria, lançamento,
recorrência e contas a pagar completos. Inventário realizado antes de qualquer alteração:

- **Nenhum conceito de cartão de crédito existia** em código (`br.app.criati.**`): busca completa por
  "cartao"/"credito"/"limite"/"bandeira"/"fechamento"/"fatura"/"virtual" não encontrou nenhuma entidade,
  tabela, repository ou service — apenas usos incidentais não relacionados (`FormaPagamentoLancamento.CREDITO`,
  `OrigemLancamentoFinanceiro.CARTAO`/`.FATURA`, ambos tags de origem de um lançamento, sem entidade de
  cartão por trás) e propostas conceituais em `docs/empresas/financeiro-les/MODELO-DE-DADOS.md`
  (`CartaoCredito`, `Bandeira`, `CompraCredito`, `Fatura` — explicitamente não implementadas ali).
- **`InstituicaoFinanceira`** (LES-F2-003) já implementa exatamente o padrão de "catálogo global com
  extensão por empresa" (`empresa_id` nulo = global, seed `Banco do Brasil`/`Banco Inter`) que
  `MODELO-DE-DADOS.md` já propunha replicar para uma futura `Bandeira` — decisão de **não** replicar esse
  padrão para bandeira está registrada abaixo, com justificativa.
- **`ContaFinanceira`** é o análogo estrutural mais próximo de um cartão (referencia `PessoaFinanceira`
  como titular e `InstituicaoFinanceira`, com o mesmo padrão de auditoria `@Generated(EventType.INSERT)` +
  `@ColumnDefault("CURRENT_TIMESTAMP")`): `CartaoCredito` foi modelado seguindo exatamente essa mesma
  convenção (mesmo padrão de validação titular-ativo-ou-inalterado, mesmo gate `exigirAdministrador`).
- **`CategoriaFinanceira.categoriaPai`** é o precedente exato para a autorreferência
  `cartao_principal_id` (self-FK opcional, `CHECK ... <> id`, validação de mesmo tenant e "profundidade
  máxima 2 níveis" no service) — reaproveitado sem alterações de padrão.
- **Nenhuma entidade possui um eixo de bloqueio independente de `StatusCadastro`** (ativo/inativo): esta é
  a única lacuna genuinamente nova neste domínio, resolvida com um campo `bloqueado` (boolean) + 
  `motivoBloqueio` (opcional) ortogonais ao `status` já existente — não reaproveita nem estende
  `StatusCadastro`, que continua representando só ativo/inativo em todo o módulo.
- **Nenhum novo dependency foi adicionado**; nenhum teste anterior foi alterado (exceto a extensão pontual
  descrita na seção "Compatibilidade").

## Bandeira: enum, não tabela de domínio — decisão e justificativa

O enunciado permitia enum, catálogo global ou tabela de domínio. **Decisão: enum Java simples**
(`br.app.criati.shared.enums.Bandeira`: `VISA, MASTERCARD, ELO, AMERICAN_EXPRESS, HIPERCARD, OUTRA`),
**não** uma tabela mirando `InstituicaoFinanceira`, apesar de essa ser a proposta registrada em
`MODELO-DE-DADOS.md`. Motivo, documentado explicitamente por divergir da proposta anterior: instituições
financeiras genuinamente variam por família/empresa (um cooperativismo local, uma fintech nova) e por isso
`InstituicaoFinanceira` precisa de cadastro local (`criarLocal`) — bandeiras de cartão, ao contrário, são um
conjunto pequeno e globalmente padronizado que **nunca** varia por empresa; o próprio enunciado reforça isso
("não vincular regras financeiras à bandeira"). Uma tabela de domínio para um enum fechado sem nenhuma regra
de negócio anexada seria arquitetura em excesso para o problema — o valor `OUTRA` já cobre qualquer bandeira
não listada, dando a mesma extensibilidade prática de um catálogo sem o custo de uma tabela, repository,
service e migration de seed adicionais. `TipoCartao` (`FISICO`/`VIRTUAL`) segue o mesmo estilo de enum
simples já usado por `TipoContaFinanceira`/`TipoFinanceiro`.

## Conceito de cartão e tipos

`CartaoCredito` representa uma linha de crédito concedida por uma instituição financeira — nunca uma conta
bancária, sem saldo. Esta entrega implementa **apenas** a estrutura cadastral: titular, instituição,
bandeira, limite, fechamento/vencimento e o vínculo físico/virtual. Compras, parcelas, faturas e pagamento
de fatura ficam fora de escopo, conforme o enunciado.

`TipoCartao`: `FISICO` (cartão principal tradicional) e `VIRTUAL` (sempre vinculado a um `FISICO`). A tabela
é única para os dois tipos (mesmo padrão de `ContaFinanceira`/`CategoriaFinanceira`, que também usam uma
única tabela por conceito, nunca duas tabelas para variantes do mesmo conceito).

## Titularidade, instituição e bandeira

Todo cartão (físico ou virtual) referencia `titular` (`PessoaFinanceira`, obrigatório), `instituicao`
(`InstituicaoFinanceira`, obrigatória) e `bandeira` (obrigatória) como **campos próprios de cada linha**,
não delegados ao principal — diferente do limite (ver seção seguinte). Validação de titular mirra
exatamente `ContaFinanceiraService.validarDados`: `pessoaRepository.findByIdAndEmpresaId` (ID de outro
tenant é tratado como "não encontrado", nunca 403 revelador), titular deve estar ativo **a menos que** seja
o mesmo titular já vinculado ao cartão em edição (mesma exceção usada por `ContaFinanceiraService` para
permitir editar outros campos de um cadastro cujo titular foi desativado depois). Instituição usa
`InstituicaoFinanceiraService.buscarDisponivel` (já existente, reaproveitado sem alteração), que já resolve
tanto instituições globais quanto locais da empresa.

**Cartão virtual: herança por padrão, não delegação obrigatória.** Ao criar um cartão virtual, se
`titularId`/`instituicaoId`/`bandeira` não forem informados na requisição, o service copia esses valores do
cartão principal (`CartaoCreditoService.criar`) — mas o valor é gravado normalmente na própria linha do
virtual e pode divergir do principal se informado explicitamente (o enunciado permite isso: "Caso se
permita titular diferente em cartão virtual, justificar e testar" — aqui a divergência é permitida por
constar como um valor comum, editável, não uma exceção de sistema). Isso é deliberadamente diferente do
tratamento do limite/fechamento/vencimento, que **nunca** podem divergir (ver próxima seção).

## Segurança de dados sensíveis

Regra obrigatória cumprida: **nunca** é armazenado número completo do cartão, CVV, senha ou data de
validade. O único dado de identificação parcial é `ultimosQuatroDigitos` (opcional, `VARCHAR(4)`),
validado por `CartaoCreditoService.validarUltimosQuatroDigitos` com regex `\d{4}` (exatamente 4 dígitos
numéricos, sem espaços/letras) e reforçado por `CHECK ck_cartao_credito_ultimos_digitos ... ~ '^[0-9]{4}$'`
na migration. Nenhum dado sensível aparece em log (a aplicação não loga corpo de requisição/entidades) nem
em nenhum teste (todos os últimos-4-dígitos de teste são valores fictícios como `1234`/`9999`).

## Limite total, limite saudável, fechamento e vencimento — sempre delegados no virtual

Diferente de titular/instituição/bandeira, estes quatro campos são **fisicamente nulos** na linha de um
cartão virtual (colunas nullable, nunca preenchidas pelo service quando `tipo = VIRTUAL`) — o valor efetivo
vem sempre de `CartaoCredito.getLimiteTotalEfetivo()`/`getLimiteSaudavelEfetivo()`/
`getDiaFechamentoEfetivo()`/`getDiaVencimentoEfetivo()`, que delegam ao `cartaoPrincipal` quando
`ehVirtual()`. Isso torna estrutural e à prova de esquecimento a regra "cartão virtual não gera limite
independente por padrão" — não é apenas uma convenção de UI, é impossível gravar um limite próprio em um
virtual através do fluxo normal do service. `BigDecimal`, escala 2, `HALF_UP`
(`CartaoCreditoService.validarValoresEstruturais`). Limite total exigido (pode ser zero) para cartão físico;
limite saudável opcional, nunca pode superar o limite total; fechamento e vencimento exigidos (1–31) para
cartão físico e devem ser dias distintos entre si — a ordem cronológica (vencimento após fechamento) é uma
preferência do enunciado, **não** validada de forma rígida, porque um dia-do-mês isolado não determina
ordem cruzando a virada do mês (ex.: fechamento dia 28, vencimento dia 5 do mês seguinte é um ciclo
perfeitamente válido e comum). Alterações de limite/fechamento/vencimento só afetam a configuração vigente,
nunca recalculam datas históricas — não existe histórico de fatura nesta entrega para recalcular.

## Ciclo do cartão (documentado, não implementado)

```
Data da compra → ciclo de fechamento → fatura correspondente → vencimento
```

Compras após o fechamento pertencerão à fatura seguinte. A alocação em fatura não é implementada nesta
tarefa — apenas os dados estruturais (`diaFechamento`/`diaVencimento`) estão preparados para uma tarefa
futura de faturas.

## Cartão principal e cartão virtual — vínculo e regras

`cartaoPrincipal` é uma autorreferência opcional (`@ManyToOne` para `CartaoCredito`, mesmo padrão de
`CategoriaFinanceira.categoriaPai`). Regras aplicadas em `CartaoCreditoService.resolverPrincipal`:

- cartão `FISICO` nunca pode ter `cartaoPrincipalId` informado (`DadosInvalidosException`, 400).
- cartão `VIRTUAL` exige `cartaoPrincipalId` (400 se ausente).
- o principal referenciado deve existir na mesma empresa (`buscarDaEmpresa`, 404 se de outro tenant),
  deve ser `FISICO` (`ehPrincipal()`) — rejeita apontar para outro `VIRTUAL`, prevenindo ciclos e
  profundidade além de 2 níveis por construção (um virtual nunca pode ser `cartaoPrincipal` de outro,
  porque só cartões `FISICO` passam na validação `ehPrincipal()`) — e deve estar `ATIVO` no momento do
  vínculo (checado apenas na criação, não reavaliado depois, pois o vínculo é imutável após criado:
  `cartaoPrincipalId` não faz parte dos parâmetros de `editar`).
- `cartao_principal_id <> id` é reforçado também em banco (`ck_cartao_credito_principal_diferente`).

**Bloqueio do principal e efeito nos virtuais.** Bloquear o cartão principal **não** altera silenciosamente
o campo `bloqueado` de nenhum cartão virtual vinculado — em vez disso, `CartaoCredito.estaBloqueadoEfetivo()`
retorna `bloqueado || (cartaoPrincipal != null && cartaoPrincipal.isBloqueado())`, refletindo o bloqueio
efetivo sem jamais escrever no registro do virtual. A API expõe tanto `bloqueado` (campo próprio) quanto
`bloqueadoEfetivo` (campo calculado) para que a interface possa distinguir "este cartão foi bloqueado
diretamente" de "este cartão está indisponível porque o principal foi bloqueado" — exatamente a preferência
do enunciado ("impedir uso e exibir alerta, sem alterar status silenciosamente").

## Limite comprometido e limite disponível

Como não existem compras nesta entrega, `getLimiteComprometidoEfetivo()` retorna sempre `0.00`
(`BigDecimal`, escala 2) — não é um campo persistido, é um método calculado, deixando claro na API que o
valor é estrutural/provisório e será substituído por um cálculo real quando compras existirem.
`getLimiteDisponivelEfetivo() = getLimiteTotalEfetivo() - getLimiteComprometidoEfetivo()` — hoje sempre
igual ao limite total. Ambos os métodos já delegam ao principal quando o cartão é virtual, então o
disponível de um virtual também nunca duplica o do principal.

## Consolidado

`CartaoCreditoService.resumir` soma limite total/saudável/disponível **apenas sobre cartões `FISICO`**
(`filter(CartaoCredito::ehPrincipal)`) — cartões virtuais nunca entram na soma, eliminando por construção o
risco de duplicar o limite concedido. Quantidades (`ativos`, `fisicos`, `virtuais`, `bloqueados`) são
contadas separadamente sobre todos os cartões ativos. `porTitular`/`porInstituicao` contam a quantidade de
cartões (físicos + virtuais) por titular/instituição, para visão de distribuição — não são somas de limite,
apenas contagem, então não há risco de dupla contagem de valor ali.

## Ativação, desativação e bloqueio — dois eixos independentes

`status` (`StatusCadastro`: `ATIVO`/`INATIVO`, reaproveitado sem alteração) representa
ativação/desativação: desativar preserva o histórico e permite reativação (mesmo padrão de
`ContaFinanceira`/`CategoriaFinanceira`); reativar um cartão cujo titular está inativo é rejeitado
(`PessoaFinanceiraInativaException`, mesmo comportamento de `ContaFinanceiraService.reativar`).

`bloqueado` (boolean) + `motivoBloqueio` (opcional) é um eixo **novo neste domínio** — nenhuma entidade
anterior do módulo tinha um conceito de bloqueio distinto de ativo/inativo. Bloquear/desbloquear não afeta
`status`; desativar/reativar não afeta `bloqueado`. Desativar um cartão principal não desativa nem
desbloqueia seus virtuais (nenhuma cascata automática) — apenas o bloqueio tem o efeito "herdado" via
`estaBloqueadoEfetivo()`, e mesmo esse efeito nunca escreve no registro do virtual.

## Duplicidade — alerta, nunca bloqueio automático

`possuiPossivelDuplicidade` segue o mesmo padrão de `ContaFinanceiraService.possuiPossivelDuplicidade`:
verifica `empresa + titular + instituição + últimos 4 dígitos + tipo + status ATIVO`, excluindo o próprio
registro (`IdNot`) — expõe um booleano informativo na resposta, nunca impede a criação/edição. Só é avaliada
quando `ultimosQuatroDigitos` está preenchido (sem os 4 dígitos não há base de comparação significativa).

## Multiempresa

Toda consulta usa `empresaId` do `ContextoEmpresaAtual` da sessão. `empresaId` nunca é aceito do cliente.
`findByIdAndEmpresaId` em todo lookup (cartão, titular, instituição, cartão principal); um ID de outro
tenant resulta em 404 (`CartaoCreditoNaoEncontradoException`/`PessoaFinanceiraNaoEncontradaException`/etc.),
nunca revela a existência do recurso em outra empresa. Testado em `CartaoCreditoControllerTests`
(`listaFiltraPorTitularTipoStatusEBloqueadoSemVazarTenant`).

## Migration `V12__criar_cartoes_credito.sql`

Cria `cartao_credito` com FKs para `empresa`, `pessoa_financeira` (titular), `instituicao_financeira`,
autorreferência para `cartao_principal_id` e para `usuario` (auditoria); checks para tipo, bandeira, status,
limites não negativos, limite saudável ≤ limite total, fechamento/vencimento entre 1 e 31, últimos-4-dígitos
via regex, `cartao_principal_id <> id` e a regra estrutural "virtual exige principal / físico não pode ter
principal" (`ck_cartao_credito_virtual_exige_principal`). Índices por empresa, empresa+titular,
empresa+instituição, empresa+status, empresa+bloqueado, empresa+principal, empresa+tipo. `V1`–`V11`
permanecem intocadas.

## Endpoints

`GET/POST/PUT /api/contexto/financeiro/cartoes`, `GET /{id}`, `GET /{id}/virtuais`, `GET /resumo`,
`POST /{id}/inativar`, `POST /{id}/reativar`, `POST /{id}/bloquear`, `POST /{id}/desbloquear`. Todos exigem
sessão autenticada + empresa ativa + aplicação `FINANCEIRO` habilitada (`ContextoFinanceiroService`, mesmo
portão único do módulo). Escrita exige perfil `ADMINISTRADOR` (mesmo precedente mais restrito de
`ContaFinanceiraService`/`InstituicaoFinanceiraService` — cartão de crédito é tratado com a mesma
sensibilidade de uma conta bancária, não com o padrão mais permissivo `ADMINISTRADOR`/`GESTOR` usado por
recorrências/contas a pagar). CSRF via meta tag + `criati-api.js`, igual ao resto da aplicação.

## Interface

`GET /app/financeiro/cartoes` (`PaginaController.financeiroCartoes`, mesmo guard das demais páginas
financeiras). Lista com filtros (titular, instituição, tipo, situação, bloqueio, busca), cards de resumo
consolidado, ações (Editar, Virtuais, Bloquear/Desbloquear, Desativar/Reativar). Formulário: ao selecionar
tipo Virtual, exige e mostra o campo "cartão principal", oculta limite/fechamento/vencimento (marcados
`hidden`, não apenas desabilitados) e exibe o aviso "Cartões virtuais compartilham o mesmo limite do cartão
principal e não aumentam o limite consolidado." Selecionar um cartão principal pré-preenche
titular/instituição/bandeira a partir dele (conveniência de UI; os campos continuam editáveis). Estado
vazio: "Nenhum cartão cadastrado. Cadastre os cartões da residência para acompanhar limites, fechamento e
vencimento." — testado em `PaginaFinanceiroSegurancaTests`.

## Compatibilidade

Nenhuma entidade, migration, endpoint ou rota existente foi alterada de forma incompatível. Nenhuma compra
ou fatura foi criada ou antecipada. `ContaFinanceira`, `LancamentoFinanceiro`, `RecorrenciaFinanceira`,
`CompromissoFinanceiro`/`OcorrenciaCompromisso`/`PagamentoOcorrenciaCompromisso` e o dashboard financeiro
não precisaram de nenhuma alteração — `CartaoCredito` é inteiramente aditivo, sem nenhum ponto de
integração com o restante do módulo nesta entrega (a integração acontecerá quando compras/faturas forem
implementadas em tarefa futura).

## Testes

- **Domínio puro** (`CartaoCreditoTests`, 13 testes): criação de cartão físico e virtual, cálculo dos
  métodos "Efetivo" (limite total/saudável/fechamento/vencimento delegados ao principal quando virtual),
  limite comprometido sempre zero, limite disponível igual ao total (físico e virtual), bloqueio do
  principal refletindo no efetivo do virtual sem alterar seu campo próprio, bloquear/desbloquear,
  inativar/reativar, validação de campos obrigatórios (nome/titular/instituição/bandeira nulos).
- **JPA/persistência** (`CartaoCreditoJpaTests`, 2 testes): persistência de cartão físico com auditoria,
  persistência de cartão virtual com vínculo real ao principal via FK e `countByCartaoPrincipalId`.
- **Web/controller** (`CartaoCreditoControllerTests`, 18 testes): criação de cartão físico com limite e
  datas, criação de virtual herdando dados do principal quando omitidos, resumo consolidado sem duplicar
  limite do virtual, físico não pode ter principal, virtual exige principal, principal deve ser físico
  (rejeita virtual apontando para virtual), limite saudável acima do total rejeitado, últimos-4-dígitos
  inválidos rejeitados, fechamento igual ao vencimento rejeitado, bloqueio/desbloqueio, bloqueio do
  principal refletindo no efetivo do virtual via API, inativação/reativação, filtros com isolamento de
  tenant (404 cruzado, titular/instituição de outro tenant rejeitados), listagem de virtuais de um
  principal, autenticação obrigatória, CSRF obrigatório, perfil `GESTOR` sem permissão de escrita, estado
  vazio.
- **Página** (`PaginaFinanceiroSegurancaTests`, ajustado): rota `/app/financeiro/cartoes` incluída nos três
  cenários já existentes (anônimo redireciona, sem aplicação habilitada redireciona, com aplicação
  habilitada renderiza e mostra o estado vazio esperado).

Total: **33 testes novos** (13 domínio + 2 JPA + 18 web, mais o ajuste pontual do teste de página), nenhum
teste dos 543 anteriores alterado ou removido.

## Limitações

- **Validação em PostgreSQL real**: não realizada. Nem `psql`, `pg_ctl` nem `docker` estão disponíveis
  neste ambiente (confirmado por tentativa direta, mesma limitação já registrada em
  `IMPLEMENTACAO-F2-007.md`). A migration segue rigorosamente as convenções já validadas em `V1`–`V11`
  (mesmos tipos, mesmo padrão de `CHECK`/`FOREIGN KEY`, mesmo padrão de autorreferência de
  `V8`/`categoria_financeira`), mas isso não substitui uma execução real contra Postgres.
- **`@Generated(event = EventType.INSERT)` em `atualizado_em`**: ao editar/bloquear/desativar um cartão,
  o teste observou o aviso do Hibernate `HHH000502` ("[atualizadoEm] ... won't be updated because the
  property is immutable") nos logs. Esse comportamento já existe hoje em `ContaFinanceira`/
  `CategoriaFinanceira` (mesma anotação, copiada fielmente por consistência com o análogo mais próximo,
  conforme decisão desta tarefa) — não é uma regressão introduzida aqui, é uma característica pré-existente
  do padrão de auditoria Pattern A já em uso no módulo. Nenhum teste desta entrega depende do valor exato
  de `atualizadoEm` após uma atualização, então isso não bloqueia a entrega, mas fica registrado para uma
  eventual revisão futura do padrão de auditoria do módulo como um todo (fora do escopo desta tarefa).
- **Bandeira não editável centralmente**: por ser um enum Java, adicionar uma nova bandeira exige alterar
  código e migration (`ck_cartao_credito_bandeira`), não um cadastro em tela. Aceitável dado que o conjunto
  de bandeiras é público e estável; documentado como trade-off consciente da decisão "enum, não tabela".
- **Sem compras, parcelas, faturas, pagamento mínimo, juros, encargos, estorno de compra, assinatura,
  orçamento, meta, empréstimo, recebível, simulador, conciliação, importação ou notificações** — todos
  fora de escopo desta tarefa, conforme o enunciado.

## Impacto na próxima tarefa

- Uma tarefa futura de compras/parcelas deve introduzir um novo agregado (ex.: `CompraCredito`) que
  referencia `CartaoCredito` e passa a alimentar `getLimiteComprometidoEfetivo()` com um valor real — hoje
  esse método é um placeholder que sempre retorna zero e deverá ser substituído (não apenas complementado)
  quando compras existirem.
- Uma tarefa futura de faturas deve usar `diaFechamento`/`diaVencimento` (já validados e armazenados) para
  alocar compras em ciclos — a lógica de "ciclo do cartão" descrita nesta entrega é apenas documentação,
  ainda não implementada.
- Se uma extensão residencial específica (`br.app.criati.financeiro.les`) precisar de conceitos adicionais
  de cartão (limite por bandeira, cartão adicional com titular diferente do responsável pela fatura etc.),
  ela deve reaproveitar `CartaoCredito` como base, não recriar o conceito de cartão físico/virtual.
