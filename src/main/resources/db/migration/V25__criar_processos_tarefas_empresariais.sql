CREATE TABLE processo_empresarial (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT,
    responsavel_usuario_empresa_id UUID,
    situacao VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    prioridade VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    data_abertura DATE NOT NULL DEFAULT CURRENT_DATE,
    prazo DATE,
    data_conclusao TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_processo_trabalho_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_processo_trabalho_responsavel FOREIGN KEY (responsavel_usuario_empresa_id) REFERENCES usuario_empresa(id),
    CONSTRAINT fk_processo_trabalho_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_processo_trabalho_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_processo_trabalho_situacao
        CHECK (situacao IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_processo_trabalho_prioridade
        CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT ck_processo_trabalho_status
        CHECK (status IN ('ATIVO', 'INATIVO')),
    -- Consistencia entre situacao e data_conclusao reforcada tambem no banco,
    -- nao apenas no service: concluido sempre tem data, os demais nunca tem.
    CONSTRAINT ck_processo_trabalho_conclusao
        CHECK ((situacao = 'CONCLUIDO') = (data_conclusao IS NOT NULL)),
    -- Suporta a FK composta de tarefa_empresarial.processo_id abaixo, garantindo
    -- que uma tarefa so referencie processo da mesma empresa.
    CONSTRAINT uq_processo_trabalho_id_empresa UNIQUE (id, empresa_id)
);

CREATE INDEX ix_processo_trabalho_empresa_situacao ON processo_empresarial (empresa_id, situacao);
CREATE INDEX ix_processo_trabalho_empresa_responsavel ON processo_empresarial (empresa_id, responsavel_usuario_empresa_id);
CREATE INDEX ix_processo_trabalho_empresa_prazo ON processo_empresarial (empresa_id, prazo);

CREATE TABLE tarefa_empresarial (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    processo_id UUID,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT,
    responsavel_usuario_empresa_id UUID,
    situacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    prioridade VARCHAR(10) NOT NULL DEFAULT 'MEDIA',
    prazo DATE,
    data_conclusao TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_tarefa_trabalho_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    -- FK composta: garante que a tarefa nunca aponte para um processo de outra empresa.
    CONSTRAINT fk_tarefa_trabalho_processo FOREIGN KEY (processo_id, empresa_id)
        REFERENCES processo_empresarial (id, empresa_id),
    CONSTRAINT fk_tarefa_trabalho_responsavel FOREIGN KEY (responsavel_usuario_empresa_id) REFERENCES usuario_empresa(id),
    CONSTRAINT fk_tarefa_trabalho_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_tarefa_trabalho_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_tarefa_trabalho_situacao
        CHECK (situacao IN ('PENDENTE', 'EM_ANDAMENTO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_tarefa_trabalho_prioridade
        CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA', 'URGENTE')),
    CONSTRAINT ck_tarefa_trabalho_status
        CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT ck_tarefa_trabalho_conclusao
        CHECK ((situacao = 'CONCLUIDA') = (data_conclusao IS NOT NULL)),
    CONSTRAINT uq_tarefa_trabalho_id_empresa UNIQUE (id, empresa_id)
);

CREATE INDEX ix_tarefa_trabalho_empresa_situacao ON tarefa_empresarial (empresa_id, situacao);
CREATE INDEX ix_tarefa_trabalho_empresa_responsavel ON tarefa_empresarial (empresa_id, responsavel_usuario_empresa_id);
CREATE INDEX ix_tarefa_trabalho_empresa_prazo ON tarefa_empresarial (empresa_id, prazo);
CREATE INDEX ix_tarefa_trabalho_empresa_processo ON tarefa_empresarial (empresa_id, processo_id);

-- Historico insert-only (sem atualizado_em, sem UPDATE/DELETE pela aplicacao).
-- Nao usa ON DELETE CASCADE: processo/tarefa nunca sao apagados fisicamente
-- pela operacao comum (apenas inativados), entao o historico nunca fica orfao.
CREATE TABLE historico_trabalho (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    processo_id UUID,
    tarefa_id UUID,
    tipo_evento VARCHAR(30) NOT NULL,
    descricao VARCHAR(500),
    valor_anterior VARCHAR(200),
    valor_novo VARCHAR(200),
    autor_usuario_id UUID NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_trabalho_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_historico_trabalho_processo FOREIGN KEY (processo_id, empresa_id)
        REFERENCES processo_empresarial (id, empresa_id),
    CONSTRAINT fk_historico_trabalho_tarefa FOREIGN KEY (tarefa_id, empresa_id)
        REFERENCES tarefa_empresarial (id, empresa_id),
    CONSTRAINT fk_historico_trabalho_autor FOREIGN KEY (autor_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_historico_trabalho_tipo_evento
        CHECK (tipo_evento IN ('CRIACAO', 'ALTERACAO_RESPONSAVEL', 'ALTERACAO_SITUACAO', 'ALTERACAO_PRAZO',
            'CONCLUSAO', 'REABERTURA', 'INATIVACAO')),
    -- Exatamente um dos dois alvos deve estar preenchido (nunca ambos, nunca nenhum).
    CONSTRAINT ck_historico_trabalho_alvo CHECK (num_nonnulls(processo_id, tarefa_id) = 1)
);

CREATE INDEX ix_historico_trabalho_empresa_processo ON historico_trabalho (empresa_id, processo_id);
CREATE INDEX ix_historico_trabalho_empresa_tarefa ON historico_trabalho (empresa_id, tarefa_id);
