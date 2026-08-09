-- CRIATI-IMP-FEAT-004: identificadores bancarios opcionais em conta_financeira,
-- para permitir sugestao automatica de conta a partir de metadados OFX.
-- Representacao textual para preservar zeros a esquerda (agencia/numero/digito
-- sao codigos, nao numeros). Sem coluna de tipo bancario dedicada: instituicao_id
-- + agencia + numero ja sao suficientes para uma correspondencia segura, e o
-- tipo (TipoContaFinanceira) ja existente na propria conta cobre a distincao
-- corrente/poupanca quando necessario.
ALTER TABLE conta_financeira
    ADD COLUMN agencia_bancaria VARCHAR(20),
    ADD COLUMN numero_conta_bancaria VARCHAR(30),
    ADD COLUMN digito_conta_bancaria VARCHAR(5);

CREATE INDEX ix_conta_financeira_empresa_identificacao_bancaria
    ON conta_financeira (empresa_id, instituicao_id, agencia_bancaria, numero_conta_bancaria)
    WHERE instituicao_id IS NOT NULL
        AND agencia_bancaria IS NOT NULL
        AND numero_conta_bancaria IS NOT NULL;

-- Conta financeira passa a ser opcional em lote/transacao enquanto a
-- importacao estiver em revisao. A confirmacao financeira continua exigindo
-- conta resolvida - aplicado em codigo (ConfirmacaoImportacaoBancariaService),
-- nunca em constraint de banco.
ALTER TABLE lote_importacao_bancaria ALTER COLUMN conta_id DROP NOT NULL;
ALTER TABLE transacao_bancaria_importada ALTER COLUMN conta_id DROP NOT NULL;

-- Metadados do extrato OFX (BANKID/BRANCHID/ACCTID/ACCTTYPE) e a conta
-- sugerida por autodeteccao, armazenados uma unica vez por lote - nunca
-- repetidos por transacao, porque o extrato inteiro pertence a uma unica
-- conta bancaria de origem.
ALTER TABLE lote_importacao_bancaria
    ADD COLUMN identificacao_banco_id VARCHAR(20),
    ADD COLUMN identificacao_agencia VARCHAR(20),
    ADD COLUMN identificacao_numero_conta VARCHAR(30),
    ADD COLUMN identificacao_tipo_conta VARCHAR(20),
    ADD COLUMN conta_sugerida_id UUID,
    ADD CONSTRAINT fk_lote_importacao_conta_sugerida
        FOREIGN KEY (conta_sugerida_id) REFERENCES conta_financeira(id);
