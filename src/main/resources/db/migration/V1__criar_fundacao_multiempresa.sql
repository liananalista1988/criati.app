CREATE TABLE empresa (
    id UUID NOT NULL,
    razao_social VARCHAR(150) NOT NULL,
    nome_fantasia VARCHAR(150) NOT NULL,
    cnpj VARCHAR(14),
    email VARCHAR(150),
    telefone VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_empresa PRIMARY KEY (id),
    CONSTRAINT uk_empresa_cnpj UNIQUE (cnpj)
);

CREATE TABLE usuario (
    id UUID NOT NULL,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE usuario_empresa (
    id UUID NOT NULL,
    usuario_id UUID NOT NULL,
    empresa_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_entrada TIMESTAMP WITH TIME ZONE NOT NULL,
    data_saida TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_usuario_empresa PRIMARY KEY (id),
    CONSTRAINT uk_usuario_empresa_usuario_empresa UNIQUE (usuario_id, empresa_id),
    CONSTRAINT uk_usuario_empresa_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT fk_usuario_empresa_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_usuario_empresa_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_usuario_empresa_usuario ON usuario_empresa (usuario_id);
CREATE INDEX idx_usuario_empresa_empresa ON usuario_empresa (empresa_id);

CREATE TABLE perfil (
    id UUID NOT NULL,
    empresa_id UUID NOT NULL,
    codigo VARCHAR(50) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_perfil PRIMARY KEY (id),
    CONSTRAINT uk_perfil_empresa_codigo UNIQUE (empresa_id, codigo),
    CONSTRAINT uk_perfil_id_empresa UNIQUE (id, empresa_id),
    CONSTRAINT fk_perfil_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

CREATE INDEX idx_perfil_empresa ON perfil (empresa_id);

CREATE TABLE permissao (
    id UUID NOT NULL,
    codigo VARCHAR(100) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_permissao PRIMARY KEY (id),
    CONSTRAINT uk_permissao_codigo UNIQUE (codigo)
);

CREATE TABLE usuario_empresa_perfil (
    usuario_empresa_id UUID NOT NULL,
    perfil_id UUID NOT NULL,
    empresa_id UUID NOT NULL,
    CONSTRAINT pk_usuario_empresa_perfil PRIMARY KEY (usuario_empresa_id, perfil_id),
    CONSTRAINT fk_usuario_empresa_perfil_vinculo FOREIGN KEY (usuario_empresa_id, empresa_id)
        REFERENCES usuario_empresa (id, empresa_id),
    CONSTRAINT fk_usuario_empresa_perfil_perfil FOREIGN KEY (perfil_id, empresa_id)
        REFERENCES perfil (id, empresa_id)
);

CREATE INDEX idx_usuario_empresa_perfil_vinculo ON usuario_empresa_perfil (usuario_empresa_id);
CREATE INDEX idx_usuario_empresa_perfil_perfil ON usuario_empresa_perfil (perfil_id);
CREATE INDEX idx_usuario_empresa_perfil_empresa ON usuario_empresa_perfil (empresa_id);

CREATE TABLE perfil_permissao (
    perfil_id UUID NOT NULL,
    permissao_id UUID NOT NULL,
    CONSTRAINT pk_perfil_permissao PRIMARY KEY (perfil_id, permissao_id),
    CONSTRAINT fk_perfil_permissao_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id),
    CONSTRAINT fk_perfil_permissao_permissao FOREIGN KEY (permissao_id) REFERENCES permissao (id)
);

CREATE INDEX idx_perfil_permissao_perfil ON perfil_permissao (perfil_id);
CREATE INDEX idx_perfil_permissao_permissao ON perfil_permissao (permissao_id);
