-- Auditoria da redefinicao GLOBAL de senha (Superadministrador redefinindo a
-- senha de qualquer usuario da plataforma, sem depender de empresa ativa ou
-- vinculo). Tabela deliberadamente separada de redefinicao_senha_auditoria
-- (V16, exclusiva do fluxo empresarial): aqui nao ha empresa_id, e tanto
-- sucesso quanto tentativas negadas/falhas geram um registro (resultado +
-- motivo), diferente do fluxo empresarial que so audita sucesso.
-- Nunca guarda senha, confirmacao, hash anterior ou hash novo.
CREATE TABLE redefinicao_senha_global_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    administrador_id UUID NOT NULL,
    -- Sem FK proposital: uma tentativa contra um id que nao corresponde a
    -- nenhum usuario real (motivo USUARIO_NAO_ENCONTRADO) tambem precisa
    -- ser auditavel, e uma FK impediria justamente esse caso.
    usuario_alvo_id UUID NOT NULL,
    acao VARCHAR(50) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    motivo VARCHAR(30) NOT NULL,
    ip_origem VARCHAR(45),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redefinicao_senha_global_auditoria_administrador
        FOREIGN KEY (administrador_id) REFERENCES usuario(id),
    CONSTRAINT ck_redefinicao_senha_global_auditoria_acao
        CHECK (acao IN ('REDEFINICAO_ADMINISTRATIVA_SENHA_GLOBAL')),
    CONSTRAINT ck_redefinicao_senha_global_auditoria_resultado
        CHECK (resultado IN ('SUCESSO', 'NEGADO', 'FALHA_VALIDACAO', 'ERRO')),
    CONSTRAINT ck_redefinicao_senha_global_auditoria_motivo
        CHECK (motivo IN ('PROPRIO_USUARIO', 'USUARIO_INATIVO', 'SENHA_INVALIDA', 'SEM_PERMISSAO',
            'USUARIO_NAO_ENCONTRADO', 'REDEFINICAO_CONCLUIDA'))
);

CREATE INDEX ix_redefinicao_senha_global_auditoria_usuario_alvo ON redefinicao_senha_global_auditoria (usuario_alvo_id);
CREATE INDEX ix_redefinicao_senha_global_auditoria_administrador ON redefinicao_senha_global_auditoria (administrador_id);
