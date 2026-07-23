CREATE TABLE emprestimo_concedido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    parte_financeira_id UUID NOT NULL,
    categoria_id UUID NOT NULL,
    descricao VARCHAR(500),
    valor_principal NUMERIC(19, 2) NOT NULL,
    data_concessao DATE NOT NULL,
    tipo_cobranca VARCHAR(30) NOT NULL,
    percentual_juros NUMERIC(7, 4),
    percentual_multa NUMERIC(7, 4),
    forma_pagamento VARCHAR(20) NOT NULL,
    quantidade_parcelas INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    motivo_cancelamento VARCHAR(500),
    cancelado_em TIMESTAMPTZ,
    cancelado_por_usuario_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_emprestimo_concedido_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_emprestimo_concedido_parte
        FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    CONSTRAINT fk_emprestimo_concedido_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_emprestimo_concedido_cancelado_por
        FOREIGN KEY (cancelado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_emprestimo_concedido_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_emprestimo_concedido_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_emprestimo_concedido_valor_principal
        CHECK (valor_principal > 0),
    CONSTRAINT ck_emprestimo_concedido_tipo_cobranca
        CHECK (tipo_cobranca IN ('SEM_JUROS', 'COM_JUROS', 'ALERTA_ATRASO', 'MULTA_ATRASO', 'JUROS_MORA_ATRASO')),
    CONSTRAINT ck_emprestimo_concedido_percentual_juros
        CHECK (percentual_juros IS NULL OR percentual_juros > 0),
    CONSTRAINT ck_emprestimo_concedido_percentual_multa
        CHECK (percentual_multa IS NULL OR percentual_multa > 0),
    CONSTRAINT ck_emprestimo_concedido_forma_pagamento
        CHECK (forma_pagamento IN ('UNICO', 'PARCELADO')),
    CONSTRAINT ck_emprestimo_concedido_quantidade_parcelas
        CHECK (quantidade_parcelas >= 1),
    CONSTRAINT ck_emprestimo_concedido_status
        CHECK (status IN ('ATIVO', 'QUITADO', 'CANCELADO'))
);

CREATE INDEX ix_emprestimo_concedido_empresa ON emprestimo_concedido (empresa_id);
CREATE INDEX ix_emprestimo_concedido_empresa_status ON emprestimo_concedido (empresa_id, status);
CREATE INDEX ix_emprestimo_concedido_empresa_parte ON emprestimo_concedido (empresa_id, parte_financeira_id);
CREATE INDEX ix_emprestimo_concedido_empresa_categoria ON emprestimo_concedido (empresa_id, categoria_id);

CREATE TABLE parcela_emprestimo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    emprestimo_id UUID NOT NULL,
    numero INTEGER NOT NULL,
    total_parcelas INTEGER NOT NULL,
    valor_principal NUMERIC(19, 2) NOT NULL,
    vencimento DATE NOT NULL,
    data_prometida DATE,
    data_efetiva_pagamento DATE,
    juros NUMERIC(19, 2) NOT NULL DEFAULT 0,
    multa NUMERIC(19, 2) NOT NULL DEFAULT 0,
    valor_recebido NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_parcela_emprestimo_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_parcela_emprestimo_emprestimo
        FOREIGN KEY (emprestimo_id) REFERENCES emprestimo_concedido(id),
    CONSTRAINT fk_parcela_emprestimo_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_parcela_emprestimo_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uk_parcela_emprestimo_numero
        UNIQUE (emprestimo_id, numero),
    CONSTRAINT ck_parcela_emprestimo_numero
        CHECK (numero >= 1 AND numero <= total_parcelas),
    CONSTRAINT ck_parcela_emprestimo_valor_principal
        CHECK (valor_principal > 0),
    CONSTRAINT ck_parcela_emprestimo_juros
        CHECK (juros >= 0),
    CONSTRAINT ck_parcela_emprestimo_multa
        CHECK (multa >= 0),
    CONSTRAINT ck_parcela_emprestimo_valor_recebido
        CHECK (valor_recebido >= 0),
    CONSTRAINT ck_parcela_emprestimo_status
        CHECK (status IN ('PENDENTE', 'PARCIALMENTE_PAGO', 'PAGO', 'CANCELADO'))
);

CREATE INDEX ix_parcela_emprestimo_empresa ON parcela_emprestimo (empresa_id);
CREATE INDEX ix_parcela_emprestimo_empresa_status ON parcela_emprestimo (empresa_id, status);
CREATE INDEX ix_parcela_emprestimo_empresa_vencimento ON parcela_emprestimo (empresa_id, vencimento);
CREATE INDEX ix_parcela_emprestimo_emprestimo ON parcela_emprestimo (emprestimo_id);

CREATE TABLE recebimento_parcela_emprestimo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    parcela_id UUID NOT NULL,
    conta_id UUID NOT NULL,
    valor NUMERIC(19, 2) NOT NULL,
    data_recebimento DATE NOT NULL,
    forma_pagamento VARCHAR(30),
    observacao VARCHAR(500),
    lancamento_financeiro_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    estornado_em TIMESTAMPTZ,
    estornado_por_usuario_id UUID,
    motivo_estorno VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_recebimento_parcela_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_recebimento_parcela_parcela
        FOREIGN KEY (parcela_id) REFERENCES parcela_emprestimo(id),
    CONSTRAINT fk_recebimento_parcela_conta
        FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_recebimento_parcela_lancamento
        FOREIGN KEY (lancamento_financeiro_id) REFERENCES lancamento_financeiro(id),
    CONSTRAINT fk_recebimento_parcela_estornado_por
        FOREIGN KEY (estornado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_recebimento_parcela_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uq_recebimento_parcela_lancamento
        UNIQUE (lancamento_financeiro_id),
    CONSTRAINT ck_recebimento_parcela_valor
        CHECK (valor > 0),
    CONSTRAINT ck_recebimento_parcela_status
        CHECK (status IN ('ATIVO', 'ESTORNADO')),
    CONSTRAINT ck_recebimento_parcela_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO')),
    CONSTRAINT ck_recebimento_parcela_estorno
        CHECK (
            (status = 'ATIVO' AND estornado_em IS NULL AND estornado_por_usuario_id IS NULL)
            OR (status = 'ESTORNADO' AND estornado_em IS NOT NULL AND estornado_por_usuario_id IS NOT NULL)
        )
);

CREATE INDEX ix_recebimento_parcela_empresa ON recebimento_parcela_emprestimo (empresa_id);
CREATE INDEX ix_recebimento_parcela_empresa_parcela ON recebimento_parcela_emprestimo (empresa_id, parcela_id);
CREATE INDEX ix_recebimento_parcela_empresa_conta ON recebimento_parcela_emprestimo (empresa_id, conta_id);

-- Nova origem para lancamentos gerados por recebimento de parcela de emprestimo concedido
-- (integral ou parcial). Preserva as origens ja existentes; apenas amplia a lista aceita
-- pelo check constraint criado na V9 e ampliado na V11.
ALTER TABLE lancamento_financeiro DROP CONSTRAINT ck_lancamento_financeiro_origem;
ALTER TABLE lancamento_financeiro ADD CONSTRAINT ck_lancamento_financeiro_origem
    CHECK (origem IN ('MANUAL', 'IMPORTACAO', 'RECORRENCIA', 'CARTAO', 'FATURA', 'CONCILIACAO', 'SISTEMA',
        'CONTA_A_PAGAR', 'EMPRESTIMO_CONCEDIDO'));
