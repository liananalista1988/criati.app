CREATE TABLE recorrencia_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    valor_padrao NUMERIC(19, 2) NOT NULL,
    conta_id UUID NOT NULL,
    categoria_id UUID NOT NULL,
    pessoa_financeira_id UUID NOT NULL,
    parte_financeira_id UUID,
    forma_pagamento VARCHAR(30),
    periodicidade VARCHAR(20) NOT NULL,
    intervalo INTEGER NOT NULL,
    dia_referencia INTEGER NOT NULL,
    mes_referencia INTEGER,
    data_inicial DATE NOT NULL,
    data_final DATE,
    proxima_competencia DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    gerar_automaticamente BOOLEAN NOT NULL DEFAULT FALSE,
    observacao VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID,
    atualizado_por_usuario_id UUID,
    pausada_em TIMESTAMPTZ,
    pausada_por_usuario_id UUID,
    encerrada_em TIMESTAMPTZ,
    encerrada_por_usuario_id UUID,
    CONSTRAINT fk_recorrencia_financeira_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_recorrencia_financeira_conta
        FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_recorrencia_financeira_categoria
        FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_recorrencia_financeira_pessoa
        FOREIGN KEY (pessoa_financeira_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_recorrencia_financeira_parte
        FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    CONSTRAINT fk_recorrencia_financeira_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_recorrencia_financeira_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_recorrencia_financeira_pausada_por
        FOREIGN KEY (pausada_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_recorrencia_financeira_encerrada_por
        FOREIGN KEY (encerrada_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_recorrencia_financeira_tipo
        CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_recorrencia_financeira_periodicidade
        CHECK (periodicidade IN ('MENSAL', 'ANUAL')),
    CONSTRAINT ck_recorrencia_financeira_status
        CHECK (status IN ('ATIVA', 'PAUSADA', 'ENCERRADA')),
    CONSTRAINT ck_recorrencia_financeira_valor_positivo
        CHECK (valor_padrao > 0),
    CONSTRAINT ck_recorrencia_financeira_intervalo
        CHECK (intervalo > 0),
    CONSTRAINT ck_recorrencia_financeira_dia
        CHECK (dia_referencia BETWEEN 1 AND 31),
    CONSTRAINT ck_recorrencia_financeira_mes
        CHECK (mes_referencia IS NULL OR mes_referencia BETWEEN 1 AND 12),
    CONSTRAINT ck_recorrencia_financeira_mes_anual
        CHECK (
            (periodicidade = 'ANUAL' AND mes_referencia IS NOT NULL)
            OR (periodicidade <> 'ANUAL' AND mes_referencia IS NULL)
        ),
    CONSTRAINT ck_recorrencia_financeira_datas
        CHECK (data_final IS NULL OR data_final >= data_inicial),
    CONSTRAINT ck_recorrencia_financeira_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN
            ('PIX', 'DEBITO', 'DINHEIRO', 'BOLETO', 'TRANSFERENCIA', 'OUTRA', 'CREDITO'))
);

CREATE INDEX ix_recorrencia_financeira_empresa ON recorrencia_financeira (empresa_id);
CREATE INDEX ix_recorrencia_financeira_empresa_status ON recorrencia_financeira (empresa_id, status);
CREATE INDEX ix_recorrencia_financeira_empresa_proxima_geracao ON recorrencia_financeira (empresa_id, proxima_competencia);
CREATE INDEX ix_recorrencia_financeira_empresa_pessoa ON recorrencia_financeira (empresa_id, pessoa_financeira_id);
CREATE INDEX ix_recorrencia_financeira_empresa_categoria ON recorrencia_financeira (empresa_id, categoria_id);
CREATE INDEX ix_recorrencia_financeira_empresa_periodicidade ON recorrencia_financeira (empresa_id, periodicidade);

ALTER TABLE lancamento_financeiro
    ADD COLUMN recorrencia_id UUID,
    ADD CONSTRAINT fk_lancamento_financeiro_recorrencia
        FOREIGN KEY (recorrencia_id) REFERENCES recorrencia_financeira(id),
    ADD CONSTRAINT uq_lancamento_financeiro_recorrencia_competencia
        UNIQUE (recorrencia_id, data_competencia);

CREATE INDEX ix_lancamento_financeiro_empresa_recorrencia
    ON lancamento_financeiro (empresa_id, recorrencia_id);
