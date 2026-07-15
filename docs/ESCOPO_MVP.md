# Escopo do MVP — Criati

## Objetivo

O MVP validará a Criati como uma plataforma SaaS modular e multiempresa, oferecendo uma fundação segura e um primeiro módulo operacional utilizável.

O MVP será dividido em duas etapas:

1. Núcleo SaaS;
2. Tarefas e Processos.

## Princípios do MVP

- entregar uma base utilizável;
- priorizar segurança e isolamento dos dados;
- evitar funcionalidades que ainda não foram validadas;
- manter a arquitetura preparada para novos módulos;
- implementar cada etapa com testes;
- não antecipar microsserviços;
- não incluir integrações complexas sem necessidade.

# Etapa 1 — Núcleo SaaS

## 1. Autenticação

### Funcionalidades

- tela de login personalizada;
- login por e-mail e senha;
- encerramento de sessão;
- recuperação de senha;
- alteração de senha;
- bloqueio de usuário inativo;
- mensagens de erro sem revelar informações sensíveis.

### Critérios de aceite

- usuário ativo consegue entrar;
- credenciais inválidas não permitem acesso;
- usuário inativo não consegue entrar;
- senha não é armazenada em texto puro;
- recuperação utiliza token temporário e de uso único;
- páginas protegidas exigem autenticação.

## 2. Administração da Criati

### Funcionalidades

- dashboard administrativo;
- cadastro e edição de empresas;
- cadastro de unidades;
- controle de status da empresa;
- cadastro de módulos;
- ativação de módulos por empresa;
- cadastro de planos;
- controle manual de assinaturas;
- consulta de usuários e vínculos.

### Critérios de aceite

- somente administradores da Criati acessam essa área;
- empresa pode ser ativada, suspensa ou cancelada;
- módulos são habilitados somente pela Criati;
- empresa suspensa respeita as regras de acesso;
- alterações críticas são auditadas.

## 3. Empresas

### Funcionalidades

- cadastro;
- edição;
- visualização;
- ativação;
- suspensão;
- cancelamento;
- configurações básicas;
- fuso horário;
- identidade visual básica.

### Dados iniciais

```text
razão social
nome fantasia
CNPJ ou documento
e-mail
telefone
status
fuso horário
data de cadastro
```

### Critérios de aceite

- empresa possui UUID;
- empresa possui código amigável;
- empresa não é apagada pela operação comum;
- dados de uma empresa não aparecem para outra.

## 4. Unidades

### Funcionalidades

- criação automática da Unidade Principal;
- cadastro de novas unidades;
- edição;
- ativação e desativação;
- vínculo de usuários às unidades.

### Critérios de aceite

- toda empresa possui uma Unidade Principal;
- unidade sempre pertence a uma empresa;
- usuário acessa somente unidades autorizadas;
- unidade desativada mantém o histórico.

## 5. Usuários e convites

### Funcionalidades

- convite por e-mail;
- criação de senha pelo usuário;
- reenvio de convite;
- cancelamento de convite;
- vínculo do mesmo usuário a várias empresas;
- ativação e desativação do vínculo;
- consulta dos vínculos.

### Estados do vínculo

```text
CONVIDADO
ATIVO
BLOQUEADO
INATIVO
```

### Critérios de aceite

- o convite possui validade;
- o token é de uso único;
- senha nunca é enviada por e-mail;
- o mesmo e-mail pode participar de várias empresas;
- desativar um vínculo não remove os outros;
- convite expirado não pode ser utilizado.

## 6. Seleção de empresa

### Funcionalidades

- seleção após o login;
- seleção automática quando houver apenas uma empresa;
- troca de empresa durante a sessão;
- exibição clara da empresa atual.

### Critérios de aceite

- somente empresas vinculadas são listadas;
- troca de empresa recarrega permissões;
- contexto anterior não permanece ativo;
- tentativa de acessar empresa não vinculada é bloqueada.

## 7. Perfis e permissões

### Perfis padrão

```text
Administrador da empresa
Gestor
Usuário
```

### Funcionalidades

- criação de perfis personalizados;
- edição;
- ativação e desativação;
- associação de permissões;
- associação de perfis ao vínculo;
- consulta das permissões efetivas.

### Critérios de aceite

- perfil pertence à empresa;
- permissão em uma empresa não vale para outra;
- usuário visualiza somente recursos autorizados;
- proteção ocorre no backend;
- perfis utilizados não são excluídos definitivamente.

## 8. Módulos

### Funcionalidades

- catálogo de módulos;
- ativação por empresa;
- desativação;
- controle de acesso por permissão;
- exibição dinâmica no menu.

### Critérios de aceite

- módulo não contratado não aparece no menu;
- acesso direto à URL também é bloqueado;
- desativação não apaga dados;
- somente a Criati altera módulos contratados.

## 9. Planos e assinaturas

### Funcionalidades

- cadastro de planos;
- limites de usuários;
- limites de unidades;
- limites de armazenamento;
- associação de módulos;
- assinatura manual por empresa;
- alteração de status.

### Estados

```text
TESTE
ATIVA
ATRASADA
SUSPENSA
CANCELADA
```

### Critérios de aceite

- cada assinatura pertence a uma empresa;
- limites podem ser consultados pela aplicação;
- empresa suspensa mantém os dados;
- alterações são auditadas;
- cobrança automática não faz parte do MVP.

## 10. Configurações da empresa

### Funcionalidades

- nome de exibição;
- logotipo;
- fuso horário;
- preferências básicas;
- configurações dos módulos.

### Critérios de aceite

- somente usuários autorizados alteram configurações;
- alterações afetam somente a empresa atual;
- configurações inválidas são rejeitadas.

## 11. Auditoria

### Eventos mínimos

- login e falhas relevantes;
- criação e alteração de empresas;
- alteração de status;
- convite de usuários;
- ativação e desativação de vínculos;
- alteração de perfis e permissões;
- ativação de módulos;
- alteração de plano e assinatura;
- troca de empresa;
- operações críticas.

### Dados mínimos

```text
evento
usuário
empresa
data e hora
origem
registro afetado
resultado
detalhes seguros
```

### Critérios de aceite

- auditoria não pode ser editada pelas telas comuns;
- registros utilizam UTC;
- senhas e tokens não aparecem na auditoria;
- eventos podem ser filtrados.

## 12. Dashboard inicial

### Administração da Criati

- empresas ativas;
- empresas em implantação;
- assinaturas por status;
- usuários ativos;
- módulos utilizados;
- alertas administrativos.

### Empresa

- empresa e unidade atuais;
- módulos disponíveis;
- usuários ativos;
- notificações;
- resumo das tarefas após a Etapa 2.

# Etapa 2 — Tarefas e Processos

## 1. Tarefas

### Funcionalidades

- criar;
- visualizar;
- editar;
- arquivar;
- atribuir responsável;
- adicionar participantes;
- definir prioridade;
- definir prazo;
- vincular unidade;
- adicionar descrição;
- adicionar categoria;
- alterar status.

### Prioridades iniciais

```text
BAIXA
MEDIA
ALTA
URGENTE
```

### Critérios de aceite

- tarefa sempre pertence a uma empresa;
- unidade, quando informada, pertence à empresa;
- responsável possui vínculo ativo;
- permissões são verificadas pelo backend;
- arquivamento mantém o histórico.

## 2. Fluxos e status

### Funcionalidades

- fluxo padrão fornecido pela Criati;
- criação de status;
- alteração de nome;
- definição de cor;
- ordenação;
- ativação e desativação;
- indicação de status inicial e final.

### Fluxo padrão

```text
Pendente
Em andamento
Concluída
```

### Critérios de aceite

- status pertence à empresa;
- tarefa não utiliza status de outra empresa;
- status em uso não é excluído;
- cada fluxo possui um status inicial;
- status final identifica conclusão.

## 3. Visualização em lista

### Funcionalidades

- paginação;
- ordenação;
- pesquisa;
- filtros;
- acesso aos detalhes;
- ações conforme permissões.

### Filtros mínimos

```text
status
responsável
unidade
categoria
prioridade
prazo
```

## 4. Kanban

### Funcionalidades

- colunas conforme os status;
- cartões resumidos;
- movimentação de tarefas;
- filtros;
- ordenação;
- visualização responsiva.

### Critérios de aceite

- usuário sem permissão não movimenta tarefa;
- mudança de coluna atualiza o status;
- mudança gera histórico;
- nenhuma movimentação mistura empresas.

## 5. Comentários

### Funcionalidades

- adicionar comentário;
- listar comentários;
- identificar autor e horário;
- gerar notificação aos participantes.

### Critérios de aceite

- comentário pertence à tarefa;
- autor deve possuir acesso;
- comentários não podem expor dados entre empresas;
- exclusão, quando permitida, preserva registro de auditoria.

## 6. Anexos

### Funcionalidades

- upload;
- download autorizado;
- listagem;
- arquivamento;
- vínculo com tarefa ou comentário.

### Critérios de aceite

- arquivo físico fica fora do PostgreSQL;
- metadados ficam no banco;
- download exige autorização;
- limites de tamanho e formato são aplicados;
- arquivo pertence à empresa atual.

## 7. Histórico

### Eventos mínimos

- criação;
- edição;
- alteração de responsável;
- alteração de prazo;
- mudança de prioridade;
- mudança de status;
- comentário;
- anexo;
- arquivamento.

### Critérios de aceite

- histórico registra autor e horário;
- registros usam UTC;
- histórico não pode ser alterado pelas telas comuns.

## 8. Tarefas recorrentes

### Funcionalidades

- recorrência diária;
- recorrência semanal;
- recorrência mensal;
- data inicial;
- data final opcional;
- próxima execução;
- ativação e desativação.

### Critérios de aceite

- não gerar tarefas duplicadas;
- recorrência respeita o fuso da empresa;
- desativação não apaga tarefas anteriores;
- falhas de geração são registradas.

## 9. Notificações

### Eventos iniciais

- nova tarefa atribuída;
- alteração de responsável;
- comentário;
- prazo próximo;
- tarefa atrasada;
- alteração importante.

### Canais do MVP

```text
notificação interna
e-mail
```

WhatsApp não faz parte do MVP.

## 10. Indicadores

### Indicadores iniciais

- total de tarefas;
- tarefas por status;
- tarefas atrasadas;
- tarefas por responsável;
- tarefas concluídas no período;
- tarefas por prioridade;
- tarefas por unidade.

## Fora do escopo do MVP

Não fazem parte da primeira entrega:

- cobrança automática;
- gateway de pagamento;
- integração com WhatsApp;
- aplicativo móvel nativo;
- microsserviços;
- modo de suporte completo;
- autenticação multifator;
- inteligência artificial avançada;
- modelos completos de processos;
- marketplace de módulos;
- banco exclusivo por empresa;
- personalização completa de domínio;
- realidade aumentada;
- módulos Financeiro e Estoque.

Esses itens poderão ser avaliados no roadmap.

## Requisitos gerais de conclusão

O MVP será considerado tecnicamente concluído quando:

1. a aplicação iniciar corretamente;
2. migrations forem executadas;
3. testes automatizados passarem;
4. dados estiverem isolados por empresa;
5. autenticação e permissões funcionarem;
6. fluxos principais estiverem documentados;
7. aplicação funcionar em computador e celular;
8. não existirem senhas ou segredos no Git;
9. logs não expuserem dados sensíveis;
10. houver instruções para executar e publicar o projeto.

## Ordem recomendada de implementação

1. configuração dos ambientes;
2. Flyway e estrutura inicial;
3. autenticação;
4. empresas e unidades;
5. usuários e convites;
6. seleção de empresa;
7. perfis e permissões;
8. módulos;
9. planos e assinaturas;
10. auditoria;
11. dashboard;
12. tarefas;
13. fluxos e status;
14. lista e filtros;
15. Kanban;
16. comentários e anexos;
17. recorrência;
18. notificações;
19. indicadores;
20. revisão de segurança e isolamento.