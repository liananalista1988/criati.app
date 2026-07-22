# Convenções Técnicas — Financeiro LeS

## Pacotes e dependências

- `br.app.criati.financeiro.shared`: conceitos reutilizáveis pelo núcleo e pelas extensões.
- `br.app.criati.financeiro.les`: extensões residenciais; pode depender do núcleo compartilhado.
- Dentro de um módulo funcional, usar `model`, `repository`, `service` e `web` somente quando houver
  classes concretas. Não criar pacotes vazios para antecipar arquitetura.
- Controller chama service; service chama repository. Integração entre módulos ocorre por service
  ou contrato explícito, nunca pelo repository de outro módulo a partir de controller.
- O núcleo compartilhado nunca importa classes de `financeiro.les`.

## Nomes e identificadores

- Classes e identificadores usam português, preservando termos técnicos consolidados.
- Entidade usa singular (`ContaFinanceira`); tabela usa `snake_case` singular
  (`conta_financeira`); chave estrangeira usa `<recurso>_id`.
- Entidades principais usam `UUID` gerado no backend/banco conforme o padrão existente.
- DTOs são específicos por operação ou visão e não expõem entidade JPA diretamente.
- DTO recebido do cliente não contém `empresaId`, autor, status interno ou outro campo
  administrativo sem justificativa e validação explícita.

## Tenant e autorização

- Toda entidade empresarial possui `empresa_id NOT NULL`; catálogo global é exceção explícita e
  documentada.
- Empresa atual vem de `ContextoEmpresaService`/`ContextoEmpresaAtual`, nunca de URL, formulário,
  campo oculto, header ou JavaScript.
- O usuário autenticado vem de `UsuarioPrincipal`; vínculo, empresa e perfil são revalidados no
  backend. A aplicação ativa é exigida pelo portão do módulo.
- Busca individual usa `findByIdAndEmpresaId`; listagem usa `findAllByEmpresaId` ou consulta mais
  específica com `empresaId`. Um UUID alheio não revela a existência do recurso.
- Relacionamentos entre registros empresariais validam o mesmo tenant no service e, quando viável,
  por constraint composta no banco.
- Superadministrador não recebe contexto empresarial implícito e não contorna vínculo/perfil.

## Valores monetários

- Usar `BigDecimal`; `double` e `float` são proibidos para dinheiro.
- Persistir em `NUMERIC(19,2)` enquanto não houver decisão específica diferente.
- Normalizar entradas em escala 2 com `RoundingMode.HALF_UP`, padrão atual de `MoedaUtils`.
- Comparar por `compareTo`/`signum`, não por `equals` quando a escala não for relevante.
- Soma começa em `BigDecimal.ZERO` e mantém normalização explícita na fronteira de entrada/saída.
- Valor nulo é inválido quando obrigatório. Negativo, zero ou positivo depende da natureza: saldo
  inicial aceita os três; lançamento de receita/despesa atual exige valor positivo.
- Moeda inicial é BRL. Não criar multimoeda ou objeto monetário complexo sem requisito aprovado.

## Datas e competência

- Instantes técnicos usam `OffsetDateTime` e `TIMESTAMPTZ`; datas civis usam `LocalDate`/`DATE`.
- Competência mensal usa `CompetenciaFinanceira` ou `YearMonth` na integração legada, sem timezone.
- Representação humana padrão da competência: `MM/AAAA`; APIs existentes em `AAAA-MM` permanecem
  compatíveis até migração planejada.
- Competência, data do lançamento, vencimento, liquidação/pagamento, fechamento e criação são
  conceitos diferentes e não devem reutilizar o mesmo campo por conveniência.

## Status e enums

- Estado finito usa enum Java com `EnumType.STRING` e constraint `CHECK` na migration.
- Texto livre não representa status. Nomes de enum são estáveis, em maiúsculas e sem acento.
- Transições pertencem ao domínio/service, são validadas no backend e preservam histórico.
- Inativação, cancelamento e envio à lixeira são conceitos distintos quando a regra assim exigir.

## Auditoria e exclusão lógica

- Novas entidades relevantes preveem `criado_em`, `criado_por`, `atualizado_em` e `atualizado_por`,
  conforme `docs/BANCO_DE_DADOS.md`; autores vêm do contexto autenticado.
- Auditoria de negócio não é log técnico e nunca registra senha, token ou documento sensível.
- Registros excluíveis usam lixeira explícita, não `DELETE` casual. Envio, restauração e futura
  exclusão definitiva revalidam tenant, autorização e relacionamentos.
- Itens na lixeira não participam de cálculos ativos. A exclusão definitiva é manual no MVP.
- O padrão concreto será implementado com a primeira entidade que o consumir; não há interface
  genérica criada antecipadamente.

## Services, repositories e controllers

- Services concentram autorização, invariantes e `@Transactional`; leitura usa
  `@Transactional(readOnly = true)` quando apropriado.
- Repositories empresariais expressam `empresaId` nos métodos. Consultas globais são restritas a
  casos administrativos explícitos.
- Controllers permanecem pequenos, validam DTOs, obtêm contexto seguro e não acessam repository.
- Mensagens de validação são claras para o usuário e não expõem SQL, tabela, caminho ou outro
  tenant. O tratamento global existente deve ser reutilizado.

## Migrations

- Uma migration nova, pequena e descritiva por tarefa funcional coesa; nunca editar migration já
  aplicada.
- Usar UUID, `TIMESTAMPTZ`, `NUMERIC(19,2)`, `empresa_id`, FKs, checks, unicidade e índices
  necessários. Validar banco vazio e atualização da versão anterior com Hibernate `validate`.
- Estrutura e dados iniciais ficam separados. Nenhum dado familiar real entra em migration.
- Rollback é conceitual e preserva dados por nova migration corretiva quando necessário.

## Testes

- Nome de classe termina em `Tests`; métodos descrevem comportamento em português.
- Unitários cobrem value objects, cálculos, estados e invariantes sem Spring quando possível.
- Integração cobre JPA, migrations, transações e constraints; MockMvc cobre segurança, DTOs, rotas e
  páginas; navegador cobre jornadas críticas e responsividade.
- Toda nova entidade empresarial inclui o cenário: Empresa A usa UUID válido da Empresa B e não
  consulta nem altera o registro.
- Testar casos positivo, nulo, limite, inválido, autorização e transição; compilação isolada não
  conclui tarefa.

## Aplicação da extensão

O código reservado é `FINANCEIRO_RESIDENCIAL`. Ele representa uma extensão reutilizável para
clientes residenciais, não dados exclusivos da família LeS. Sua futura ativação exige também
`FINANCEIRO`, vínculo `EmpresaAplicacao`, autorização no backend, migration de catálogo e testes.
Não será habilitado automaticamente e não existe no catálogo até a tarefa funcional correspondente.
