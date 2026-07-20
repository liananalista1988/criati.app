CREATE TABLE aplicacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    descricao VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_aplicacao_status
        CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE TABLE empresa_aplicacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    aplicacao_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_empresa_aplicacao_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_empresa_aplicacao_aplicacao
        FOREIGN KEY (aplicacao_id) REFERENCES aplicacao(id),
    CONSTRAINT uk_empresa_aplicacao
        UNIQUE (empresa_id, aplicacao_id),
    CONSTRAINT ck_empresa_aplicacao_status
        CHECK (status IN ('ATIVO', 'INATIVO'))
);

CREATE INDEX ix_empresa_aplicacao_empresa_status ON empresa_aplicacao (empresa_id, status);
CREATE INDEX ix_empresa_aplicacao_aplicacao ON empresa_aplicacao (aplicacao_id);

INSERT INTO aplicacao (codigo, nome, descricao, status)
VALUES
    ('FINANCEIRO', 'Gerenciador Financeiro', 'Controle financeiro, receitas, despesas, contas e resultados', 'ATIVO'),
    ('CLINICA', 'Gestao de Clinica', 'Pacientes, profissionais, agenda e atendimentos', 'ATIVO')
ON CONFLICT (codigo) DO NOTHING;
