-- LES-F3-005: estados de pagamento da fatura (PARCIALMENTE_PAGA, PAGA, ATRASADA)
-- e saldo financiado/encargos manuais, ambos derivados de compras/parcelas ja
-- existentes (V13) mais o novo componente carregado do ciclo anterior.

-- Identificacao estavel da categoria tecnica usada pelo pagamento da fatura.
-- O codigo e opcional para categorias criadas pelo usuario, mas unico por
-- empresa quando preenchido. O backfill preserva eventual categoria criada
-- por uma execucao anterior desta funcionalidade antes da correcao.
ALTER TABLE categoria_financeira
    ADD COLUMN codigo_sistema VARCHAR(80);

WITH categorias_existentes AS (
    SELECT DISTINCT ON (empresa_id) id
    FROM categoria_financeira
    WHERE tipo = 'DESPESA'
      AND UPPER(BTRIM(nome)) = UPPER('Pagamento de fatura de cartao')
    ORDER BY empresa_id, (status = 'ATIVO') DESC, criado_em, id
)
UPDATE categoria_financeira
SET codigo_sistema = 'PAGAMENTO_FATURA_CARTAO'
WHERE id IN (SELECT id FROM categorias_existentes);

ALTER TABLE categoria_financeira
    ADD CONSTRAINT uq_categoria_financeira_empresa_codigo_sistema
    UNIQUE (empresa_id, codigo_sistema);

ALTER TABLE fatura_cartao
    DROP CONSTRAINT ck_fatura_cartao_status;
ALTER TABLE fatura_cartao
    ADD CONSTRAINT ck_fatura_cartao_status
    CHECK (status IN ('ABERTA', 'FECHADA', 'PARCIALMENTE_PAGA', 'PAGA', 'ATRASADA'));

-- fechado_em/fechado_por_usuario_id continuam obrigatorios em qualquer estado
-- posterior a FECHADA (nenhum deles retrocede para ABERTA).
ALTER TABLE fatura_cartao
    DROP CONSTRAINT ck_fatura_cartao_fechamento;
ALTER TABLE fatura_cartao
    ADD CONSTRAINT ck_fatura_cartao_fechamento
    CHECK (
        (status = 'ABERTA' AND fechado_em IS NULL AND fechado_por_usuario_id IS NULL)
        OR (status IN ('FECHADA', 'PARCIALMENTE_PAGA', 'PAGA', 'ATRASADA')
            AND fechado_em IS NOT NULL AND fechado_por_usuario_id IS NOT NULL)
    );

-- Saldo herdado da fatura anterior nao quitada (congelado uma unica vez na
-- abertura, ver FaturaCartao#assumirSaldoFinanciado) e encargos manuais
-- (juros/multa, ver FaturaCartao#aplicarEncargos) - ambos somam ao valor
-- devido sem alterar valor_total (que continua representando apenas a soma
-- das parcelas da propria competencia).
ALTER TABLE fatura_cartao
    ADD COLUMN saldo_financiado_anterior NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE fatura_cartao
    ADD COLUMN juros NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE fatura_cartao
    ADD COLUMN multa NUMERIC(19, 2) NOT NULL DEFAULT 0;
ALTER TABLE fatura_cartao
    ADD CONSTRAINT ck_fatura_cartao_saldo_financiado CHECK (saldo_financiado_anterior >= 0);
ALTER TABLE fatura_cartao
    ADD CONSTRAINT ck_fatura_cartao_juros CHECK (juros >= 0);
ALTER TABLE fatura_cartao
    ADD CONSTRAINT ck_fatura_cartao_multa CHECK (multa >= 0);

-- Cada pagamento gera exatamente um LancamentoFinanceiro (origem FATURA, ja
-- aceita pelo check constraint de lancamento_financeiro desde a V14) - nunca
-- reaproveitado por outro pagamento. Sem coluna de estorno/status de
-- proposito: LES-F3-006 e responsavel por essa evolucao.
CREATE TABLE pagamento_fatura_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    fatura_id UUID NOT NULL,
    conta_pagamento_id UUID NOT NULL,
    data_pagamento DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    lancamento_financeiro_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_pagamento_fatura_cartao_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_pagamento_fatura_cartao_fatura
        FOREIGN KEY (fatura_id) REFERENCES fatura_cartao(id),
    CONSTRAINT fk_pagamento_fatura_cartao_conta
        FOREIGN KEY (conta_pagamento_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_pagamento_fatura_cartao_lancamento
        FOREIGN KEY (lancamento_financeiro_id) REFERENCES lancamento_financeiro(id),
    CONSTRAINT fk_pagamento_fatura_cartao_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_pagamento_fatura_cartao_lancamento
        UNIQUE (lancamento_financeiro_id),
    CONSTRAINT ck_pagamento_fatura_cartao_valor
        CHECK (valor > 0),
    CONSTRAINT ck_pagamento_fatura_cartao_tipo
        CHECK (tipo IN ('INTEGRAL', 'PARCIAL', 'MINIMO'))
);

CREATE INDEX ix_pagamento_fatura_cartao_empresa_fatura
    ON pagamento_fatura_cartao (empresa_id, fatura_id);
