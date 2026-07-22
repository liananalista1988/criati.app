CREATE TABLE pessoa_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    usuario_id UUID,
    apelido VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_pessoa_financeira_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_pessoa_financeira_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_pessoa_financeira_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_pessoa_financeira_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_pessoa_financeira_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX ix_pessoa_financeira_empresa ON pessoa_financeira (empresa_id);
CREATE INDEX ix_pessoa_financeira_empresa_status ON pessoa_financeira (empresa_id, status);
CREATE UNIQUE INDEX ux_pessoa_financeira_empresa_usuario_ativo
    ON pessoa_financeira (empresa_id, usuario_id)
    WHERE usuario_id IS NOT NULL AND status = 'ATIVO';

CREATE TABLE parte_financeira (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    documento VARCHAR(30),
    apelido VARCHAR(100),
    observacao VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID NOT NULL,
    CONSTRAINT fk_parte_financeira_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_parte_financeira_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_parte_financeira_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_parte_financeira_tipo
        CHECK (tipo IN ('PESSOA', 'ORGANIZACAO', 'ESTABELECIMENTO', 'OUTRA')),
    CONSTRAINT ck_parte_financeira_status CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX ix_parte_financeira_empresa ON parte_financeira (empresa_id);
CREATE INDEX ix_parte_financeira_empresa_status ON parte_financeira (empresa_id, status);
CREATE INDEX ix_parte_financeira_empresa_nome ON parte_financeira (empresa_id, nome);
