# CRIATI-FIN-023 — Perfis bancários Inter e Banco do Brasil

## Estratégia

Os parsers CSV e OFX selecionam um perfil de normalização antes de aplicar o
fallback genérico já existente. O perfil altera somente a leitura do arquivo;
lote, hash SHA-256, duplicidades, autorização, isolamento multiempresa,
confirmação e descarte continuam no domínio compartilhado.

O CSV Inter é reconhecido pelo conjunto completo e não ambíguo de cabeçalhos
`Data Lançamento`, `Histórico`, `Descrição`, `Valor` e `Saldo`. Linhas anteriores
ao cabeçalho são tratadas como preâmbulo. `Histórico` e `Descrição` são
normalizados e combinados, o valor usa a convenção brasileira e `Saldo` nunca é
convertido em transação. A ordem original do arquivo é preservada.

O OFX Banco do Brasil é reconhecido por `BANKID` igual a `1` ou `001`. Blocos
cujo `NAME` ou `MEMO` seja exatamente `Saldo Anterior` ou `Saldo do dia` são
informativos e ignorados antes da validação de data. Movimentações reais exigem
data, valor e `FITID` válidos. `NAME` e `MEMO` são combinados e o sinal original
do valor e o tipo bancário são preservados. `DTSTART` e `DTEND` não validam nem
substituem as datas individuais.

## Segurança e dados

- os limites existentes de tamanho, linhas, colunas e campos permanecem ativos;
- o fallback genérico mantém suas validações anteriores;
- nenhuma linha informativa genérica é ignorada fora do perfil Banco do Brasil;
- nenhum conteúdo bancário bruto é registrado em logs;
- fixtures usam exclusivamente empresas, identificadores, descrições e valores
  sintéticos;
- arquivos reais permanecem fora do repositório e foram usados somente para
  verificar a estrutura local do OFX;
- nenhuma confirmação ou criação automática de lançamento foi adicionada.

## Banco de dados

Não há alteração de entidade, tabela, constraint ou índice. Nenhuma migration é
necessária para os perfis de normalização.
