# CRIATI-FIN-019 — Importação bancária CSV

## Estratégia

A importação CSV reutiliza o lote, as transações, a prévia, o histórico, o descarte lógico e a detecção de duplicidades da importação OFX. O formato é registrado no lote e nenhuma transação cria lançamento financeiro ou é confirmada durante o upload.

O parser aceita apenas UTF-8, com BOM opcional no início, e detecta vírgula ou ponto e vírgula somente quando um único separador produz cabeçalhos válidos. Os cabeçalhos são normalizados sem acentos e reconhecidos por uma lista fechada para data, descrição, valor, tipo/sinal, documento e identificador bancário. Data, descrição e valor precisam ser unívocos; ambiguidades são rejeitadas.

Datas aceitas: `yyyy-MM-dd`, `dd/MM/yyyy`, `dd-MM-yyyy` e `yyyyMMdd`. Valores aceitam sinal e até duas casas decimais, com ponto ou vírgula decimal. O tipo opcional pode indicar crédito/entrada ou débito/saída; a coluna `sinal` aceita `+` ou `-`, sem permitir conflito entre entrada e valor negativo.

## Segurança e limites

- extensão obrigatória `.csv`, nome sem caminho e tamanho máximo configurável;
- UTF-8 estrito, controles inválidos e BOM interno rejeitados;
- células com prefixo de fórmula (`=`, `+`, `-` ou `@`) rejeitadas fora da coluna de valor;
- limites configuráveis de linhas, colunas e tamanho de campo;
- identificador bancário limitado a 150 caracteres e documento a 100, conforme o banco;
- conteúdo bruto não é registrado em logs;
- conta, lote e consultas permanecem vinculados à empresa do contexto autenticado;
- falhas provocam rollback integral.

Configurações: `CRIATI_IMPORTACAO_CSV_TAMANHO_MAXIMO_BYTES`, `CRIATI_IMPORTACAO_CSV_MAXIMO_LINHAS`, `CRIATI_IMPORTACAO_CSV_MAXIMO_COLUNAS` e `CRIATI_IMPORTACAO_CSV_MAXIMO_CARACTERES_CAMPO`.

## API e banco

O upload usa `POST /api/contexto/financeiro/importacoes-bancarias/csv` com multipart `arquivo` e `contaFinanceiraId`. Listagem, detalhe, prévia e descarte são os mesmos contratos já existentes para lotes bancários.

A migration `V22__adicionar_formato_csv_importacao_bancaria.sql` é necessária porque a constraint cumulativa de `formato` criada na V20 aceitava somente `OFX`. A V22 apenas amplia essa allowlist para `OFX` e `CSV`.
