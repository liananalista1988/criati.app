# Plataforma de módulos — Criati

## Catálogo técnico

`CodigoAplicacao` é a definição central e tipada dos módulos reconhecidos pelo produto. Ela concentra
código estável, nome de exibição, descrição curta, chave visual, rota inicial, situação técnica e ordem.

| Código | Situação técnica | Persistência atual | Rota |
|---|---|---|---|
| `FINANCEIRO` | Operacional | catálogo e vínculo por empresa | `/app/financeiro` |
| `CLINICA` | Demonstração | catálogo e vínculo por empresa | `/app/clinica` |
| `TAREFAS_PROCESSOS` | Indisponível | não persistido | nenhuma |
| `ESTOQUE` | Indisponível | não persistido | nenhuma |

Módulo indisponível não é semeado no banco, não aparece para empresas e não possui rota navegável.
Reconhecê-lo no catálogo técnico apenas reserva código e metadados para integração futura.

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
a disponibilidade persistida dos módulos atualmente integrados.
