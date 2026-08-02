CREATE TABLE regra_classificacao_importacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    conta_id UUID,
    descricao_referencia VARCHAR(500) NOT NULL,
    padrao_normalizado VARCHAR(300) NOT NULL,
    estrategia_comparacao VARCHAR(20) NOT NULL DEFAULT 'CONTEM',
    prioridade INTEGER NOT NULL DEFAULT 0,
    tipo VARCHAR(20) NOT NULL,
    categoria_id UUID NOT NULL,
    pessoa_financeira_id UUID,
    forma_pagamento VARCHAR(30),
    encaminhamento_sugerido VARCHAR(30),
    nivel_confianca VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    aplicacao VARCHAR(20) NOT NULL DEFAULT 'SUGESTAO',
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    quantidade_utilizacoes INTEGER NOT NULL DEFAULT 0,
    ultima_utilizacao_em TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por_usuario_id UUID NOT NULL,
    atualizado_por_usuario_id UUID,
    CONSTRAINT fk_regra_classif_empresa FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT fk_regra_classif_conta FOREIGN KEY (conta_id) REFERENCES conta_financeira(id),
    CONSTRAINT fk_regra_classif_categoria FOREIGN KEY (categoria_id) REFERENCES categoria_financeira(id),
    CONSTRAINT fk_regra_classif_pessoa FOREIGN KEY (pessoa_financeira_id) REFERENCES pessoa_financeira(id),
    CONSTRAINT fk_regra_classif_criado_por FOREIGN KEY (criado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_regra_classif_atualizado_por FOREIGN KEY (atualizado_por_usuario_id) REFERENCES usuario(id),
    CONSTRAINT ck_regra_classif_estrategia CHECK (estrategia_comparacao IN ('IGUAL', 'CONTEM', 'PREFIXO')),
    CONSTRAINT ck_regra_classif_tipo CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_regra_classif_encaminhamento CHECK (encaminhamento_sugerido IS NULL OR encaminhamento_sugerido IN
        ('FATURA_CARTAO', 'EMPRESTIMO_RECEBIDO', 'EMPRESTIMO_CONCEDIDO',
         'EMPRESTIMO_RECEBIMENTO_PARCELA', 'EMPRESTIMO_PAGAMENTO_PARCELA')),
    CONSTRAINT ck_regra_classif_confianca CHECK (nivel_confianca IN ('ALTA', 'MEDIA', 'BAIXA')),
    CONSTRAINT ck_regra_classif_aplicacao CHECK (aplicacao IN ('AUTOMATICA', 'SUGESTAO')),
    CONSTRAINT ck_regra_classif_status CHECK (status IN ('ATIVO', 'INATIVO')),
    CONSTRAINT ck_regra_classif_utilizacoes CHECK (quantidade_utilizacoes >= 0),
    CONSTRAINT ck_regra_classif_prioridade CHECK (prioridade >= 0),
    -- Ajuste obrigatorio 2 (CRIATI-IMP-002A): CONTEM nunca pode ser
    -- automatica, decisao de negocio explicita - nao e uma questao de
    -- tamanho de padrao. So IGUAL/PREFIXO podem ser AUTOMATICA.
    CONSTRAINT ck_regra_classif_automatica_estrategia
        CHECK (aplicacao <> 'AUTOMATICA' OR estrategia_comparacao IN ('IGUAL', 'PREFIXO')),
    -- Protecao complementar (nunca a principal - a validacao semantica contra
    -- padroes genericos de verdade fica no service, ver ajuste obrigatorio 3).
    CONSTRAINT ck_regra_classif_automatica_minima
        CHECK (aplicacao <> 'AUTOMATICA' OR length(padrao_normalizado) >= 6)
);

CREATE INDEX ix_regra_classificacao_empresa_status
    ON regra_classificacao_importacao (empresa_id, status);
CREATE INDEX ix_regra_classificacao_empresa_conta
    ON regra_classificacao_importacao (empresa_id, conta_id);

-- Dois indices unicos parciais em vez de um UNIQUE comum: UNIQUE normal nao
-- bloqueia duplicidade quando conta_id e NULL (NULL <> NULL no Postgres).
CREATE UNIQUE INDEX uq_regra_classif_com_conta
    ON regra_classificacao_importacao (empresa_id, conta_id, padrao_normalizado, tipo)
    WHERE conta_id IS NOT NULL;
CREATE UNIQUE INDEX uq_regra_classif_sem_conta
    ON regra_classificacao_importacao (empresa_id, padrao_normalizado, tipo)
    WHERE conta_id IS NULL;

ALTER TABLE transacao_bancaria_importada
    ADD COLUMN regra_classificacao_id UUID,
    ADD COLUMN origem_classificacao VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

ALTER TABLE transacao_bancaria_importada
    ADD CONSTRAINT fk_transacao_importada_regra
        FOREIGN KEY (regra_classificacao_id) REFERENCES regra_classificacao_importacao(id),
    ADD CONSTRAINT ck_transacao_importada_origem_classificacao
        CHECK (origem_classificacao IN ('MANUAL', 'REGRA_SUGERIDA', 'REGRA_AUTOMATICA')),
    ADD CONSTRAINT ck_transacao_importada_origem_regra_coerente
        CHECK (regra_classificacao_id IS NOT NULL OR origem_classificacao = 'MANUAL');

CREATE INDEX ix_transacao_importada_regra
    ON transacao_bancaria_importada (regra_classificacao_id)
    WHERE regra_classificacao_id IS NOT NULL;
