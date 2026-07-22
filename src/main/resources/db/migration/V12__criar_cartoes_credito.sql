CREATE TABLE cartao_credito (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    pessoa_titular_id UUID NOT NULL,
    instituicao_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cartao_principal_id UUID,
    bandeira VARCHAR(30) NOT NULL,
    ultimos_quatro_digitos VARCHAR(4),
    limite_total NUMERIC(19, 2),
    limite_saudavel NUMERIC(19, 2),
    dia_fechamento INTEGER,
    dia_vencimento INTEGER,
    status VARCHAR(20) NOT NULL,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueio VARCHAR(500),
    observacao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_cartao_credito_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_cartao_credito_titular
        FOREIGN KEY (pessoa_titular_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_cartao_credito_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao_financeira(id),
    CONSTRAINT fk_cartao_credito_principal
        FOREIGN KEY (cartao_principal_id) REFERENCES cartao_credito(id),
    CONSTRAINT fk_cartao_credito_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_cartao_credito_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_cartao_credito_tipo
        CHECK (tipo IN ('FISICO', 'VIRTUAL')),
    CONSTRAINT ck_cartao_credito_bandeira
        CHECK (bandeira IN ('VISA', 'MASTERCARD', 'ELO', 'AMERICAN_EXPRESS', 'HIPERCARD', 'OUTRA')),
    CONSTRAINT ck_cartao_credito_status
        CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT ck_cartao_credito_limite_total
        CHECK (limite_total IS NULL OR limite_total >= 0),
    CONSTRAINT ck_cartao_credito_limite_saudavel
        CHECK (limite_saudavel IS NULL OR limite_saudavel >= 0),
    CONSTRAINT ck_cartao_credito_limite_saudavel_max
        CHECK (limite_saudavel IS NULL OR limite_total IS NULL OR limite_saudavel <= limite_total),
    CONSTRAINT ck_cartao_credito_fechamento
        CHECK (dia_fechamento IS NULL OR dia_fechamento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_credito_vencimento
        CHECK (dia_vencimento IS NULL OR dia_vencimento BETWEEN 1 AND 31),
    CONSTRAINT ck_cartao_credito_ultimos_digitos
        CHECK (ultimos_quatro_digitos IS NULL OR ultimos_quatro_digitos ~ '^[0-9]{4}$'),
    CONSTRAINT ck_cartao_credito_principal_diferente
        CHECK (cartao_principal_id IS NULL OR cartao_principal_id <> id),
    CONSTRAINT ck_cartao_credito_virtual_exige_principal
        CHECK (
            (tipo = 'VIRTUAL' AND cartao_principal_id IS NOT NULL)
            OR (tipo = 'FISICO' AND cartao_principal_id IS NULL)
        )
);

CREATE INDEX ix_cartao_credito_empresa ON cartao_credito (empresa_id);
CREATE INDEX ix_cartao_credito_empresa_titular ON cartao_credito (empresa_id, pessoa_titular_id);
CREATE INDEX ix_cartao_credito_empresa_instituicao ON cartao_credito (empresa_id, instituicao_id);
CREATE INDEX ix_cartao_credito_empresa_status ON cartao_credito (empresa_id, status);
CREATE INDEX ix_cartao_credito_empresa_bloqueado ON cartao_credito (empresa_id, bloqueado);
CREATE INDEX ix_cartao_credito_empresa_principal ON cartao_credito (empresa_id, cartao_principal_id);
CREATE INDEX ix_cartao_credito_empresa_tipo ON cartao_credito (empresa_id, tipo);
