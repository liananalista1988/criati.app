CREATE TABLE convite (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    email VARCHAR(180) NOT NULL,
    perfil VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    utilizado_em TIMESTAMPTZ,
    criado_por_usuario_id UUID NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_convite_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_convite_criado_por_usuario
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_convite_perfil
        CHECK (perfil IN ('ADMINISTRADOR', 'GESTOR', 'USUARIO')),
    CONSTRAINT ck_convite_status
        CHECK (status IN ('PENDENTE', 'UTILIZADO', 'EXPIRADO', 'REVOGADO'))
);

CREATE INDEX ix_convite_empresa_email_status ON convite (empresa_id, email, status);
