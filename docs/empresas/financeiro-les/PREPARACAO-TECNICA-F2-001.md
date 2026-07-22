# Preparação Técnica — LES-F2-001

## Objetivo e escopo

Esta tarefa cria somente a fronteira técnica inicial do Financeiro LeS e um conceito compartilhado
de competência mensal. Não cria entidade JPA, tabela, migration, endpoint, página, menu, regra de
cartão, fatura, parcela ou outro caso de uso financeiro.

## Diagnóstico da arquitetura atual

### Organização e camadas

O projeto é um monólito modular por funcionalidade sob `br.app.criati`. Os módulos existentes
incluem `acesso`, `admin`, `aplicacao`, `convite`, `empresa`, `financeiro`, `security`, `tenant` e
`usuario`. Módulos com domínio persistente usam, conforme a necessidade, `model`, `repository`,
`service` e `web`; controllers chamam services e os services controlam autorização e transação.

O módulo `financeiro` atual contém:

- entidades `ContaFinanceira`, `CategoriaFinanceira` e `LancamentoFinanceiro`;
- repositories Spring Data com buscas empresariais por `id + empresaId`;
- services transacionais e regras de perfil;
- controllers e DTOs sob `web`;
- dashboard e cálculo de saldo;
- páginas, scripts e estilos já em produção no projeto;
- migration `V5__criar_estrutura_financeiro.sql` e testes JPA/MockMvc.

### Empresa, autenticação e autorização

`ContextoEmpresaService` é a fonte do contexto ativo. Ele revalida na sessão o usuário, a empresa,
o vínculo `UsuarioEmpresa`, o status da empresa e o perfil, retornando `ContextoEmpresaAtual`.
`ContextoFinanceiroService` acrescenta a exigência da aplicação `FINANCEIRO` ativa. O
Superadministrador sem vínculo empresarial continua sem acesso implícito ao Financeiro.

O frontend não define o tenant. Controllers obtêm o usuário autenticado, chamam o portão de
contexto e repassam `ContextoEmpresaAtual` aos casos de uso. Recursos empresariais são localizados
com o UUID e o `empresaId` do contexto; UUID alheio é tratado como inexistente.

### Persistência, auditoria e exclusão

As entidades financeiras atuais usam UUID, relação obrigatória com `Empresa`, `BigDecimal`, enums
persistidos como texto e `OffsetDateTime` para `criadoEm`/`atualizadoEm`. A auditoria existente é
somente temporal: ainda não há mecanismo comum para `criadoPor`/`atualizadoPor` nem trilha de
alterações completa. Inativação/cancelamento preservam parte do histórico, mas a lixeira restaurável
do Financeiro LeS ainda não foi implementada.

### Validação, erros e testes

DTOs aplicam Bean Validation e os services repetem invariantes críticas. Exceções de domínio são
traduzidas pelo `GlobalExceptionHandler` em respostas genéricas, sem stack trace. A suíte combina
testes unitários, JPA, service e MockMvc, incluindo acesso por UUID de outra empresa. Flyway possui
cinco migrations; Hibernate valida o esquema fora do perfil de testes.

## Estrutura criada

```text
br.app.criati.financeiro
├── model/repository/service/web  # módulo existente, preservado
├── shared                       # conceitos financeiros reutilizáveis
│   └── CompetenciaFinanceira
└── les                          # fronteira das extensões residenciais
```

`financeiro.shared` contém comportamento real e pode ser usado pelo núcleo e pela extensão.
`financeiro.les` possui nesta etapa apenas documentação de pacote, suficiente para declarar a
direção sem criar classes ou subpacotes cerimoniais. Novos subpacotes surgirão somente com uma
responsabilidade concreta.

## Fronteira e direção de dependências

O núcleo compartilhado abrange pessoa, parte financeira, conta, categoria, lançamento,
recorrência, anexo, auditoria e conceitos técnicos reutilizáveis. A extensão LeS abrange cartão,
fatura, parcela, orçamento, meta de economia, empréstimo, recebível, compra para terceiro,
simulador e conciliação.

Direção permitida:

```text
financeiro.les → financeiro.shared e serviços públicos do núcleo financeiro
```

Direção proibida:

```text
financeiro.shared ou núcleo existente → financeiro.les
```

Não foi adicionado teste arquitetural porque o projeto não possui ferramenta apropriada e a tarefa
proíbe acrescentar dependência somente para isso. A regra fica documentada e deverá ser verificada
em revisão; uma ferramenta poderá ser avaliada em decisão estrutural futura.

## Decisões de reaproveitamento

- Evoluir gradualmente as entidades atuais nas tarefas próprias; não duplicar conta, categoria ou
  lançamento em `financeiro.les`.
- Manter temporariamente os pacotes, rotas, páginas e contratos atuais sem renomeação.
- Reutilizar `ContextoEmpresaService`, `ContextoEmpresaAtual`, `ContextoFinanceiroService`,
  `AplicacaoService`, perfis e tratamento global de erros.
- Reservar o código de catálogo `FINANCEIRO_RESIDENCIAL` para a extensão. O código não é adicionado
  ao enum nem ao catálogo agora, pois ativá-lo exige migration/seed e uma entrega funcional própria.
  A extensão dependerá de `FINANCEIRO`, mas não será habilitada automaticamente com ele.
- Usar `CompetenciaFinanceira` para novos contratos mensais; adaptar persistência e APIs atuais
  apenas nas tarefas funcionais correspondentes, sem quebrar `dataCompetencia` ou `AAAA-MM` atuais.
- Manter `BigDecimal`, escala 2 e `HALF_UP` como convenção vigente. `MoedaUtils` poderá ser movido ou
  exposto de forma gradual quando surgir consumidor compartilhado; não foi duplicado.

## Pontos de acoplamento e dívida conhecida

- Services financeiros carregam `Empresa` via `EmpresaRepository`, criando dependência direta com
  o módulo `empresa`; isso é aceitável no monólito atual e não será abstraído sem caso de uso.
- Algumas consultas auxiliares (`existsByContaId`, `existsByCategoriaId`) não expressam tenant no
  nome. Hoje recebem IDs obtidos após busca empresarial, mas novos repositories deverão preferir o
  filtro explícito por empresa.
- Listagens carregam todos os registros da empresa e filtram em memória; paginação e filtros no
  banco serão tratados quando volume e tarefa justificarem.
- `atualizadoEm` não possui hoje mecanismo geral de atualização e faltam autores da auditoria.
- Status de cadastro/cancelamento não equivale à lixeira restaurável exigida para o domínio novo.
- `LancamentoFinanceiro.dataCompetencia` é uma data completa, enquanto novos conceitos mensais
  usarão `CompetenciaFinanceira`; a transição deve ser deliberada e compatível.

## Contrato técnico introduzido

`CompetenciaFinanceira` é um record imutável com ano e mês, intervalo de ano de 1 a 9999, mês de 1
a 12, igualdade estrutural, ordenação cronológica, conversão para `YearMonth` e formato
`MM/AAAA`. Ele não contém dia, horário ou timezone e não é uma entidade persistida.

Nenhum contrato genérico de entidade empresarial, exclusão lógica ou auditoria foi criado: sem uma
entidade consumidora, essas interfaces seriam abstrações especulativas e poderiam divergir do
modelo físico que será decidido nas próximas tarefas.

## Limitações e impacto nas próximas tarefas

LES-F2-002 pode usar a fronteira compartilhada para Pessoa e ParteFinanceira, mas ainda deverá
definir sua migration, auditoria concreta, exclusão/lixeira, repositories empresariais e testes de
acesso cruzado. Antes de disponibilizar funcionalidades residenciais, uma tarefa própria deverá
incluir `FINANCEIRO_RESIDENCIAL` no catálogo com migration e autorização compatível.

Permanecem fora desta entrega: conciliação dos novos tipos de conta, modelo físico completo de
auditoria/lixeira, teste automatizado de arquitetura e migração do contrato de competência já
existente. Nenhuma dessas limitações quebra o Financeiro atual.
