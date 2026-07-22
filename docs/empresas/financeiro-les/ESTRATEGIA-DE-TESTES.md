# Estratégia de Testes — Financeiro LeS

## Política de conclusão

Nenhuma tarefa é concluída apenas porque compila. A proporção de testes acompanha o risco e cada
funcionalidade inclui casos positivo, limite, erro, autorização e isolamento empresarial. Valores
financeiros de homologação são fictícios, determinísticos e conferíveis manualmente.

## Pirâmide de testes

### Unitários

- cálculos de saldo, consumo, orçamento, meta, exposição e projeção;
- arredondamento e soma de parcelas;
- invariantes, estados e transições;
- níveis de risco e recomendações explicadas;
- prevenção de duplicidade e precedência de categorização.

Devem ser rápidos, sem banco ou relógio não controlado. Resultados monetários usam precisão e
arredondamento explícitos.

### Integração

- mappings JPA, constraints, índices e relacionamentos;
- repositories com `id + empresa_id` e listas pelo tenant;
- transações atômicas, como transferência e pagamento;
- Flyway desde banco vazio e atualização de versão anterior;
- concorrência/idempotência em recorrências, parcelas e importações.

Teste obrigatório em toda entidade empresarial: usuário da Empresa A tenta acessar um UUID válido
da Empresa B e não lê, altera, restaura, baixa ou exclui o registro.

### MockMvc

- autenticação, CSRF, aplicação ativa, vínculo, perfil e permissão;
- endpoints e páginas para sucesso, validação, não encontrado e acesso negado;
- DTOs impedindo `empresa_id` e campos administrativos controlados pelo cliente;
- mensagens seguras sem stack trace, SQL ou existência de recurso alheio.

### Navegador

- jornadas críticas: primeiro cadastro, lançamento, transferência, compra parcelada, fechamento e
  pagamento de fatura, orçamento, terceiro e simulação;
- formulários, confirmação, foco, estados vazios/erro/carregamento;
- desktop e mobile, inicialmente 1280 × 720 e 360 × 800;
- atualização dos indicadores após cada operação.

## Suites por marco

| Marco | Regressão obrigatória |
|---|---|
| M1 | contexto, cadastros, lançamentos, transferência, saldos e tenant |
| M2 | recorrência, anexos, cartões, parcelas, faturas, estornos e pagamentos |
| M3 | fórmulas de orçamento/meta e rastreabilidade do dashboard |
| M4 | terceiros, projeções, risco e simulador |
| M5 | parsers, hashes, idempotência, conciliação e regras automáticas |
| M6 | formatos, falhas externas, consentimento, privacidade e idempotência |

## Dados e evidências

- Fixtures usam UUIDs e nomes fictícios explícitos, sem copiar dados familiares.
- Cada cenário financeiro registra entradas, cálculo esperado e regra de arredondamento.
- Evidências visuais não exibem e-mail, saldo, conta, limite ou comprovante real.
- Dados criados pelo teste são isolados e descartáveis.

## Gates

Antes de integrar uma tarefa: testes focados, suíte completa, `git diff --check`, migration em banco
limpo quando houver, inspeção de segurança e homologação do fluxo. Para o fechamento de fase,
executar também `clean verify`, revisar cobertura dos riscos e registrar pendências sem mascarar
falhas ou testes ignorados.
