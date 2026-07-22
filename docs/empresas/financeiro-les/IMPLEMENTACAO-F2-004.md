# Implementação LES-F2-004 — Categorias financeiras

## Diagnóstico e reaproveitamento

O Financeiro já possuía `CategoriaFinanceira`, tabela `categoria_financeira`, repository, service,
API `/api/contexto/financeiro/categorias`, página `/app/financeiro/categorias` e vínculo direto de
`LancamentoFinanceiro`. A entrega evolui esses contratos, preservando UUIDs, relacionamentos,
rota e navegação. Não foi criada entidade, tabela ou página concorrente.

Antes da V8, a categoria já pertencia a uma empresa, possuía nome, natureza `RECEITA`/`DESPESA`,
status e datas. Não havia hierarquia, descrição, ordem, configuração para orçamento nem autores.
A inativação já era lógica e os lançamentos mantinham a FK, mas qualquer edição era bloqueada se
houvesse histórico.

## Modelo e hierarquia

`CategoriaFinanceira` passa a possuir descrição opcional, categoria pai opcional, ordem de
exibição, `permiteOrcamento` e usuários de criação/atualização. O campo legado `tipo` continua
representando a natureza financeira para não quebrar lançamentos e consumidores existentes.
Não existe `AMBAS`: receita e despesa permanecem classificações inequívocas.

A hierarquia tem no máximo dois níveis. Categoria sem pai é também o grupo visual; categoria com
pai é a classificação detalhada. Pai e filha devem pertencer à empresa atual, estar em relação
sem ciclos e ter a mesma natureza. Pai inativo não recebe filha nova. Cor e ícone não foram
incluídos porque não existia suporte e nenhum comportamento financeiro depende deles.

Não há cascata de status. Uma categoria principal com filhas ativas não pode ser inativada até
que o usuário mova ou inative explicitamente cada filha. Assim não ocorre mudança silenciosa.
Reativar uma filha exige pai ativo.

## Regras e histórico

- A empresa é sempre derivada do contexto autenticado; nenhum DTO recebe `empresaId`.
- Novas categorias exigem administrador, nome, natureza e autor válido.
- Nome exibido é apenas aparado e tem espaços internos repetidos consolidados; a comparação de
  duplicidade ignora caixa e espaços nas extremidades.
- Duas categorias ativas não podem ter o mesmo nome normalizado, pai, natureza e empresa. O mesmo
  nome em pais ou naturezas diferentes é permitido.
- Ordem é inteira não negativa, pode repetir e desempata por nome.
- Despesas sem configuração explícita iniciam permitindo orçamento; receitas não. O booleano é
  armazenado em cada categoria, sem herança implícita e sem implementar orçamento.
- Categoria inativa sai da listagem padrão, pode ser consultada administrativamente e continua
  vinculada e visível nos lançamentos históricos. Não existe endpoint de exclusão física.
- Nome, descrição, ordem e configuração de orçamento podem ser corrigidos com histórico. Natureza
  e pai não mudam quando há lançamentos: a API exige tratamento explícito futuro, sem reclassificar
  silenciosamente o passado.
- Criação, atualização e mudanças de status registram usuário e instante. Como a auditoria
  versionada ainda não existe, não foi criado um sistema paralelo de eventos.

## Migration V8

`V8__evoluir_categorias_financeiras.sql` altera a tabela existente, adiciona os novos campos, FKs
de pai e autores, checks de ordem e autorreferência e índices tenant-aware. Dois índices únicos
parciais tratam separadamente categorias raiz e filhas ativas, porque `NULL` no pai não deve
permitir duplicidade de raízes. Registros legados são preservados; despesas existentes recebem
`permite_orcamento=true`; autores permanecem anuláveis somente para compatibilidade histórica.

Não há seed de categorias pessoais ou valores reais. Categorias sugeridas serão criadas
manualmente nesta etapa ou por um onboarding futuro, após decisão própria.

## API e interface

- `GET /api/contexto/financeiro/categorias` — filtros `status`, `tipo`, `paiId`, `busca`,
  `principais` e `permiteOrcamento`; sem status retorna somente ativas;
- `GET /api/contexto/financeiro/categorias/{id}`;
- `POST /api/contexto/financeiro/categorias`;
- `PUT /api/contexto/financeiro/categorias/{id}`;
- `POST /api/contexto/financeiro/categorias/{id}/inativar`;
- `POST /api/contexto/financeiro/categorias/{id}/reativar`;
- `GET /api/contexto/financeiro/categorias/resumo`.

O DTO de saída informa pai, nível, quantidade de filhas, ordem, orçamento, status, datas e autores.
O payload antigo com apenas `nome` e `tipo` continua aceito com valores padrão.

A página existente foi evoluída com busca, filtro de natureza e status, resumo, hierarquia visual,
detalhes, formulário completo, confirmação de inativação e estados de carregamento, vazio, erro,
sucesso e validação. Ela reaproveita sidebar, topbar, temas e estilos globais, inclusive variações
clara/escura, Criati/Windows/Compacto e responsividade já oferecidas pelo layout.

## Segurança e testes

Leitura exige autenticação, empresa ativa e Financeiro habilitado. Escrita exige administrador e
CSRF. Toda busca de categoria e pai usa `id + empresa`; UUID de outro tenant é tratado como não
encontrado. O banco reforça integridade local e o service cobre regras que uma FK não expressa,
como tenant, nível, natureza e ciclo.

Testes JPA, MockMvc e de página cobrem criação raiz/filha, validações, duplicidade normalizada,
nomes em pais distintos, hierarquia, natureza, ordem, orçamento, status, filtros, resumo,
autenticação, CSRF, perfil, tenant, página e compatibilidade dos contratos anteriores.

## Limitações e próxima etapa

- Não há histórico versionado campo a campo, apenas autor e instante atuais.
- Mudança de pai/natureza com histórico é bloqueada; um fluxo de confirmação versionada poderá
  ser criado quando a auditoria completa existir.
- Não foram implementados drag-and-drop, cor, ícone, seed/onboarding, orçamento, categorização
  automática, relatórios ou lançamentos novos.
- A validação visual depende de navegador disponível no ambiente; testes web automatizados não
  substituem homologação humana de todas as resoluções.

A base fica pronta para as próximas operações financeiras referenciarem uma única categoria
tenant-aware e para uma tarefa futura criar orçamento sem mudar novamente o conceito principal.
