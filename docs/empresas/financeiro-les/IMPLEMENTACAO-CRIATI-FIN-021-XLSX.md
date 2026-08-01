# CRIATI-FIN-021 — Importação bancária XLSX

## Estratégia

A importação XLSX reutiliza o mesmo lote, transação importada, prévia, histórico, descarte, confirmação, hash e detecção de duplicidades dos formatos OFX e CSV. O upload apenas cria a prévia; nenhum lançamento financeiro é gerado automaticamente.

O endpoint é `POST /api/contexto/financeiro/importacoes-bancarias/xlsx`, com multipart `arquivo` e `contaId`. Somente `.xlsx` é aceito; `.xls` e documentos OOXML criptografados são rejeitados.

O parser usa Apache POI 5.5.1. Antes da abertura do workbook, a assinatura OOXML e a estrutura ZIP são verificadas com limites de entradas e bytes descompactados. A primeira planilha visível com cabeçalhos obrigatórios válidos é utilizada, mas todas as planilhas são percorridas para detectar conteúdo inseguro.

## Mapeamento

Os cabeçalhos são normalizados sem acentos e reconhecidos por uma lista fechada para data, descrição, valor, tipo/sinal, documento e identificador bancário. Data, descrição e valor precisam ser unívocos.

Datas aceitas: células de data do Excel ou textos `yyyy-MM-dd`, `dd/MM/yyyy`, `dd-MM-yyyy` e `yyyyMMdd`. Valores aceitam sinal e até duas casas decimais, com ponto ou vírgula decimal. Tipo/sinal aceita entrada/crédito, saída/débito, `+` ou `-`.

## Segurança e memória

- arquivo compactado limitado a 2 MiB por padrão;
- conteúdo descompactado limitado a 16 MiB e no máximo 200 entradas ZIP;
- proteção adicional do `ZipSecureFile`, com taxa mínima de compressão de 1%;
- até 5 planilhas, 10.000 transações, 50 colunas e 500 caracteres por célula;
- fórmulas, textos com prefixo de fórmula, hyperlinks, relações externas, células com erro e planilhas ocultas são rejeitados;
- entradas ZIP duplicadas, macros, controles ActiveX e objetos embutidos são rejeitados;
- nomes externos e caminhos internos inseguros são rejeitados;
- conteúdo bruto não é registrado em logs;
- falhas causam rollback integral;
- conta, lotes e transações permanecem isolados pela empresa do contexto autenticado.

Os limites podem ser sobrescritos pelas variáveis `CRIATI_IMPORTACAO_XLSX_TAMANHO_MAXIMO_BYTES`, `CRIATI_IMPORTACAO_XLSX_MAXIMO_PLANILHAS`, `CRIATI_IMPORTACAO_XLSX_MAXIMO_LINHAS`, `CRIATI_IMPORTACAO_XLSX_MAXIMO_COLUNAS`, `CRIATI_IMPORTACAO_XLSX_MAXIMO_CARACTERES_CELULA`, `CRIATI_IMPORTACAO_XLSX_MAXIMO_ENTRADAS_ZIP` e `CRIATI_IMPORTACAO_XLSX_MAXIMO_BYTES_DESCOMPACTADOS`.

## Banco

A migration `V23__adicionar_formato_xlsx_importacao_bancaria.sql` amplia somente a allowlist da constraint de formato para `OFX`, `CSV` e `XLSX`. Nenhuma entidade ou tabela específica para Excel foi criada.
