CREATE TABLE valor_a_receber_parcela_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    parcela_id UUID NOT NULL,
    vencimento DATE NOT NULL,
    valor_recebido NUMERIC(19, 2) NOT NULL DEFAULT 0,
    data_prometida DATE,
    data_efetiva_recebimento DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_valor_a_receber_parcela_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_valor_a_receber_parcela_parcela
        FOREIGN KEY (parcela_id) REFERENCES parcela_compra_cartao(id),
    CONSTRAINT fk_valor_a_receber_parcela_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_valor_a_receber_parcela_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_valor_a_receber_parcela_cartao_parcela
        UNIQUE (parcela_id),
    CONSTRAINT ck_valor_a_receber_parcela_valor_recebido
        CHECK (valor_recebido >= 0),
    CONSTRAINT ck_valor_a_receber_parcela_status
        CHECK (status IN ('PENDENTE', 'PARCIALMENTE_RESSARCIDA', 'RESSARCIDA', 'CANCELADA'))
);

CREATE INDEX ix_valor_a_receber_parcela_cartao_empresa ON valor_a_receber_parcela_cartao (empresa_id);
CREATE INDEX ix_valor_a_receber_parcela_cartao_empresa_status ON valor_a_receber_parcela_cartao (empresa_id, status);
CREATE INDEX ix_valor_a_receber_parcela_cartao_empresa_vencimento ON valor_a_receber_parcela_cartao (empresa_id, vencimento);

CREATE TABLE ressarcimento_parcela_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    valor_a_receber_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    data_ressarcimento DATE NOT NULL,
    forma_pagamento VARCHAR(30),
    observacao VARCHAR(500),
    lancamento_financeiro_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    estornado_em TIMESTAMPTZ,
    estornado_por_usuario_id UUID,
    motivo_estorno VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_ressarcimento_parcela_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_ressarcimento_parcela_valor_a_receber
        FOREIGN KEY (valor_a_receber_id) REFERENCES valor_a_receber_parcela_cartao(id),
    CONSTRAINT fk_ressarcimento_parcela_conta
        FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_ressarcimento_parcela_lancamento
        FOREIGN KEY (lancamento_financeiro_id) REFERENCES lancamento_financeiro(id),
    CONSTRAINT fk_ressarcimento_parcela_estornado_por
        FOREIGN KEY (estornado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_ressarcimento_parcela_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_ressarcimento_parcela_cartao_lancamento
        UNIQUE (lancamento_financeiro_id),
    CONSTRAINT ck_ressarcimento_parcela_valor
        CHECK (valor > 0),
    CONSTRAINT ck_ressarcimento_parcela_status
        CHECK (status IN ('ATIVO', 'ESTORNADO')),
    CONSTRAINT ck_ressarcimento_parcela_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO')),
    CONSTRAINT ck_ressarcimento_parcela_estorno
        CHECK (
            (status = 'ATIVO' AND estornado_em IS NULL AND estornado_por_usuario_id IS NULL)
            OR (status = 'ESTORNADO' AND estornado_em IS NOT NULL AND estornado_por_usuario_id IS NOT NULL)
        )
);

CREATE INDEX ix_ressarcimento_parcela_cartao_empresa ON ressarcimento_parcela_cartao (empresa_id);
CREATE INDEX ix_ressarcimento_parcela_cartao_empresa_valor_a_receber ON ressarcimento_parcela_cartao (empresa_id, valor_a_receber_id);
CREATE INDEX ix_ressarcimento_parcela_cartao_empresa_conta ON ressarcimento_parcela_cartao (empresa_id, conta_id);

-- Nova origem para lancamentos gerados por ressarcimento de compra para terceiro
-- (integral ou parcial). Preserva as origens ja existentes; apenas amplia a lista
-- aceita pelo check constraint criado na V9 e ampliado nas V11/V14.
ALTER TABLE lancamento_financeiro DROP CONSTRAINT ck_lancamento_financeiro_origem;
ALTER TABLE lancamento_financeiro ADD CONSTRAINT ck_lancamento_financeiro_origem
    CHECK (origem IN ('MANUAL', 'IMPORTACAO', 'RECORRENCIA', 'CARTAO', 'FATURA', 'CONCILIACAO', 'SISTEMA',
        'CONTA_A_PAGAR', 'EMPRESTIMO_CONCEDIDO', 'RESSARCIMENTO_COMPRA_TERCEIRO'));
