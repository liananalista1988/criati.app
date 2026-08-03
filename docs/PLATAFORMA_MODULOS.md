# Plataforma de módulos — Criati

## Catálogo técnico

`CodigoAplicacao` é a definição central e tipada dos módulos reconhecidos pelo produto. Ela concentra
código estável, nome de exibição, descrição curta, chave visual, rota inicial, situação técnica e ordem.

| Código | Situação técnica | Persistência atual | Rota |
|---|---|---|---|
| `FINANCEIRO` | Operacional | catálogo e vínculo por empresa | `/app/financeiro` |
| `CLINICA` | Demonstração | catálogo e vínculo por empresa | `/app/clinica` |
| `TAREFAS_PROCESSOS` | Operacional | catálogo e vínculo por empresa | `/app/trabalho/processos` |
| `ESTOQUE` | Indisponível | não persistido | nenhuma |

Módulo indisponível não é semeado no banco, não aparece para empresas e não possui rota navegável.
Reconhecê-lo no catálogo técnico apenas reserva código e metadados para integração futura.

Tarefas e Processos reutiliza a mesma habilitação técnica de `EmpresaAplicacao`. Com o módulo habilitado,
todos os perfis empresariais válidos podem consultar processos e tarefas; `ADMINISTRADOR` mantém as escritas
estruturais e o responsável pode atualizar somente o andamento das próprias tarefas, conforme as regras do módulo.
Página e API repetem o gate modular no backend, independentemente da visibilidade do menu.

## Disponibilidade para a empresa

`ModuloDisponibilidadeService` combina o catálogo técnico com `Aplicacao` e `EmpresaAplicacao` já
existentes. Um módulo só é apresentado quando:

- é conhecido e tecnicamente disponível;
- a aplicação global está ativa;
- o vínculo da aplicação com a empresa atual está ativo;
- o contexto empresarial e seu perfil foram previamente validados pelo backend.

Todos os perfis empresariais atuais podem visualizar módulos habilitados. Permissões granulares ainda
não existem; quando forem implementadas, deverão ser incorporadas ao serviço central, sem decisão no
frontend.

## Limite comercial

O vínculo ativo em `EmpresaAplicacao` representa habilitação técnica para uma empresa. Ele não comprova
contratação, plano, pagamento ou assinatura. Esses conceitos comerciais continuam futuros e não podem
ser inferidos pelo catálogo ou exibidos como produtos contratados.

Nenhuma migration foi necessária: `aplicacao` e `empresa_aplicacao`, criadas anteriormente, já suportam
a disponibilidade persistida dos módulos atualmente integrados. A integração de Trabalho incorpora somente a
`V25__criar_processos_tarefas_empresariais.sql` criada pelo próprio módulo; não cria migration adicional.

## Entrada e navegação

A entrada autenticada resolve duas dimensões em sequência, sem usar a quantidade de empresas como se
fosse a quantidade de módulos:

1. com uma empresa ativa vinculada, o backend seleciona seu contexto automaticamente; com várias, o
   dashboard preserva a escolha explícita;
2. depois de definido o contexto, um único módulo disponível abre diretamente, enquanto zero ou vários
   levam ao panorama de módulos. O caso zero permanece em um estado vazio estável, sem redirecionamento
   circular.

O mesmo resolvedor atende acessos diretos a `/app/dashboard` e `/app/aplicacoes`. A troca de empresa na
topbar retorna a essa entrada central, de modo que um módulo da empresa anterior não permaneça aberto.

A sidebar recebe do backend somente módulos tecnicamente disponíveis e habilitados para a empresa.
Links de administração empresarial são renderizados apenas para `ADMINISTRADOR`; a ocultação é apoio de
interface, e as rotas continuam protegidas no backend. Os grupos internos do Financeiro e o grupo de Tarefas e
Processos permanecem recolhidos por padrão, abrindo automaticamente apenas o grupo da página atual.
