ALTER TABLE transacao_bancaria_importada
    ADD COLUMN situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN lancamento_financeiro_id UUID,
    ADD COLUMN confirmada_em TIMESTAMPTZ,
    ADD COLUMN confirmada_por_usuario_id UUID,
    ADD COLUMN ignorada_em TIMESTAMPTZ,
    ADD COLUMN ignorada_por_usuario_id UUID,
    ADD CONSTRAINT fk_transacao_importada_confirmada_por
        FOREIGN KEY (confirmada_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT fk_transacao_importada_ignorada_por
        FOREIGN KEY (ignorada_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT uq_transacao_importada_lancamento UNIQUE (lancamento_financeiro_id),
    ADD CONSTRAINT ck_transacao_importada_situacao CHECK (situacao IN ('PENDENTE', 'CONFIRMADA', 'IGNORADA')),
    ADD CONSTRAINT ck_transacao_importada_processamento CHECK (
        (situacao = 'PENDENTE'
            AND lancamento_financeiro_id IS NULL
            AND confirmada_em IS NULL AND confirmada_por_usuario_id IS NULL
            AND ignorada_em IS NULL AND ignorada_por_usuario_id IS NULL)
        OR (situacao = 'CONFIRMADA'
            AND lancamento_financeiro_id IS NOT NULL
            AND confirmada_em IS NOT NULL AND confirmada_por_usuario_id IS NOT NULL
            AND ignorada_em IS NULL AND ignorada_por_usuario_id IS NULL)
        OR (situacao = 'IGNORADA'
            AND lancamento_financeiro_id IS NULL
            AND confirmada_em IS NULL AND confirmada_por_usuario_id IS NULL
            AND ignorada_em IS NOT NULL AND ignorada_por_usuario_id IS NOT NULL)
    );

ALTER TABLE lancamento_financeiro
    ADD CONSTRAINT uq_lancamento_financeiro_empresa_id UNIQUE (empresa_id, id);

ALTER TABLE transacao_bancaria_importada
    ADD CONSTRAINT fk_transacao_importada_lancamento_empresa
        FOREIGN KEY (empresa_id, lancamento_financeiro_id)
        REFERENCES lancamento_financeiro (empresa_id, id);

CREATE INDEX ix_transacao_importada_empresa_lote_situacao
    ON transacao_bancaria_importada (empresa_id, lote_id, situacao, sequencia);
