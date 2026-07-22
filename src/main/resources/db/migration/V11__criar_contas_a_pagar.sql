CREATE TABLE compromisso_financeiro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    categoria_id UUID NOT NULL,
    pessoa_financeira_id UUID NOT NULL,
    parte_financeira_id UUID,
    conta_padrao_id UUID,
    recorrencia_id UUID,
    tipo_valor VARCHAR(20) NOT NULL,
    valor_padrao NUMERIC(19, 2),
    dia_vencimento_padrao INTEGER,
    forma_pagamento_padrao VARCHAR(30),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    observacao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_compromisso_financeiro_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_compromisso_financeiro_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_compromisso_financeiro_pessoa
        FOREIGN KEY (pessoa_financeira_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_compromisso_financeiro_parte
        FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    CONSTRAINT fk_compromisso_financeiro_conta_padrao
        FOREIGN KEY (conta_padrao_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_compromisso_financeiro_recorrencia
        FOREIGN KEY (recorrencia_id) REFERENCES recorrencia_financeira(id),
    CONSTRAINT fk_compromisso_financeiro_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_compromisso_financeiro_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_compromisso_financeiro_recorrencia
        UNIQUE (recorrencia_id),
    CONSTRAINT ck_compromisso_financeiro_tipo_valor
        CHECK (tipo_valor IN ('FIXO', 'VARIAVEL')),
    CONSTRAINT ck_compromisso_financeiro_valor_padrao
        CHECK (valor_padrao IS NULL OR valor_padrao > 0),
    CONSTRAINT ck_compromisso_financeiro_dia_vencimento
        CHECK (dia_vencimento_padrao IS NULL OR dia_vencimento_padrao BETWEEN 1 AND 31),
    CONSTRAINT ck_compromisso_financeiro_forma_pagamento
        CHECK (forma_pagamento_padrao IS NULL OR forma_pagamento_padrao IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO'))
);

CREATE INDEX ix_compromisso_financeiro_empresa ON compromisso_financeiro (empresa_id);
CREATE INDEX ix_compromisso_financeiro_empresa_ativo ON compromisso_financeiro (empresa_id, ativo);
CREATE INDEX ix_compromisso_financeiro_empresa_pessoa ON compromisso_financeiro (empresa_id, pessoa_financeira_id);
CREATE INDEX ix_compromisso_financeiro_empresa_categoria ON compromisso_financeiro (empresa_id, categoria_id);

CREATE TABLE ocorrencia_compromisso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    compromisso_id UUID,
    recorrencia_id UUID,
    competencia DATE NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    categoria_id UUID NOT NULL,
    pessoa_financeira_id UUID NOT NULL,
    parte_financeira_id UUID,
    conta_prevista_id UUID,
    valor_previsto NUMERIC(19, 2),
    valor_principal NUMERIC(19, 2) NOT NULL,
    vencimento DATE NOT NULL,
    data_recebimento_cobranca DATE,
    status VARCHAR(30) NOT NULL,
    juros NUMERIC(19, 2) NOT NULL DEFAULT 0,
    multa NUMERIC(19, 2) NOT NULL DEFAULT 0,
    desconto NUMERIC(19, 2) NOT NULL DEFAULT 0,
    valor_pago NUMERIC(19, 2) NOT NULL DEFAULT 0,
    observacao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_ocorrencia_compromisso_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_ocorrencia_compromisso_compromisso
        FOREIGN KEY (compromisso_id) REFERENCES compromisso_financeiro(id),
    CONSTRAINT fk_ocorrencia_compromisso_recorrencia
        FOREIGN KEY (recorrencia_id) REFERENCES recorrencia_financeira(id),
    CONSTRAINT fk_ocorrencia_compromisso_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_ocorrencia_compromisso_pessoa
        FOREIGN KEY (pessoa_financeira_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_ocorrencia_compromisso_parte
        FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    CONSTRAINT fk_ocorrencia_compromisso_conta_prevista
        FOREIGN KEY (conta_prevista_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_ocorrencia_compromisso_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_ocorrencia_compromisso_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_ocorrencia_compromisso_recorrencia_competencia
        UNIQUE (recorrencia_id, competencia),
    CONSTRAINT uq_ocorrencia_compromisso_compromisso_competencia
        UNIQUE (compromisso_id, competencia),
    CONSTRAINT ck_ocorrencia_compromisso_status
        CHECK (status IN ('PENDENTE', 'PARCIALMENTE_PAGA', 'PAGA', 'CANCELADA')),
    CONSTRAINT ck_ocorrencia_compromisso_valor_previsto
        CHECK (valor_previsto IS NULL OR valor_previsto > 0),
    CONSTRAINT ck_ocorrencia_compromisso_valor_principal
        CHECK (valor_principal > 0),
    CONSTRAINT ck_ocorrencia_compromisso_juros
        CHECK (juros >= 0),
    CONSTRAINT ck_ocorrencia_compromisso_multa
        CHECK (multa >= 0),
    CONSTRAINT ck_ocorrencia_compromisso_desconto
        CHECK (desconto >= 0),
    CONSTRAINT ck_ocorrencia_compromisso_desconto_limite
        CHECK (desconto <= valor_principal + juros + multa),
    CONSTRAINT ck_ocorrencia_compromisso_valor_pago
        CHECK (valor_pago >= 0)
);

CREATE INDEX ix_ocorrencia_compromisso_empresa ON ocorrencia_compromisso (empresa_id);
CREATE INDEX ix_ocorrencia_compromisso_empresa_status ON ocorrencia_compromisso (empresa_id, status);
CREATE INDEX ix_ocorrencia_compromisso_empresa_vencimento ON ocorrencia_compromisso (empresa_id, vencimento);
CREATE INDEX ix_ocorrencia_compromisso_empresa_pessoa ON ocorrencia_compromisso (empresa_id, pessoa_financeira_id);
CREATE INDEX ix_ocorrencia_compromisso_empresa_categoria ON ocorrencia_compromisso (empresa_id, categoria_id);
CREATE INDEX ix_ocorrencia_compromisso_empresa_parte ON ocorrencia_compromisso (empresa_id, parte_financeira_id);
CREATE INDEX ix_ocorrencia_compromisso_empresa_compromisso ON ocorrencia_compromisso (empresa_id, compromisso_id);
CREATE INDEX ix_ocorrencia_compromisso_empresa_competencia ON ocorrencia_compromisso (empresa_id, competencia);

CREATE TABLE pagamento_ocorrencia_compromisso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    ocorrencia_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    data_pagamento DATE NOT NULL,
    forma_pagamento VARCHAR(30),
    observacao VARCHAR(500),
    lancamento_financeiro_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    estornado_em TIMESTAMPTZ,
    estornado_por_usuario_id UUID,
    motivo_estorno VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_pagamento_ocorrencia_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_pagamento_ocorrencia_ocorrencia
        FOREIGN KEY (ocorrencia_id) REFERENCES ocorrencia_compromisso(id),
    CONSTRAINT fk_pagamento_ocorrencia_conta
        FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_pagamento_ocorrencia_lancamento
        FOREIGN KEY (lancamento_financeiro_id) REFERENCES lancamento_financeiro(id),
    CONSTRAINT fk_pagamento_ocorrencia_estornado_por
        FOREIGN KEY (estornado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_pagamento_ocorrencia_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_pagamento_ocorrencia_lancamento
        UNIQUE (lancamento_financeiro_id),
    CONSTRAINT ck_pagamento_ocorrencia_valor
        CHECK (valor > 0),
    CONSTRAINT ck_pagamento_ocorrencia_status
        CHECK (status IN ('ATIVO', 'ESTORNADO')),
    CONSTRAINT ck_pagamento_ocorrencia_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO')),
    CONSTRAINT ck_pagamento_ocorrencia_estorno
        CHECK (
            (status = 'ATIVO' AND estornado_em IS NULL AND estornado_por_usuario_id IS NULL)
            OR (status = 'ESTORNADO' AND estornado_em IS NOT NULL AND estornado_por_usuario_id IS NOT NULL)
        )
);

CREATE INDEX ix_pagamento_ocorrencia_empresa ON pagamento_ocorrencia_compromisso (empresa_id);
CREATE INDEX ix_pagamento_ocorrencia_empresa_ocorrencia ON pagamento_ocorrencia_compromisso (empresa_id, ocorrencia_id);
CREATE INDEX ix_pagamento_ocorrencia_empresa_conta ON pagamento_ocorrencia_compromisso (empresa_id, conta_id);

-- Nova origem para lancamentos gerados por pagamento de conta a pagar (integral ou parcial).
-- Preserva as origens ja existentes; apenas amplia a lista aceita pelo check constraint criado na V9.
ALTER TABLE lancamento_financeiro DROP CONSTRAINT ck_lancamento_financeiro_origem;
ALTER TABLE lancamento_financeiro ADD CONSTRAINT ck_lancamento_financeiro_origem
    CHECK (origem IN ('MANUAL', 'IMPORTACAO', 'RECORRENCIA', 'CARTAO', 'FATURA', 'CONCILIACAO', 'SISTEMA', 'CONTA_A_PAGAR'));
