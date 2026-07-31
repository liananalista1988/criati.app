# CRIATI-FIN-014 — Fundação segura da importação bancária OFX

## Objetivo e limite desta etapa

A importação OFX cria somente uma área de preparação rastreável. O upload valida e estrutura o
arquivo, persiste um lote e suas transações e devolve uma prévia. Nenhuma operação desta etapa cria,
edita, categoriza ou associa `LancamentoFinanceiro`; confirmação, categorização e conciliação ficam
para tarefas posteriores.

O identificador CRIATI-FIN-014 também aparece em um merge anterior sobre telas de compras para
terceiros. Este documento usa o título completo “Fundação segura da importação bancária OFX” para
eliminar ambiguidade, sem alterar o histórico Git existente.

## Modelo

- `LoteImportacaoBancaria`: empresa, conta, SHA-256 dos bytes recebidos, formato `OFX`, nome
  original validado, tamanho, status, contadores, data/usuário de criação e data/usuário de descarte.
- `TransacaoBancariaImportada`: empresa, lote, conta, sequência, data, valor assinado, tipo bancário,
  descrição, identificador bancário e documento quando disponíveis, chave de duplicidade e os
  indicadores `duplicadaNoArquivo` e `possivelmenteJaImportada`.
- Status do lote: `PREVIA_DISPONIVEL` e `DESCARTADO`. Descartar é lógico: lote e transações
  permanecem para histórico e auditoria.

A migration cumulativa é `V20__criar_importacao_bancaria.sql`. Todas as consultas operacionais
filtram `empresa_id`; IDs de outra empresa respondem como inexistentes.

## Arquivo e parser

Somente nomes simples terminados em `.ofx` são aceitos. Separadores de diretório, `..`, caracteres
de controle, arquivo vazio e conteúdo acima de `CRIATI_IMPORTACAO_OFX_TAMANHO_MAXIMO_BYTES` são
rejeitados. O nome é apenas metadado e nunca é usado para criar um caminho.

Os bytes são lidos exclusivamente do `MultipartFile` recebido e o SHA-256 é calculado sobre esses
mesmos bytes. O conteúdo bruto não é persistido no banco, no filesystem ou em logs. As representações
textuais dos DTOs omitem nome de arquivo, hash e dados das transações para reduzir exposição acidental
quando o log técnico do framework estiver em `DEBUG`. O parser aceita
OFX SGML/XML nos encodings usuais declarados pelo arquivo, limita-se às tags bancárias necessárias e
não instancia parser XML, não resolve DTD/entidades e não acessa rede ou filesystem. `DOCTYPE`,
`ENTITY`, conteúdo binário e estrutura incompatível são rejeitados.

Campos extraídos: `DTPOSTED`, `TRNAMT`, `TRNTYPE`, `MEMO`/`NAME`, `FITID` e
`CHECKNUM`/`REFNUM`. Data e valor são obrigatórios e validados; `FITID` pode estar ausente.

## Hash e duplicidades

- A constraint única `(empresa_id, hash_arquivo)` e a validação no serviço impedem importar o mesmo
  arquivo duas vezes para a mesma empresa. O mesmo hash pode existir em empresas diferentes.
- Cada transação recebe uma chave SHA-256: usa `FITID` normalizado quando disponível; sem ele, usa
  data, valor, tipo, descrição e documento normalizados.
- Repetições da chave dentro do arquivo são preservadas e marcadas como `duplicadaNoArquivo`.
- Uma chave já presente no histórico da mesma empresa e conta, inclusive em lote descartado, é marcada como
  `possivelmenteJaImportada`. É apenas um alerta; não ocorre conciliação nem exclusão automática.

## API e autorização

Base: `/api/contexto/financeiro/importacoes-bancarias`.

- `POST /ofx` (`multipart/form-data`, campos `contaId` e `arquivo`): valida, importa e retorna prévia.
- `GET /`: lista lotes da empresa atual.
- `GET /{id}`: consulta lote e transações da empresa atual.
- `POST /{id}/descartar`: descarte lógico.

Todas as rotas exigem autenticação, contexto empresarial ativo e módulo Financeiro habilitado.
Upload e descarte exigem perfil `ADMINISTRADOR` ou `GESTOR`; leitura permanece disponível ao usuário
autenticado do módulo. CSRF continua ativo nas operações de escrita.

## Configuração

```properties
criati.financeiro.importacao-ofx.tamanho-maximo-bytes=${CRIATI_IMPORTACAO_OFX_TAMANHO_MAXIMO_BYTES:1048576}
```

O limite padrão é 1 MiB. O limite global do servidor multipart continua sendo uma barreira adicional;
o menor limite efetivo prevalece.

## Garantias testadas

Os testes cobrem OFX válido/inválido, vazio, extensão/nome inseguros, tamanho, hash por empresa,
duplicidades internas e históricas, conta e consulta entre tenants, prévia sem lançamento, descarte,
rollback, encoding e caracteres especiais, sinais monetários, datas ausentes/inválidas, ausência de
identificador bancário, conteúdo malformado, autenticação, autorização e CSRF.
