ALTER TABLE lote_importacao_bancaria DROP CONSTRAINT ck_lote_importacao_formato;
ALTER TABLE lote_importacao_bancaria ADD CONSTRAINT ck_lote_importacao_formato
    CHECK (formato IN ('OFX', 'CSV'));
