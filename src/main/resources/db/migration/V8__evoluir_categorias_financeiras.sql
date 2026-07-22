ALTER TABLE categoria_financeira
    ADD COLUMN descricao VARCHAR(500),
    ADD COLUMN categoria_pai_id UUID,
    ADD COLUMN ordem_exibicao INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN permite_orcamento BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN criado_por_usuario_id UUID,
    ADD COLUMN atualizado_por_usuario_id UUID,
    ADD CONSTRAINT fk_categoria_financeira_pai
        FOREIGN KEY (categoria_pai_id) REFERENCES categoria_financeira(id),
    ADD CONSTRAINT fk_categoria_financeira_criado_por
        FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT fk_categoria_financeira_atualizado_por
        FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    ADD CONSTRAINT ck_categoria_financeira_ordem_nao_negativa CHECK (ordem_exibicao >= 0),
    ADD CONSTRAINT ck_categoria_financeira_pai_diferente CHECK (categoria_pai_id IS NULL OR categoria_pai_id <> id);

UPDATE categoria_financeira SET permite_orcamento = TRUE WHERE tipo = 'DESPESA';

CREATE UNIQUE INDEX ux_categoria_financeira_raiz_ativa
    ON categoria_financeira (empresa_id, tipo, UPPER(BTRIM(nome)))
    WHERE categoria_pai_id IS NULL AND status = 'ATIVO';
CREATE UNIQUE INDEX ux_categoria_financeira_filha_ativa
    ON categoria_financeira (empresa_id, categoria_pai_id, tipo, UPPER(BTRIM(nome)))
    WHERE categoria_pai_id IS NOT NULL AND status = 'ATIVO';
CREATE INDEX ix_categoria_financeira_empresa_status_ordem
    ON categoria_financeira (empresa_id, status, ordem_exibicao, nome);
CREATE INDEX ix_categoria_financeira_empresa_pai
    ON categoria_financeira (empresa_id, categoria_pai_id);
CREATE INDEX ix_categoria_financeira_empresa_orcamento
    ON categoria_financeira (empresa_id, permite_orcamento, status);
