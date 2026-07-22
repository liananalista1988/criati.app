# Implementação LES-F2-002 — Pessoas e partes financeiras

## Objetivo

Esta entrega acrescenta ao núcleo financeiro compartilhado os cadastros de pessoas da residência e de partes financeiras externas. Ela não cria contas, categorias, lançamentos, cartões ou outros conceitos das etapas seguintes.

## Separação dos conceitos

- `Usuario` continua sendo exclusivamente a identidade autenticada e não foi alterado.
- `PessoaFinanceira` representa um membro interno da vida financeira da residência. Pode existir sem acesso ao sistema e pode ter vínculo opcional com um `Usuario` ativo da mesma empresa.
- `ParteFinanceira` representa uma pessoa ou organização externa envolvida em relações financeiras.

Uma pessoa da residência não é duplicada automaticamente como parte financeira. A interface usa o nome mais amigável **Contatos financeiros**, enquanto o domínio preserva o nome técnico `ParteFinanceira`.

## Persistência

A migration `V6__criar_pessoas_e_partes_financeiras.sql` cria:

- `pessoa_financeira`: empresa, nome, apelido, usuário opcional, status, datas e usuários responsáveis;
- `parte_financeira`: empresa, nome, tipo, documento opcional, apelido, observação, status, datas e usuários responsáveis.

As duas tabelas usam UUID, chaves estrangeiras para empresa e auditoria, restrições de status e índices por empresa e status. `pessoa_financeira` possui ainda índice único parcial para impedir duas pessoas ativas vinculadas ao mesmo usuário na mesma empresa. `parte_financeira` possui restrição para os tipos `PESSOA`, `ORGANIZACAO`, `ESTABELECIMENTO` e `OUTRA`, além de índice para pesquisa por nome.

## Regras e validações

- A empresa é sempre obtida do contexto autenticado; nenhum DTO recebe `empresaId`.
- Busca, alteração e mudança de status usam o par identificador e empresa ativa.
- Nome é obrigatório, tem espaços externos removidos e aceita até 150 caracteres.
- Apelido e observação em branco são persistidos como nulos.
- Documento é opcional e normalizado para caracteres alfanuméricos em maiúsculas; CPF ou CNPJ não são exigidos.
- O usuário opcional deve estar ativo e possuir vínculo ativo com a mesma empresa.
- Um usuário só pode estar ligado a uma pessoa ativa por empresa.
- Nomes repetidos de partes financeiras são aceitos.
- Leituras são permitidas aos perfis com acesso ao Financeiro; escritas exigem `ADMINISTRADOR`, seguindo a política já usada pelo módulo.

## Serviços e repositórios

`PessoaFinanceiraService` e `ParteFinanceiraService` concentram criação, consulta, listagem, atualização, normalização, ativação, desativação e validações de tenant. Os repositórios não expõem consultas empresariais sem filtro por empresa.

As operações registram `criadoEm`, `atualizadoEm`, `criadoPorUsuario` e `atualizadoPorUsuario`. Alterações de vínculo também atualizam o responsável e a data. Não foi criado um sistema paralelo de auditoria.

## Endpoints

Pessoas da residência:

- `GET /api/contexto/financeiro/pessoas`
- `GET /api/contexto/financeiro/pessoas/{id}`
- `GET /api/contexto/financeiro/pessoas/usuarios-vinculaveis`
- `POST /api/contexto/financeiro/pessoas`
- `PUT /api/contexto/financeiro/pessoas/{id}`
- `POST /api/contexto/financeiro/pessoas/{id}/inativar`
- `POST /api/contexto/financeiro/pessoas/{id}/reativar`

Contatos financeiros:

- `GET /api/contexto/financeiro/contatos`, com filtros opcionais `status`, `tipo` e `busca`
- `GET /api/contexto/financeiro/contatos/{id}`
- `POST /api/contexto/financeiro/contatos`
- `PUT /api/contexto/financeiro/contatos/{id}`
- `POST /api/contexto/financeiro/contatos/{id}/inativar`
- `POST /api/contexto/financeiro/contatos/{id}/reativar`

Os endpoints reutilizam sessão, CSRF, `ContextoFinanceiroService` e o tratamento global de erros existentes.

## Telas e navegação

- `/app/financeiro/pessoas`: listagem, filtro de status, cadastro, edição, vínculo opcional com usuário, ativação e desativação.
- `/app/financeiro/contatos`: listagem, busca por nome, filtro por tipo e status, cadastro, edição, ativação e desativação.

O dashboard do Financeiro recebeu atalhos para as duas páginas. As telas reutilizam sidebar, topbar, temas, estilos e componentes responsivos da Criati. Há estados de carregamento, vazio e erro, confirmações de mudança de status e mensagens de sucesso ou falha. Uma possível duplicidade de contato não bloqueia o cadastro, conforme a regra do MVP.

## Segurança e isolamento

As páginas exigem autenticação, empresa ativa, vínculo ativo e aplicação Financeiro habilitada. Os endpoints repetem essa validação e não confiam em identificadores de empresa do navegador. Um UUID pertencente a outro tenant resulta em recurso não encontrado, sem revelar sua existência. A seleção de usuário apresenta e aceita apenas vínculos ativos da empresa atual.

## Testes

Os testes de integração com MockMvc cobrem criação e validação dos dois conceitos, vínculo opcional, vínculo válido, rejeição de tenant cruzado, duplicidade de vínculo ativo, atualização, desativação, reativação, normalização de documento, nomes duplicados, pesquisa, autenticação, autorização e isolamento de listagens e consultas. Os testes de páginas cobrem as novas rotas com e sem acesso ao Financeiro.

## Limitações e próximas etapas

- Não há exclusão física nem lixeira financeira completa; o ciclo suportado é ativo/inativo com reativação.
- Não há validação fiscal de CPF/CNPJ nem bloqueio automático de possível duplicidade.
- Não há herança ou conversão automática entre pessoa da residência e parte financeira.
- A entrega prepara referências seguras para a LES-F2-003 e para titularidades e contrapartes futuras, sem antecipar seus modelos.
