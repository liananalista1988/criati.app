# Dependências entre Módulos — Financeiro LeS

## Regra de direção

O núcleo compartilhado não depende das extensões residenciais. Controllers chamam services;
services aplicam contexto, autorização, transação e invariantes; repositories sempre filtram
dados empresariais por `empresa_id`. Integrações entre módulos ocorrem por serviços ou contratos
explícitos, sem acesso direto ao repository de outro módulo.

## Mapa de dependências

```text
Fundação SaaS (segurança, tenant, empresa, usuário, aplicação)
  └─ Núcleo financeiro compartilhado
      ├─ pessoa e parte financeira
      ├─ contas e categorias
      └─ lançamentos e transferências
          ├─ contas a pagar e recorrências
          ├─ cartões → compras → parcelas → faturas → pagamentos/estornos
          │                         └─ limite saudável
          ├─ orçamento e meta
          └─ terceiros (compromissos, empréstimos e recebíveis)
              └─ projeções → Simulador de Gastos

Indicadores de cada ramo ───────────→ Dashboard Familiar
Dados operacionais estáveis ────────→ importação → conciliação → categorização
Contratos estáveis ─────────────────→ exportações, agenda e integrações
```

## Matriz

| Consumidor | Dependências obrigatórias | Não deve depender de |
|---|---|---|
| Pessoa/parte financeira | tenant, empresa, usuário opcional | cartão, fatura |
| Conta/categoria | tenant e núcleo compartilhado | extensão residencial |
| Lançamento | conta, categoria, parte financeira opcional | dashboard |
| Transferência | duas contas da mesma empresa, lançamento | receita/despesa artificial |
| Conta a pagar | parte, categoria, recorrência, lançamento na liquidação | cartão |
| Cartão | pessoa, conta pagadora, catálogos | compra futura |
| Compra | cartão, pessoa, categoria, parte | pagamento de fatura |
| Parcela | compra e regra de arredondamento | importação |
| Fatura | cartão principal e parcelas | dashboard |
| Pagamento de fatura | fatura e conta pagadora | novo consumo |
| Orçamento/meta | lançamentos, compras, faturas e competência | simulador |
| Terceiros | parte, conta, lançamento/recebível | consumo residencial agregado |
| Dashboard | serviços de consulta dos módulos concluídos | repositories alheios |
| Simulador | orçamento, meta, projeções, faturas, parcelas e terceiros | persistência obrigatória |
| Conciliação | importação e movimentos existentes | OCR/PDF |
| Categorização | conciliação e histórico suficiente | recomendação opaca |

## Regras multiempresa transversais

- O `empresa_id` vem do contexto autenticado, nunca de URL, formulário ou JavaScript.
- Toda busca empresarial combina identificador do recurso e empresa atual.
- Relações compostas validam que origem e destino pertencem à mesma empresa.
- Um UUID válido de outra empresa resulta em não encontrado ou acesso negado sem revelar dados.
- Pessoa vinculada a usuário não transfere automaticamente permissões ou contexto.
- Instituições/bandeiras globais, se existirem, são catálogo explícito; registros empresariais
  continuam isolados.
- Auditoria, anexos, lixeira, importações e regras automáticas preservam o mesmo tenant.

## Caminho crítico

O caminho de maior risco é `conta → cartão → compra → parcela → fatura → pagamento → orçamento →
projeção → simulador`. Alterações de contrato nesse caminho exigem revisão das dependências a
jusante. Dashboard pode ser incrementado após cada fonte, mas não autoriza antecipar a fonte.
