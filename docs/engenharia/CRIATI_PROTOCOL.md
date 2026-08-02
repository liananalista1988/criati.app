# CRIATI Protocol — Protocolo operacional multiagente

Tarefa de origem: `CRIATI-OPS-001`.

## Finalidade

Este documento é a fonte central das regras operacionais para pessoas, GPT,
Codex, Claude Code e outros agentes que trabalhem no Criati.app. Ele reduz
repetição de contexto, consumo de tokens, trabalho duplicado e risco operacional.

`AGENTS.md` e `CLAUDE.md` são apenas pontos de entrada. Regras operacionais devem
ser alteradas aqui, sem cópias divergentes nesses arquivos.

## Precedência e conflitos

As instruções explícitas da tarefa definem o escopo. Este protocolo governa a
execução; os documentos oficiais governam produto, arquitetura, dados e
segurança. Nenhum agente pode redefinir silenciosamente uma decisão aprovada.

Antes de alterar código, banco, dependências, estrutura ou documentação técnica,
leia o que for aplicável:

- [Visão do Produto](../VISAO_DO_PRODUTO.md);
- [Decisões](../DECISOES.md);
- [Arquitetura](../ARQUITETURA.MD);
- [Modelo Multiempresa](../MODELO_MULTIEMPRESA.md);
- [Escopo do MVP](../ESCOPO_MVP.md);
- [Banco de Dados](../BANCO_DE_DADOS.md);
- [Segurança](../SEGURANCA.MD);
- [Roadmap](../ROADMAP.md);
- documentação específica do módulo afetado.

Se houver contradição, ambiguidade estrutural ou risco de sobrescrever uma
instrução importante, pare antes de editar e solicite decisão.

## Princípios de execução

- Trabalhe em modo econômico, seguro e compacto.
- Faça somente o escopo solicitado; melhorias adicionais devem ser propostas
  separadamente.
- Preserve mudanças existentes do usuário e de outros agentes.
- Prefira inspeção objetiva, alterações pequenas e validação proporcional ao
  risco.
- Não esconda erros, não masque testes e não declare sucesso sem evidência.
- Código e identificadores permanecem em português, salvo termos técnicos
  consolidados. Documentação, comunicação e commits usam português claro.
- Nunca exponha credenciais, tokens, dados pessoais, conteúdo bancário ou outros
  dados sensíveis em código, fixtures, logs, commits ou relatórios.

## Git e operações externas

- Nunca altere diretamente a branch `main`.
- Confirme branch, `HEAD`, upstream e `git status` antes de iniciar.
- Use branch e, quando houver trabalho concorrente, worktree próprios.
- Não use force push.
- Não faça merge, push, abra PR ou produza outra alteração externa sem
  autorização explícita.
- Não crie commit sem autorização explícita. Quando a tarefa autorizar commit,
  implemente, teste, audite e comite no mesmo ciclo.
- Não use `git reset --hard`, descarte, limpeza, stash ou operação destrutiva sem
  autorização e sem confirmar exatamente o alvo.
- Commits devem ser pequenos, coesos e descritivos; não misture tarefas nem
  alterações incidentais.
- Antes de concluir, execute `git diff --check`, revise o diff e confirme o
  estado final do worktree.

## Protocolo multiagente

### Responsabilidade por agente

- GPT e Codex entram por `AGENTS.md`; Claude Code entra por `CLAUDE.md`. Ambos os
  arquivos obrigam a leitura integral deste protocolo antes de qualquer tarefa.
- Todos os agentes obedecem às mesmas regras. Nenhum modelo possui autorização
  especial para relaxar segurança, Git, testes, escopo ou decisões aprovadas.
- A responsabilidade é definida pela tarefa, não pelo modelo: o agente de
  implementação altera apenas sua branch/worktree; o agente de integração aplica
  commits já auditados; o agente de auditoria não corrige nem comita sem
  autorização explícita.
- Um único agente deve ser o responsável ativo por tarefa, branch/worktree e
  conjunto de arquivos. Outro agente só assume após uma passagem de trabalho
  explícita ou ao receber uma tarefa independente e não sobreposta.

Antes de assumir uma tarefa, cada agente deve:

1. conferir branch, `HEAD`, worktrees, status e commits recentes;
2. identificar branch, worktree, tarefa e arquivos sob sua responsabilidade;
3. verificar se a mudança já existe em outro commit ou branch;
4. evitar editar o mesmo worktree ou os mesmos arquivos simultaneamente com
   outro agente;
5. não reaplicar nem reimplementar uma correção já existente.

Na passagem de trabalho entre agentes, informe no mínimo: identificador da
tarefa, branch/worktree, `HEAD` base, arquivos alterados, testes executados,
pendências, riscos e hash do commit, quando houver. O agente receptor deve
comparar commits e diff antes de integrar.

Conflitos só podem ser resolvidos quando a intenção de ambos os lados estiver
plenamente compreendida. Pare diante de conflito estrutural ou de autoria
incerta.

## Ciclo padrão de uma tarefa

1. **Diagnosticar:** ler a tarefa e os documentos relevantes; confirmar Git e
   arquivos relacionados; registrar riscos e suposições.
2. **Delimitar:** declarar resultado esperado, arquivos prováveis, validações e
   critérios de parada.
3. **Implementar:** alterar somente o necessário, preservando arquitetura,
   segurança e trabalho alheio.
4. **Testar durante a implementação:** executar testes focados a cada incremento
   relevante e corrigir a causa real das falhas.
5. **Validar integralmente:** executar a suíte completa antes do commit quando a
   natureza da mudança puder afetar a aplicação. Mudanças exclusivamente
   documentais exigem validação de links, referências, diff e escopo.
6. **Auditar:** revisar segurança, multiempresa, dinheiro, dados, logs,
   migrations, dependências, escopo e arquivos inesperados.
7. **Corrigir e retestar:** repetir as validações afetadas e a suíte necessária.
8. **Comitar, se autorizado:** criar um único commit coeso, salvo instrução
   diferente.
9. **Relatar:** entregar resultado compacto e verificável.

## Arquitetura e escopo

- O projeto é `Criati`, group `br.app`, artifact `criati`, package base
  `br.app.criati`, em Java 17 e Maven. O backend usa Spring Boot 3.x; o frontend
  usa Thymeleaf, HTML, CSS e JavaScript; o banco é PostgreSQL; a arquitetura é um
  monólito modular.
- Preserve PostgreSQL, Flyway, UUID, Hibernate `validate` e a stack acima.
- O fluxo padrão é `Controller → Service → Repository → Banco`.
- Controllers recebem e validam contratos, chamam serviços e não acessam
  repositories diretamente.
- Serviços concentram casos de uso, autorização, contexto empresarial e limites
  transacionais.
- DTO não é entidade e clientes não definem campos administrativos internos.
- Não adicione dependência estrutural, tecnologia, serviço pago ou mudança de
  arquitetura sem aprovação.
- Não altere identidade visual, módulos ou documentação aprovada fora do escopo.

### Persistência, contratos e erros

- Repositories empresariais consultam por identificador mais empresa atual;
  `findById` ou `findAll` sem escopo empresarial exigem justificativa administrativa
  explícita.
- Entidades principais usam UUID. Evite `FetchType.EAGER`, `CascadeType.ALL` e
  Lombok `@Data` sem análise; não inclua relacionamentos sensíveis em `toString`.
- Use `@Transactional` conscientemente e `readOnly = true` para leituras quando
  apropriado. Avalie `@Version` em fluxos concorrentes.
- Contratos usam DTOs específicos e validação de backend. O cliente não controla
  empresa, privilégios, status interno, autoria ou outros campos administrativos.
- Erros apresentados ao usuário não expõem stack trace, SQL, caminhos locais,
  tabelas, credenciais nem detalhes de outra empresa.

### Interface e integrações

- Thymeleaf mantém escape de HTML, evita `th:utext`, inclui CSRF em formulários e
  nunca é a única camada de autorização.
- JavaScript não armazena segredos nem duplica regras de negócio; validação no
  navegador nunca substitui validação no backend.
- E-mail nunca transporta senha. Tokens são aleatórios, temporários, de uso único
  e não aparecem integralmente em logs.
- Credenciais e configurações sensíveis usam variáveis de ambiente e nunca são
  versionadas.
- Antes de adicionar dependência, confirme necessidade, manutenção,
  compatibilidade, segurança e ausência de recurso equivalente já disponível.
- Mudança de comportamento relevante exige documentação consistente, sem alterar
  decisões aprovadas silenciosamente.

### Mudanças que exigem aprovação

Não introduza sem aprovação: microsserviços, banco por empresa, comunicação
distribuída, novo framework frontend, aplicativo móvel, gateway de pagamento,
integração com WhatsApp, inteligência artificial, Row-Level Security,
autenticação externa, domínio personalizado ou serviço pago. Uma proposta deve
explicar problema, alternativas, recomendação, impactos, riscos e esforço.

## Segurança e multiempresa

Toda operação empresarial deve derivar o contexto da sessão autenticada e
validar usuário, empresa atual, vínculo ativo, unidade, perfil, permissão, módulo
ativo e estados aplicáveis. Nunca confie em `empresa_id`, `unidade_id`, campos
ocultos, cabeçalhos ou JavaScript enviados pelo cliente.

- Consultas empresariais usam recurso mais `empresa_id` atual; UUID isolado não
  é autorização.
- Recurso de outra empresa não pode ter existência ou dados revelados.
- Negue acesso por padrão, mantenha CSRF e aplique autorização no backend e no
  caso de uso.
- Autenticação e sessão usam Spring Security. Senhas passam por
  `PasswordEncoder`, preferencialmente Argon2id; nunca são reversíveis, enviadas
  por e-mail ou registradas.
- Não registre senha, token, cookie, segredo, chave, documento integral nem
  conteúdo financeiro ou bancário bruto.
- Uploads validam nome, extensão, conteúdo, tamanho e empresa; arquivos não ficam
  em diretório público.
- Operações críticas exigem auditoria segura e sem conteúdo sensível
  desnecessário.
- Toda funcionalidade empresarial deve testar UUID válido pertencente a outra
  empresa e confirmar zero vazamento.

## Dados, dinheiro e migrations

- Valores monetários usam `BigDecimal`, escala e arredondamento explícitos.
- Regras financeiras não podem ser alteradas para satisfazer testes.
- Operações financeiras exigem atomicidade, idempotência e rastreabilidade
  proporcionais ao risco.
- Toda mudança estrutural usa uma nova migration Flyway pequena e cumulativa;
  migration já executada nunca é editada.
- Entidades principais usam UUID, tabelas empresariais usam `empresa_id` e
  instantes persistidos usam `TIMESTAMPTZ`.
- Migrations preservam dados e incluem constraints, chaves e índices necessários,
  inclusive os de isolamento multiempresa.
- Quando houver migration, valide todas as migrations em PostgreSQL real e banco
  vazio, confirme a versão esperada e o Hibernate `validate`.
- Não use H2 como substituto da validação PostgreSQL exigida.
- Não inclua dados reais ou de demonstração de clientes em migration ou fixture.

## Testes e definição de pronto

Testes devem ser proporcionais ao risco e cobrir, quando aplicável:

- regra de negócio, limites, erros e rollback;
- autenticação, autorização, CSRF e permissões;
- isolamento multiempresa com recurso válido de outra empresa;
- idempotência, concorrência e constraints;
- logs e respostas sem dados sensíveis;
- regressão dos fluxos relacionados.

No Windows, use `mvnw.cmd test`; em Linux ou macOS, `./mvnw test`. Testes
dependentes de tempo devem usar relógio controlável ou datas relativas e
determinísticas.

Uma tarefa só está pronta quando o requisito foi atendido, o código compila, as
validações aplicáveis passam, segurança e multiempresa foram auditadas, a
documentação está coerente, nenhum segredo foi incluído e o diff contém apenas o
escopo autorizado.

## Critérios de parada

Pare e solicite decisão antes de prosseguir se encontrar:

- conflito estrutural ou entre regras/documentos;
- mais de uma interpretação materialmente relevante ou decisão de negócio
  ausente;
- migration inesperada, fora de sequência, destrutiva ou que exija reescrever
  migration aplicada;
- regra financeira ou de negócio indefinida;
- risco de perda ou corrupção de dados;
- falha de segurança, autorização, privacidade ou vazamento em logs;
- risco de violar isolamento multiempresa;
- dependência insegura ou mudança arquitetural não autorizada;
- trabalho concorrente ou autoria de alterações que não possa ser determinada;
- falha real cuja correção amplie materialmente o escopo.

Não pare apenas porque a tarefa é difícil ou demorada. Investigue primeiro todas
as alternativas seguras dentro do escopo.

## Formato padrão de tarefas

Use o identificador:

```text
CRIATI-<ÁREA>-<NÚMERO>
```

Padrão canônico: `CRIATI-XXX-000`, com área em maiúsculas e número de três
dígitos. Sufixo alfabético, como `CRIATI-FIN-015A`, só deve ser usado para uma
extensão explicitamente relacionada à tarefa original.

Uma tarefa deve informar, quando aplicável:

```text
identificador e título
objetivo
base, branch e HEAD esperados
escopo e proibições
decisões obrigatórias
migrations e contratos
testes e validações
critérios de parada
política de commit e operações externas
formato da entrega
```

## Relatório final

Entregue um relatório compacto com:

- resultado e arquivos alterados;
- testes focados e suíte completa, com contagens e falhas;
- PostgreSQL/migrations, quando aplicável;
- auditoria de segurança, dinheiro, dados, logs, multiempresa e escopo;
- riscos, limitações e pendências reais;
- resumo do diff e `git status`;
- branch, `HEAD` final e hash do commit, quando criado;
- confirmação das operações externas realizadas ou não realizadas.

Não omita falhas e não apresente como concluída uma validação que não pôde ser
executada.
