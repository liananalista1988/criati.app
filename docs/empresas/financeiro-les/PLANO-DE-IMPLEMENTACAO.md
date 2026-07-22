# Plano de Implementação — Financeiro LeS

## Fluxo de uma tarefa

1. Confirmar branch, árvore limpa, documentação e linha de base dos testes.
2. Revisar dependências, invariantes e pendências da funcionalidade.
3. Fechar escopo vertical coeso e critérios de aceite.
4. Criar uma migration apenas se o incremento exigir persistência nova.
5. Implementar modelo/repository/service/controller/DTO/interface necessários, sem antecipar o
   módulo seguinte.
6. Criar testes proporcionais, incluindo acesso cruzado por UUID válido.
7. Homologar casos principal, limite e erro no navegador responsivo.
8. Executar regressão, revisar diff, documentar e produzir relatório final.

## Granularidade

Uma tarefa representa um caso de uso ou pequeno conjunto coeso e deve ser revertível. É aceitável
atravessar banco, backend, frontend e testes quando isso entrega uma única capacidade utilizável.
Não é aceitável criar um módulo inteiro, todas as tabelas ou uma camada isolada sem valor
verificável. Tarefa classificada como crítica exige cenários de cálculo independentes e revisão
específica de invariantes.

## Estratégia de migrations

- Uma migration por tarefa funcional ou conjunto realmente coeso, com nome descritivo.
- Nunca editar migration aplicada; correção usa nova versão.
- Usar UUID, `TIMESTAMPTZ`, `empresa_id`, chaves estrangeiras, checks, unicidade e índices.
- Quando tecnicamente viável, constraints compostas reforçam que relações mantêm o tenant.
- Exclusão lógica e auditoria são modeladas conforme a entidade, não por exclusão física casual.
- Rollback é conceitual: registrar impacto, plano de reversão por nova migration e preservação de
  dados; não prometer `down migration` automática inexistente.
- Estrutura e dados iniciais ficam separados; nenhuma migration contém dado familiar real.
- Validar banco vazio, atualização desde a versão anterior e Hibernate `validate`.

## Estratégia de dados iniciais

| Ambiente | Origem permitida | Restrições |
|---|---|---|
| Desenvolvimento | fixtures/perfil local explícito | somente dados fictícios; desabilitado por padrão quando aplicável |
| Teste | builders/fixtures descartáveis | determinísticos e isolados por empresa |
| Homologação | carga sanitizada ou cadastro pela interface | sem cópia de dados reais; acesso restrito |
| Produção | fluxos autenticados ou script local não versionado e auditado | sem seed de e-mail, salário, saldo, conta, limite ou comprovante real |

A empresa e seus dois usuários reais serão criados somente pelo fluxo seguro existente ou, se
necessário, por script operacional local não versionado, revisado na tarefa adequada.

## Estratégia de homologação

Cada tarefa funcional passa por testes automatizados, aplicação em ambiente isolado, navegador,
cenários principal/erro/limite, responsividade e evidências sanitizadas quando necessárias.
Módulos financeiros críticos usam uma tabela de valores controlados para conferir consumo,
compromisso, caixa e projeções. O relatório registra versão, cenário, resultado e pendências.

## Critérios para avançar

- entrada e dependências da fase satisfeitas;
- pendências bloqueadoras resolvidas;
- contratos a montante estáveis;
- suíte anterior aprovada;
- escopo e aceite fechados;
- nenhum dado sensível necessário para desenvolver.

## Ordem de entregas

1. LES-F2: fundação, cadastros, lançamentos, saldos, contas a pagar e recorrências.
2. LES-F3: cartões, compras, parcelas, faturas, pagamentos, estornos e limite saudável.
3. LES-F4: orçamento, meta e consolidação do dashboard.
4. LES-F5: terceiros, exposição, simulador, projeções e recomendações.
5. LES-F6: importação estruturada, conciliação, duplicidade e categorização.
6. Exportações e integrações: somente após necessidade e contratos estáveis, em tarefas futuras.

## Riscos e controles

| Risco | Controle |
|---|---|
| Construir tudo de uma vez | fases com gates e tarefas verticais pequenas |
| Excesso ou migration única | migration coesa por tarefa e revisão do esquema |
| Reaproveitamento incorreto | matriz núcleo × extensão antes da primeira entidade |
| Duplicação de conceitos | contratos de módulo e glossário como referência |
| Dashboard antes dos dados | cards somente após serviço-fonte validado |
| Simulador antes das regras | gate após orçamento, faturas, parcelas e projeções |
| Importação antes da conciliação | definir estados e vínculo antes do parser produtivo |
| OCR prematuro | manter PDF/OCR fora da fase inicial |
| Falta de teste multiempresa | cenário cruzado obrigatório em cada tarefa |
| Dados reais em seed | fixtures fictícias e fluxo seguro de produção |
| Tarefas grandes/mistura de fases | limite de responsabilidade e dependências explícitas |
| Mudança fora do escopo | revisão do diff e aprovação para decisão estrutural |

## Primeira entrega

LES-F2-001 prepara fronteiras e convenções, registra o reaproveitamento do módulo `financeiro`,
define os critérios multiempresa e localiza os testes a estender. Não cria todas as entidades nem
uma migration abrangente. Sua saída permite detalhar LES-F2-002 sem decisão arquitetural implícita.
