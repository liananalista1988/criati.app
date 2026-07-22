CREATE TABLE instituicao_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID,
    nome VARCHAR(150) NOT NULL,
    codigo VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_instituicao_financeira_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_instituicao_financeira_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_instituicao_financeira_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_instituicao_financeira_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE UNIQUE INDEX ux_instituicao_financeira_global_nome
    ON instituicao_financeira (UPPER(nome)) WHERE empresa_id IS NULL;
CREATE UNIQUE INDEX ux_instituicao_financeira_empresa_nome
    ON instituicao_financeira (empresa_id, UPPER(nome)) WHERE empresa_id IS NOT NULL;
CREATE INDEX ix_instituicao_financeira_empresa_status
    ON instituicao_financeira (empresa_id, status);

INSERT INTO instituicao_financeira (id, nome, codigo, status)
VALUES
    ('1b000000-0000-4000-8000-000000000001', 'Banco do Brasil', '001', 'ATIVO'),
    ('1b000000-0000-4000-8000-000000000077', 'Banco Inter', '077', 'ATIVO');

ALTER TABLE conta_financeira
    ADD COLUMN pessoa_titular_id UUID,
    ADD COLUMN instituicao_id UUID,
    ADD COLUMN moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    ADD COLUMN data_saldo_inicial DATE,
    ADD COLUMN permite_conciliacao BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN criado_por_usuario_id UUID,
    ADD COLUMN atualizado_por_usuario_id UUID;

UPDATE conta_financeira
SET data_saldo_inicial = CAST(criado_em AS DATE),
    permite_conciliacao = CASE
        WHEN tipo IN ('CONTA_CORRENTE', 'POUPANCA', 'INVESTIMENTO') THEN TRUE
        ELSE FALSE
    END;

ALTER TABLE conta_financeira
    ALTER COLUMN data_saldo_inicial SET NOT NULL,
    ADD CONSTRAINT fk_conta_financeira_pessoa_titular
        FOREIGN KEY (pessoa_titular_id) REFERENCES pessoa_financeira(id),
    ADD CONSTRAINT fk_conta_financeira_instituicao
        FOREIGN KEY (instituicao_id) REFERENCES instituicao_financeira(id),
    ADD CONSTRAINT fk_conta_financeira_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT fk_conta_financeira_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT ck_conta_financeira_moeda CHECK (moeda = 'BRL');

ALTER TABLE conta_financeira DROP CONSTRAINT ck_conta_financeira_tipo;
ALTER TABLE conta_financeira ADD CONSTRAINT ck_conta_financeira_tipo
    CHECK (tipo IN (
        'CAIXA', 'CONTA_CORRENTE', 'CONTA_PAGAMENTO', 'POUPANCA',
        'DINHEIRO', 'CARTEIRA', 'INVESTIMENTO', 'OUTRA'
    ));

CREATE INDEX ix_conta_financeira_empresa_titular
    ON conta_financeira (empresa_id, pessoa_titular_id);
CREATE INDEX ix_conta_financeira_empresa_instituicao
    ON conta_financeira (empresa_id, instituicao_id);
CREATE INDEX ix_conta_financeira_empresa_nome
    ON conta_financeira (empresa_id, nome);
CREATE INDEX ix_conta_financeira_empresa_conciliacao
    ON conta_financeira (empresa_id, permite_conciliacao, status);
