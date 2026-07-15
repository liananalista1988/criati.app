# Banco de Dados — Criati

## Objetivo

Este documento define os padrões de banco de dados da Criati, incluindo:

- PostgreSQL;
- isolamento multiempresa;
- versionamento com Flyway;
- identificação por UUID;
- auditoria;
- desativação de registros;
- integridade entre tabelas;
- segurança das credenciais;
- ambientes de desenvolvimento e produção.

## Tecnologia

O banco principal será o PostgreSQL.

Configuração local inicial:

```text
Servidor: localhost
Porta: 5432
Banco: criati_db
Usuário da aplicação: criati_app
```

A senha não poderá ser registrada no Git.

## Responsabilidades

| Componente | Responsabilidade |
|---|---|
| PostgreSQL | Armazenar e proteger os dados |
| Flyway | Criar e modificar a estrutura do banco |
| JPA/Hibernate | Mapear, consultar e persistir dados |
| Spring Boot | Aplicar regras, permissões e isolamento |
| Git | Versionar os scripts de migration |

## Hibernate

O Hibernate não deverá alterar automaticamente a estrutura do banco.

Configuração obrigatória:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

O modo `validate` apenas verifica se as entidades Java correspondem às tabelas existentes.

Não utilizar em produção:

```properties
spring.jpa.hibernate.ddl-auto=create
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.hibernate.ddl-auto=update
```

## Flyway

As alterações do banco serão controladas pelo Flyway.

Diretório:

```text
src/main/resources/db/migration
```

Exemplo:

```text
db/migration
├── V1__cria_estrutura_inicial.sql
├── V2__cria_empresas_e_unidades.sql
├── V3__cria_usuarios_e_vinculos.sql
├── V4__cria_perfis_e_permissoes.sql
├── V5__cria_modulos_planos_e_assinaturas.sql
└── V6__cria_tarefas_e_fluxos.sql
```

## Regras das migrations

- Todo ajuste estrutural terá uma nova migration.
- Uma migration executada nunca será editada.
- Scripts serão testados antes da publicação.
- O Flyway executará somente migrations ainda não aplicadas.
- Dados iniciais indispensáveis poderão ser inseridos por migration.
- Dados de demonstração não deverão entrar nas migrations de produção.
- Cada migration deverá possuir uma responsabilidade clara.
- Alterações destrutivas exigirão análise e backup.
- Migrations não deverão depender de execução manual posterior.

Exemplo correto:

```text
V7__adiciona_fuso_horario_empresa.sql
```

Exemplo incorreto:

```text
V7__ajustes.sql
```

## Convenções de nomenclatura

### Tabelas e colunas

Utilizar:

```text
snake_case
nomes no singular
letras minúsculas
```

Exemplos:

```text
empresa
usuario
usuario_empresa
status_tarefa
empresa_modulo
```

Evitar:

```text
Empresa
tbEmpresa
TBL_USUARIOS
dadosGerais
```

### Chaves

Chave primária:

```text
id
```

Chaves estrangeiras:

```text
empresa_id
usuario_id
unidade_id
perfil_id
modulo_id
```

### Datas e horários

```text
criado_em
atualizado_em
desativado_em
expira_em
ultimo_acesso_em
```

### Booleanos

Nomes devem indicar claramente uma condição:

```text
ativo
principal
obrigatorio
permite_todas_unidades
```

## Identificadores

As entidades principais utilizarão UUID como chave primária.

Exemplo:

```sql
id UUID PRIMARY KEY
```

O UUID poderá ser gerado pela aplicação.

Para informações apresentadas ao usuário, poderão existir códigos amigáveis:

```text
EMP-000001
UND-000001
TAR-2026-000001
```

O código amigável:

- não substitui o UUID;
- poderá ser pesquisado;
- deverá ser único dentro do contexto definido;
- não deverá ser utilizado como única proteção de segurança.

## Tipos recomendados

| Informação | PostgreSQL |
|---|---|
| Identificador | `UUID` |
| Texto curto | `VARCHAR` |
| Texto longo | `TEXT` |
| Data | `DATE` |
| Data e horário | `TIMESTAMPTZ` |
| Valor monetário | `NUMERIC(19,2)` |
| Percentual | `NUMERIC` com precisão definida |
| Verdadeiro/falso | `BOOLEAN` |
| Configuração flexível | `JSONB`, somente quando justificado |

Não utilizar `FLOAT` ou `DOUBLE PRECISION` para valores monetários.

## Datas e fuso horário

Instantes serão armazenados em UTC utilizando:

```sql
TIMESTAMPTZ
```

A aplicação converterá os horários conforme o fuso da empresa.

Fuso inicial:

```text
America/Fortaleza
```

Datas sem horário, como uma competência ou data de vencimento, poderão utilizar:

```sql
DATE
```

## Auditoria padrão

Entidades relevantes deverão possuir:

```text
criado_em
criado_por
atualizado_em
atualizado_por
```

Quando houver desativação:

```text
desativado_em
desativado_por
motivo_desativacao
```

A auditoria poderá ser preenchida por mecanismos do Spring Data JPA, desde que o usuário autenticado seja identificado corretamente.

## Exclusão de registros

A operação normal utilizará:

- desativação;
- cancelamento;
- suspensão;
- arquivamento.

Não serão utilizados apagamentos físicos indiscriminados.

Exemplo:

```text
status = INATIVO
desativado_em = data e horário
desativado_por = usuário responsável
```

Exclusão física ficará restrita a situações específicas e controladas.

## Banco compartilhado

Inicialmente, todas as empresas utilizarão o mesmo banco PostgreSQL.

Tabelas empresariais deverão possuir:

```sql
empresa_id UUID NOT NULL
```

Quando o dado estiver associado a uma unidade:

```sql
unidade_id UUID
```

Exemplo:

```sql
CREATE TABLE tarefa (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL,
    unidade_id UUID,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT,
    criado_em TIMESTAMPTZ NOT NULL
);
```

## Isolamento multiempresa

Toda consulta empresarial deverá utilizar a empresa atual.

Conceito correto:

```sql
SELECT *
FROM tarefa
WHERE id = :tarefaId
  AND empresa_id = :empresaAtual;
```

Conceito proibido:

```sql
SELECT *
FROM tarefa
WHERE id = :tarefaId;
```

Mesmo utilizando UUID, a aplicação deverá validar o `empresa_id`.

## Integridade entre empresas

O banco deverá impedir, sempre que viável, que registros de empresas diferentes sejam relacionados.

Exemplo de erro que deve ser evitado:

```text
Tarefa da Empresa A
→ status pertencente à Empresa B
```

Para relações empresariais críticas, poderão ser utilizadas restrições compostas envolvendo:

```text
empresa_id + id do registro relacionado
```

Exemplo conceitual:

```sql
FOREIGN KEY (empresa_id, status_id)
REFERENCES status_tarefa (empresa_id, id)
```

Essa proteção complementa as validações da aplicação.

## Restrições

O banco deverá utilizar:

- `PRIMARY KEY`;
- `FOREIGN KEY`;
- `NOT NULL`;
- `UNIQUE`;
- `CHECK`;
- índices apropriados.

As regras não deverão existir somente na interface.

Exemplos:

```sql
CHECK (limite_usuarios >= 0)
CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE'))
```

Quando os estados forem alterados frequentemente, poderá ser preferível uma tabela de domínio ou validação controlada pela aplicação.

## Índices

Tabelas empresariais deverão considerar índices iniciados por `empresa_id`.

Exemplos:

```sql
CREATE INDEX idx_tarefa_empresa
    ON tarefa (empresa_id);

CREATE INDEX idx_tarefa_empresa_status
    ON tarefa (empresa_id, status_id);

CREATE INDEX idx_tarefa_empresa_responsavel
    ON tarefa (empresa_id, responsavel_id);

CREATE INDEX idx_tarefa_empresa_prazo
    ON tarefa (empresa_id, prazo);
```

Índices deverão ser criados com base nas consultas reais. Índices desnecessários também possuem custo.

## Unicidade por empresa

Alguns dados serão únicos apenas dentro de uma empresa.

Exemplo:

```sql
UNIQUE (empresa_id, codigo)
```

Ou:

```sql
UNIQUE (empresa_id, nome_normalizado)
```

Isso permite que empresas diferentes utilizem o mesmo nome de categoria, perfil ou status.

## Tabelas globais

Tabelas globais não pertencem diretamente a uma empresa.

Exemplos:

```text
usuario
modulo
permissao
plano
```

Essas tabelas deverão possuir regras específicas de administração.

## Tabelas do núcleo empresarial

Estrutura conceitual inicial:

```text
empresa
unidade
usuario
usuario_empresa
usuario_empresa_unidade
perfil
usuario_empresa_perfil
permissao
perfil_permissao
modulo
empresa_modulo
plano
plano_modulo
assinatura
convite
token_recuperacao_senha
notificacao
arquivo
auditoria
```

## Empresa

Campos conceituais:

```text
id
codigo
razao_social
nome_fantasia
documento
email
telefone
status
fuso_horario
criado_em
criado_por
atualizado_em
atualizado_por
desativado_em
desativado_por
```

Regras:

- UUID como chave;
- código amigável único;
- documento normalizado para pesquisa e unicidade;
- fuso horário obrigatório;
- empresa não será apagada pela operação comum.

## Unidade

Campos conceituais:

```text
id
empresa_id
codigo
nome
tipo
principal
status
criado_em
criado_por
atualizado_em
atualizado_por
desativado_em
desativado_por
```

Regras:

- sempre pertence a uma empresa;
- cada empresa terá uma Unidade Principal;
- código único dentro da empresa;
- unidade em uso será desativada, não excluída.

## Usuário

Campos conceituais:

```text
id
nome
email
senha_hash
status
email_verificado_em
ultimo_acesso_em
criado_em
atualizado_em
```

Regras:

- usuário é global;
- e-mail normalizado e único;
- senha armazenada somente como hash;
- senha nunca será salva na auditoria;
- o mesmo usuário poderá acessar várias empresas.

## Vínculo entre usuário e empresa

Tabela:

```text
usuario_empresa
```

Campos conceituais:

```text
id
usuario_id
empresa_id
status
permite_todas_unidades
data_entrada
data_saida
criado_em
criado_por
atualizado_em
atualizado_por
```

Regra de unicidade:

```text
usuario_id + empresa_id
```

## Perfis e permissões

Tabelas:

```text
perfil
permissao
perfil_permissao
usuario_empresa_perfil
```

O perfil pertence à empresa, enquanto a permissão representa uma capacidade conhecida pela plataforma.

Exemplo:

```text
TAREFA_VISUALIZAR
TAREFA_CRIAR
TAREFA_EDITAR
USUARIO_CONVIDAR
PERFIL_ADMINISTRAR
```

## Módulos

Tabelas:

```text
modulo
empresa_modulo
```

O catálogo de módulos será global.

A tabela `empresa_modulo` definirá:

- empresa;
- módulo;
- status;
- data de ativação;
- data de desativação;
- configurações específicas.

## Planos e assinaturas

Tabelas:

```text
plano
plano_modulo
assinatura
```

No MVP, a assinatura será administrada manualmente pela Criati.

Estados previstos:

```text
TESTE
ATIVA
ATRASADA
SUSPENSA
CANCELADA
```

## Convites e tokens

Tokens deverão:

- ser aleatórios;
- possuir validade;
- ser de uso único;
- ser inutilizados após o uso;
- não aparecer em logs;
- preferencialmente ser armazenados de forma protegida.

Tabelas conceituais:

```text
convite
token_recuperacao_senha
```

## Tarefas e processos

Estrutura conceitual inicial:

```text
tarefa
status_tarefa
categoria_tarefa
tarefa_participante
tarefa_comentario
tarefa_historico
tarefa_recorrencia
arquivo
```

Todas essas tabelas deverão respeitar o isolamento por empresa.

## Status de tarefas

Cada empresa poderá criar seu próprio fluxo.

Campos conceituais:

```text
id
empresa_id
nome
cor
ordem
inicial
finalizador
ativo
```

Regras:

- pelo menos um status inicial por fluxo;
- status em uso não será excluído;
- status pertence obrigatoriamente a uma empresa.

## Tarefa

Campos conceituais:

```text
id
empresa_id
unidade_id
codigo
titulo
descricao
status_id
categoria_id
responsavel_usuario_empresa_id
prioridade
prazo
concluida_em
arquivada_em
criado_em
criado_por
atualizado_em
atualizado_por
```

O responsável deverá possuir vínculo ativo com a empresa da tarefa.

## Arquivos

O conteúdo físico dos arquivos não será salvo no PostgreSQL.

A tabela armazenará:

```text
id
empresa_id
nome_original
nome_armazenado
tipo_conteudo
tamanho
chave_armazenamento
hash
criado_em
criado_por
arquivado_em
```

O backend controlará o acesso ao arquivo.

## Auditoria

A auditoria deverá registrar:

```text
id
empresa_id
usuario_id
evento
recurso
registro_id
resultado
origem
detalhes_seguros
ocorrido_em
```

Não registrar:

- senhas;
- tokens completos;
- segredos;
- informações desnecessárias;
- conteúdo sensível sem justificativa.

## JSONB

`JSONB` poderá ser usado para configurações flexíveis, como preferências visuais ou configurações específicas de módulos.

Não deverá substituir uma modelagem relacional clara.

Exemplo aceitável:

```text
configuracoes_visuais
preferencias_notificacao
configuracoes_modulo
```

Exemplo inadequado:

```text
armazenar todos os dados de uma empresa em uma única coluna JSONB
```

## Ambientes

Perfis planejados:

```text
local
test
homolog
prod
```

Cada ambiente terá:

- banco próprio;
- credenciais próprias;
- configurações próprias;
- migrations equivalentes;
- segredos fora do Git.

O banco de produção nunca deverá ser utilizado para testes locais.

## Variáveis de ambiente

Exemplo:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

Valores reais não deverão aparecer em:

```text
application.properties
application-prod.properties
README.md
commits
logs
capturas de tela
```

## Dados de desenvolvimento

Dados para testes locais deverão ser criados por mecanismo específico do perfil local ou por testes automatizados.

Não inserir dados fictícios de clientes nas migrations de produção.

## Transações

Serviços que realizarem várias alterações relacionadas deverão utilizar transações.

Exemplo:

```text
cadastrar empresa
→ criar Unidade Principal
→ criar vínculo do administrador
→ atribuir perfil
→ criar assinatura inicial
```

Se uma etapa falhar, a operação deverá ser revertida de forma consistente.

## Concorrência

Registros que possam sofrer atualizações simultâneas poderão utilizar controle de versão otimista.

Exemplo conceitual:

```text
versao
```

No JPA:

```java
@Version
```

Isso evita que uma alteração sobrescreva silenciosamente outra.

## Backup e recuperação

Antes da produção, deverá existir uma estratégia para:

- backup automático;
- retenção;
- restauração;
- teste periódico de recuperação;
- proteção das cópias;
- documentação do procedimento.

Um backup somente será considerado confiável após a restauração ser testada.

## Revisão de migrations

Antes de aceitar uma migration, verificar:

1. nomes claros;
2. tipos corretos;
3. chaves primárias;
4. chaves estrangeiras;
5. isolamento por empresa;
6. restrições;
7. índices;
8. preservação de dados;
9. compatibilidade com os ambientes;
10. possibilidade de execução em produção.

## Proibições

Não será permitido:

- salvar senha em texto puro;
- colocar credenciais no Git;
- utilizar `ddl-auto=update` em produção;
- editar migration já executada;
- criar tabela empresarial sem `empresa_id`;
- consultar dado empresarial somente pelo UUID;
- apagar histórico indiscriminadamente;
- armazenar valores monetários em ponto flutuante;
- criar relacionamento sem validar a empresa;
- utilizar dados reais de clientes em testes comuns.

## Princípio principal

O banco deverá reforçar a segurança e a integridade da aplicação. O isolamento entre empresas não poderá depender apenas da interface ou da disciplina do programador.