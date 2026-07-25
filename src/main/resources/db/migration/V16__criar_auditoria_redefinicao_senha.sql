-- CRIATI-SEG-001: registro imutavel (insert-only) de cada redefinicao
-- administrativa de senha. Nunca guarda senha, confirmacao, hash anterior
-- ou hash novo — apenas quem fez (administrador), em quem (usuario_afetado),
-- em qual empresa, quando e qual acao. Nao ha coluna atualizado_em de
-- proposito: o registro nunca e alterado apos criado.
CREATE TABLE redefinicao_senha_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    administrador_id UUID NOT NULL,
    usuario_afetado_id UUID NOT NULL,
    acao VARCHAR(40) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redefinicao_senha_auditoria_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_redefinicao_senha_auditoria_administrador
        FOREIGN KEY (administrador_id) REFERENCES usuario(id),
    CONSTRAINT fk_redefinicao_senha_auditoria_usuario_afetado
        FOREIGN KEY (usuario_afetado_id) REFERENCES usuario(id),
    CONSTRAINT ck_redefinicao_senha_auditoria_acao
        CHECK (acao IN ('REDEFINICAO_ADMINISTRATIVA_SENHA'))
);

CREATE INDEX ix_redefinicao_senha_auditoria_empresa ON redefinicao_senha_auditoria (empresa_id);
CREATE INDEX ix_redefinicao_senha_auditoria_usuario_afetado ON redefinicao_senha_auditoria (empresa_id, usuario_afetado_id);
