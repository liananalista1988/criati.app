CREATE TABLE fatura_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    cartao_principal_id UUID NOT NULL,
    competencia DATE NOT NULL,
    periodo_inicial DATE NOT NULL,
    periodo_final DATE NOT NULL,
    data_fechamento DATE NOT NULL,
    data_vencimento DATE NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    fechado_em TIMESTAMPTZ,
    fechado_por_usuario_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_fatura_cartao_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_fatura_cartao_principal
        FOREIGN KEY (cartao_principal_id) REFERENCES cartao_credito(id),
    CONSTRAINT fk_fatura_cartao_fechado_por
        FOREIGN KEY (fechado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_fatura_cartao_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_fatura_cartao_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_fatura_cartao_empresa_principal_competencia
        UNIQUE (empresa_id, cartao_principal_id, competencia),
    CONSTRAINT ck_fatura_cartao_valor_total
        CHECK (valor_total >= 0),
    CONSTRAINT ck_fatura_cartao_status
        CHECK (status IN ('ABERTA', 'FECHADA')),
    CONSTRAINT ck_fatura_cartao_periodo
        CHECK (periodo_inicial <= periodo_final AND periodo_final = data_fechamento),
    CONSTRAINT ck_fatura_cartao_fechamento
        CHECK (
            (status = 'ABERTA' AND fechado_em IS NULL AND fechado_por_usuario_id IS NULL)
            OR (status = 'FECHADA' AND fechado_em IS NOT NULL AND fechado_por_usuario_id IS NOT NULL)
        )
);

CREATE INDEX ix_fatura_cartao_empresa_competencia
    ON fatura_cartao (empresa_id, competencia);
CREATE INDEX ix_fatura_cartao_empresa_principal_status
    ON fatura_cartao (empresa_id, cartao_principal_id, status);

ALTER TABLE parcela_compra_cartao
    ADD CONSTRAINT fk_parcela_compra_fatura
    FOREIGN KEY (fatura_id) REFERENCES fatura_cartao(id);

CREATE INDEX ix_parcela_compra_empresa_fatura
    ON parcela_compra_cartao (empresa_id, fatura_id);
