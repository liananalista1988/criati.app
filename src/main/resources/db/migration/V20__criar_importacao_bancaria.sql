CREATE TABLE lote_importacao_bancaria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    hash_arquivo VARCHAR(64) NOT NULL,
    formato VARCHAR(20) NOT NULL,
    nome_original VARCHAR(255) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    quantidade_transacoes INTEGER NOT NULL,
    quantidade_duplicadas_arquivo INTEGER NOT NULL,
    quantidade_possiveis_duplicadas INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    descartado_em TIMESTAMPTZ,
    descartado_por_usuario_id UUID,
    CONSTRAINT fk_lote_importacao_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_lote_importacao_conta FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_lote_importacao_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_lote_importacao_descartado_por FOREIGN KEY (descartado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_lote_importacao_empresa_hash UNIQUE (empresa_id, hash_arquivo),
    CONSTRAINT ck_lote_importacao_hash CHECK (hash_arquivo ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_lote_importacao_formato CHECK (formato IN ('OFX')),
    CONSTRAINT ck_lote_importacao_tamanho CHECK (tamanho_bytes > 0),
    CONSTRAINT ck_lote_importacao_status CHECK (status IN ('PREVIA_DISPONIVEL', 'DESCARTADO')),
    CONSTRAINT ck_lote_importacao_quantidades CHECK (
        quantidade_transacoes > 0
        AND quantidade_duplicadas_arquivo >= 0
        AND quantidade_possiveis_duplicadas >= 0
        AND quantidade_duplicadas_arquivo <= quantidade_transacoes
        AND quantidade_possiveis_duplicadas <= quantidade_transacoes
    ),
    CONSTRAINT ck_lote_importacao_descarte CHECK (
        (status = 'PREVIA_DISPONIVEL' AND descartado_em IS NULL AND descartado_por_usuario_id IS NULL)
        OR (status = 'DESCARTADO' AND descartado_em IS NOT NULL AND descartado_por_usuario_id IS NOT NULL)
    )
);

CREATE INDEX ix_lote_importacao_empresa_criado
    ON lote_importacao_bancaria (empresa_id, criado_em DESC);
CREATE INDEX ix_lote_importacao_empresa_conta
    ON lote_importacao_bancaria (empresa_id, conta_id);

CREATE TABLE transacao_bancaria_importada (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    sequencia INTEGER NOT NULL,
    data_transacao DATE NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    tipo_bancario VARCHAR(40) NOT NULL,
    descricao VARCHAR(500),
    identificador_bancario VARCHAR(150),
    documento VARCHAR(100),
    chave_duplicidade VARCHAR(64) NOT NULL,
    duplicada_no_arquivo BOOLEAN NOT NULL DEFAULT FALSE,
    possivelmente_ja_importada BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transacao_importada_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_transacao_importada_lote FOREIGN KEY (lote_id) REFERENCES lote_importacao_bancaria(id),
    CONSTRAINT fk_transacao_importada_conta FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT uq_transacao_importada_lote_sequencia UNIQUE (lote_id, sequencia),
    CONSTRAINT ck_transacao_importada_sequencia CHECK (sequencia > 0),
    CONSTRAINT ck_transacao_importada_valor CHECK (valor <> 0),
    CONSTRAINT ck_transacao_importada_chave CHECK (chave_duplicidade ~ '^[0-9a-f]{64}$')
);

CREATE INDEX ix_transacao_importada_empresa_lote
    ON transacao_bancaria_importada (empresa_id, lote_id, sequencia);
CREATE INDEX ix_transacao_importada_empresa_conta_chave
    ON transacao_bancaria_importada (empresa_id, conta_id, chave_duplicidade);
