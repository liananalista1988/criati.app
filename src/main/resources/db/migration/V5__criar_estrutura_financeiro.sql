CREATE TABLE conta_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    saldo_inicial NUMERIC(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conta_financeira_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT ck_conta_financeira_tipo
        CHECK (tipo IN ('CAIXA', 'CONTA_CORRENTE', 'POUPANCA', 'INVESTIMENTO', 'OUTRA')),
    CONSTRAINT ck_conta_financeira_status
        CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX ix_conta_financeira_empresa ON conta_financeira (empresa_id);
CREATE INDEX ix_conta_financeira_empresa_status ON conta_financeira (empresa_id, status);

CREATE TABLE categoria_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_categoria_financeira_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT ck_categoria_financeira_tipo
        CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_categoria_financeira_status
        CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX ix_categoria_financeira_empresa ON categoria_financeira (empresa_id);
CREATE INDEX ix_categoria_financeira_empresa_tipo ON categoria_financeira (empresa_id, tipo);

CREATE TABLE lancamento_financeiro (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    categoria_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    data_competencia DATE NOT NULL,
    data_pagamento DATE,
    status VARCHAR(20) NOT NULL,
    observacao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lancamento_financeiro_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_lancamento_financeiro_conta
        FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_lancamento_financeiro_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT ck_lancamento_financeiro_tipo
        CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_lancamento_financeiro_status
        CHECK (status IN ('PENDENTE', 'PAGO', 'CANCELADO')),
    CONSTRAINT ck_lancamento_financeiro_valor_positivo
        CHECK (valor > 0)
);

CREATE INDEX ix_lancamento_financeiro_empresa ON lancamento_financeiro (empresa_id);
CREATE INDEX ix_lancamento_financeiro_empresa_competencia ON lancamento_financeiro (empresa_id, data_competencia);
CREATE INDEX ix_lancamento_financeiro_empresa_status ON lancamento_financeiro (empresa_id, status);
CREATE INDEX ix_lancamento_financeiro_conta ON lancamento_financeiro (conta_id);
CREATE INDEX ix_lancamento_financeiro_categoria ON lancamento_financeiro (categoria_id);
