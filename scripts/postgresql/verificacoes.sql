-- LES-TECH-001 — verificacoes de diagnostico contra o PostgreSQL de validacao.
--
-- Todo bloco de teste negativo roda dentro de BEGIN...ROLLBACK: nenhuma linha
-- de teste permanece no banco ao final da execucao deste script, mesmo que os
-- INSERTs de "setup" de cada bloco sejam bem-sucedidos. Todos os identificadores
-- de teste usam UUIDs fixos e obviamente ficticios (prefixo 00000000-...-00),
-- nunca dados reais.
--
-- Uso: docker compose exec -T postgres-validacao \
--        psql -U "$POSTGRES_VALIDACAO_USER" -d "$POSTGRES_VALIDACAO_DB" -f -  < scripts/postgresql/verificacoes.sql
--
-- Este arquivo foi escrito com base na leitura direta das migrations V1-V12,
-- mas NAO foi executado contra um PostgreSQL real neste ambiente (Docker
-- indisponivel) — ver docs/empresas/financeiro-les/VALIDACAO-POSTGRESQL.md.

\set ON_ERROR_STOP off
\pset pager off

\echo '=================================================================='
\echo '1) Versao final aplicada pelo Flyway (esperado: 12, success = t)'
\echo '=================================================================='
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;

\echo '=================================================================='
\echo '2) Tabelas do dominio financeiro (esperado: V1 a V12 presentes)'
\echo '=================================================================='
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;

\echo '=================================================================='
\echo '3) Constraints da tabela cartao_credito (autorreferencia, checks)'
\echo '=================================================================='
SELECT conname, pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'cartao_credito'::regclass
ORDER BY conname;

\echo '=================================================================='
\echo '4) Indices da tabela cartao_credito (tenant, titular, instituicao, principal)'
\echo '=================================================================='
SELECT indexname, indexdef FROM pg_indexes
WHERE tablename = 'cartao_credito'
ORDER BY indexname;

\echo '=================================================================='
\echo '5) Constraints da tabela categoria_financeira (autorreferencia categoria_pai)'
\echo '=================================================================='
SELECT conname, pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'categoria_financeira'::regclass
ORDER BY conname;

\echo '=================================================================='
\echo '6) Constraints de unicidade de ocorrencia_compromisso (recorrencia+competencia)'
\echo '=================================================================='
SELECT conname, pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'ocorrencia_compromisso'::regclass
ORDER BY conname;

\echo '=================================================================='
\echo '7) Origens aceitas em lancamento_financeiro (deve incluir CONTA_A_PAGAR)'
\echo '=================================================================='
SELECT conname, pg_get_constraintdef(oid) AS definicao
FROM pg_constraint
WHERE conrelid = 'lancamento_financeiro'::regclass AND conname = 'ck_lancamento_financeiro_origem';

\echo '=================================================================='
\echo '8) TESTE NEGATIVO: cartao fisico com limite saudavel > limite total'
\echo '   Esperado: ERRO por ck_cartao_credito_limite_saudavel_max'
\echo '=================================================================='
BEGIN;
INSERT INTO empresa (id, nome, nome_fantasia, cnpj, status)
VALUES ('00000000-0000-4000-8000-000000000001', 'Empresa Diagnostico LES-TECH-001', 'Empresa Diagnostico',
        '00000000000191', 'ATIVO');
INSERT INTO pessoa_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000001',
        'Pessoa Diagnostico', 'ATIVO', now(), now());
INSERT INTO instituicao_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000003', '00000000-0000-4000-8000-000000000001',
        'Instituicao Diagnostico', 'ATIVO', now(), now());
INSERT INTO cartao_credito (id, empresa_id, pessoa_titular_id, instituicao_id, nome, tipo, bandeira,
        limite_total, limite_saudavel, dia_fechamento, dia_vencimento, status)
VALUES ('00000000-0000-4000-8000-000000000004', '00000000-0000-4000-8000-000000000001',
        '00000000-0000-4000-8000-000000000002', '00000000-0000-4000-8000-000000000003',
        'Cartao Diagnostico Invalido', 'FISICO', 'VISA', 1000.00, 2000.00, 5, 12, 'ATIVO');
ROLLBACK;

\echo '=================================================================='
\echo '9) TESTE NEGATIVO: cartao virtual sem cartao principal'
\echo '   Esperado: ERRO por ck_cartao_credito_virtual_exige_principal'
\echo '=================================================================='
BEGIN;
INSERT INTO empresa (id, nome, nome_fantasia, cnpj, status)
VALUES ('00000000-0000-4000-8000-000000000011', 'Empresa Diagnostico LES-TECH-001', 'Empresa Diagnostico',
        '00000000000192', 'ATIVO');
INSERT INTO pessoa_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000012', '00000000-0000-4000-8000-000000000011',
        'Pessoa Diagnostico', 'ATIVO', now(), now());
INSERT INTO instituicao_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000013', '00000000-0000-4000-8000-000000000011',
        'Instituicao Diagnostico', 'ATIVO', now(), now());
INSERT INTO cartao_credito (id, empresa_id, pessoa_titular_id, instituicao_id, nome, tipo, bandeira, status)
VALUES ('00000000-0000-4000-8000-000000000014', '00000000-0000-4000-8000-000000000011',
        '00000000-0000-4000-8000-000000000012', '00000000-0000-4000-8000-000000000013',
        'Cartao Virtual Invalido', 'VIRTUAL', 'VISA', 'ATIVO');
ROLLBACK;

\echo '=================================================================='
\echo '10) TESTE NEGATIVO: instituicao inexistente (violacao de FK)'
\echo '    Esperado: ERRO por fk_cartao_credito_instituicao'
\echo '=================================================================='
BEGIN;
INSERT INTO empresa (id, nome, nome_fantasia, cnpj, status)
VALUES ('00000000-0000-4000-8000-000000000021', 'Empresa Diagnostico LES-TECH-001', 'Empresa Diagnostico',
        '00000000000193', 'ATIVO');
INSERT INTO pessoa_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000022', '00000000-0000-4000-8000-000000000021',
        'Pessoa Diagnostico', 'ATIVO', now(), now());
INSERT INTO cartao_credito (id, empresa_id, pessoa_titular_id, instituicao_id, nome, tipo, bandeira,
        limite_total, dia_fechamento, dia_vencimento, status)
VALUES ('00000000-0000-4000-8000-000000000024', '00000000-0000-4000-8000-000000000021',
        '00000000-0000-4000-8000-000000000022', '00000000-0000-4000-8000-000000000099',
        'Cartao FK Invalida', 'FISICO', 'VISA', 500.00, 5, 12, 'ATIVO');
ROLLBACK;

\echo '=================================================================='
\echo '11) TESTE NEGATIVO: duplicidade de ocorrencia por recorrencia+competencia'
\echo '    Esperado: ERRO por uq_ocorrencia_compromisso_recorrencia_competencia'
\echo '=================================================================='
BEGIN;
INSERT INTO empresa (id, nome, nome_fantasia, cnpj, status)
VALUES ('00000000-0000-4000-8000-000000000031', 'Empresa Diagnostico LES-TECH-001', 'Empresa Diagnostico',
        '00000000000194', 'ATIVO');
INSERT INTO pessoa_financeira (id, empresa_id, nome, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000032', '00000000-0000-4000-8000-000000000031',
        'Pessoa Diagnostico', 'ATIVO', now(), now());
INSERT INTO categoria_financeira (id, empresa_id, nome, tipo, status)
VALUES ('00000000-0000-4000-8000-000000000033', '00000000-0000-4000-8000-000000000031',
        'Categoria Diagnostico', 'DESPESA', 'ATIVO');
INSERT INTO conta_financeira (id, empresa_id, pessoa_titular_id, nome, tipo, saldo_inicial, moeda,
        data_saldo_inicial, permite_conciliacao, status)
VALUES ('00000000-0000-4000-8000-000000000034', '00000000-0000-4000-8000-000000000031',
        '00000000-0000-4000-8000-000000000032', 'Conta Diagnostico', 'CAIXA', 0, 'BRL',
        current_date, FALSE, 'ATIVO');
INSERT INTO recorrencia_financeira (id, empresa_id, tipo, descricao, valor_padrao, conta_id, categoria_id,
        pessoa_financeira_id, periodicidade, intervalo, dia_referencia, data_inicial, proxima_competencia,
        status, gerar_automaticamente, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000035', '00000000-0000-4000-8000-000000000031', 'DESPESA',
        'Recorrencia Diagnostico', 100.00, '00000000-0000-4000-8000-000000000034',
        '00000000-0000-4000-8000-000000000033', '00000000-0000-4000-8000-000000000032', 'MENSAL', 1, 10,
        '2026-01-01', '2026-01-01', 'ATIVA', FALSE, now(), now());
INSERT INTO ocorrencia_compromisso (id, empresa_id, recorrencia_id, competencia, descricao, categoria_id,
        pessoa_financeira_id, valor_principal, vencimento, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000036', '00000000-0000-4000-8000-000000000031',
        '00000000-0000-4000-8000-000000000035', '2026-01-01', 'Ocorrencia Diagnostico 1',
        '00000000-0000-4000-8000-000000000033', '00000000-0000-4000-8000-000000000032', 100.00,
        '2026-01-10', 'PENDENTE', now(), now());
-- Mesma recorrencia + mesma competencia: deve violar a unicidade.
INSERT INTO ocorrencia_compromisso (id, empresa_id, recorrencia_id, competencia, descricao, categoria_id,
        pessoa_financeira_id, valor_principal, vencimento, status, criado_em, atualizado_em)
VALUES ('00000000-0000-4000-8000-000000000037', '00000000-0000-4000-8000-000000000031',
        '00000000-0000-4000-8000-000000000035', '2026-01-01', 'Ocorrencia Diagnostico Duplicada',
        '00000000-0000-4000-8000-000000000033', '00000000-0000-4000-8000-000000000032', 100.00,
        '2026-01-10', 'PENDENTE', now(), now());
ROLLBACK;

\echo '=================================================================='
\echo 'Fim das verificacoes. Nenhuma linha de teste foi commitada (todos os'
\echo 'blocos usaram ROLLBACK). Revise as mensagens ERROR acima: cada uma'
\echo 'delas era esperada e prova a constraint correspondente.'
\echo '=================================================================='
