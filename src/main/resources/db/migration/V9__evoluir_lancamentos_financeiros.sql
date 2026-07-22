ALTER TABLE lancamento_financeiro
    ADD COLUMN pessoa_financeira_id UUID,
    ADD COLUMN parte_financeira_id UUID,
    ADD COLUMN data_vencimento DATE,
    ADD COLUMN forma_pagamento VARCHAR(30),
    ADD COLUMN origem VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN criado_por_usuario_id UUID,
    ADD COLUMN atualizado_por_usuario_id UUID,
    ADD CONSTRAINT fk_lancamento_financeiro_pessoa
        FOREIGN KEY (pessoa_financeira_id) REFERENCES pessoa_financeira(id),
    ADD CONSTRAINT fk_lancamento_financeiro_parte
        FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    ADD CONSTRAINT fk_lancamento_financeiro_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT fk_lancamento_financeiro_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT ck_lancamento_financeiro_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO')),
    ADD CONSTRAINT ck_lancamento_financeiro_origem
        CHECK (origem IN ('MANUAL', 'IMPORTACAO', 'RECORRENCIA', 'CARTAO', 'FATURA', 'CONCILIACAO', 'SISTEMA'));

ALTER TABLE lancamento_financeiro DROP CONSTRAINT ck_lancamento_financeiro_status;
ALTER TABLE lancamento_financeiro ADD CONSTRAINT ck_lancamento_financeiro_status
    CHECK (status IN ('PENDENTE', 'LIQUIDADO', 'PAGO', 'CANCELADO'));
ALTER TABLE lancamento_financeiro ADD CONSTRAINT ck_lancamento_financeiro_liquidacao
    CHECK (
        (status = 'PENDENTE' AND data_pagamento IS NULL)
        OR (status IN ('LIQUIDADO', 'PAGO') AND data_pagamento IS NOT NULL)
        OR status = 'CANCELADO'
    );

CREATE INDEX ix_lancamento_financeiro_empresa_pessoa
    ON lancamento_financeiro (empresa_id, pessoa_financeira_id);
CREATE INDEX ix_lancamento_financeiro_empresa_parte
    ON lancamento_financeiro (empresa_id, parte_financeira_id);
CREATE INDEX ix_lancamento_financeiro_empresa_vencimento
    ON lancamento_financeiro (empresa_id, data_vencimento);
CREATE INDEX ix_lancamento_financeiro_empresa_liquidacao
    ON lancamento_financeiro (empresa_id, data_pagamento);
