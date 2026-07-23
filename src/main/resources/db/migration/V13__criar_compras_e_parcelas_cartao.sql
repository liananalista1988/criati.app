CREATE TABLE compra_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), empresa_id UUID NOT NULL,
    cartao_id UUID NOT NULL, cartao_principal_id UUID NOT NULL,
    pessoa_responsavel_id UUID NOT NULL, categoria_id UUID NOT NULL, parte_financeira_id UUID,
    descricao VARCHAR(200) NOT NULL, data_compra DATE NOT NULL,
    valor_total NUMERIC(19,2) NOT NULL, quantidade_parcelas INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL, observacao VARCHAR(500),
    motivo_cancelamento VARCHAR(500), cancelado_em TIMESTAMPTZ, cancelado_por_usuario_id UUID,
    motivo_estorno VARCHAR(500), estornado_em TIMESTAMPTZ, estornado_por_usuario_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL, atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_compra_cartao_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_compra_cartao_cartao FOREIGN KEY (cartao_id) REFERENCES cartao_credito(id),
    CONSTRAINT fk_compra_cartao_principal FOREIGN KEY (cartao_principal_id) REFERENCES cartao_credito(id),
    CONSTRAINT fk_compra_cartao_pessoa FOREIGN KEY (pessoa_responsavel_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_compra_cartao_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_compra_cartao_parte FOREIGN KEY (parte_financeira_id) REFERENCES parte_financeira(id),
    CONSTRAINT fk_compra_cartao_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_compra_cartao_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_compra_cartao_cancelado_por FOREIGN KEY (cancelado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_compra_cartao_estornado_por FOREIGN KEY (estornado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_compra_cartao_valor CHECK (valor_total > 0),
    CONSTRAINT ck_compra_cartao_parcelas CHECK (quantidade_parcelas >= 1),
    CONSTRAINT ck_compra_cartao_status CHECK (status IN ('ATIVA','CANCELADA','ESTORNADA'))
);

CREATE TABLE parcela_compra_cartao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), empresa_id UUID NOT NULL, compra_id UUID NOT NULL,
    numero INTEGER NOT NULL, total_parcelas INTEGER NOT NULL, valor NUMERIC(19,2) NOT NULL,
    competencia DATE NOT NULL, status VARCHAR(20) NOT NULL, fatura_id UUID,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL, atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_parcela_compra_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_parcela_compra_compra FOREIGN KEY (compra_id) REFERENCES compra_cartao(id),
    CONSTRAINT fk_parcela_compra_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_parcela_compra_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT uk_parcela_compra_numero UNIQUE (compra_id, numero),
    CONSTRAINT ck_parcela_compra_numero CHECK (numero >= 1 AND numero <= total_parcelas),
    CONSTRAINT ck_parcela_compra_valor CHECK (valor > 0),
    CONSTRAINT ck_parcela_compra_status CHECK (status IN ('ABERTA','CANCELADA','ESTORNADA'))
);

CREATE INDEX ix_compra_cartao_empresa ON compra_cartao (empresa_id);
CREATE INDEX ix_compra_cartao_empresa_principal_status ON compra_cartao (empresa_id, cartao_principal_id, status);
CREATE INDEX ix_compra_cartao_empresa_cartao ON compra_cartao (empresa_id, cartao_id);
CREATE INDEX ix_compra_cartao_empresa_pessoa ON compra_cartao (empresa_id, pessoa_responsavel_id);
CREATE INDEX ix_compra_cartao_empresa_categoria ON compra_cartao (empresa_id, categoria_id);
CREATE INDEX ix_compra_cartao_empresa_data ON compra_cartao (empresa_id, data_compra);
CREATE INDEX ix_parcela_compra_empresa_competencia ON parcela_compra_cartao (empresa_id, competencia, status);
CREATE INDEX ix_parcela_compra_compra ON parcela_compra_cartao (compra_id);
