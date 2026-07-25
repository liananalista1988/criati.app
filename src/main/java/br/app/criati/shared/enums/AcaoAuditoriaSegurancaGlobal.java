package br.app.criati.shared.enums;

// Acao da auditoria GLOBAL de seguranca (redefinicao-senha-global-auditoria) -
// deliberadamente separada de AcaoAuditoriaSeguranca (redefinicao_senha_auditoria,
// exclusiva do fluxo empresarial da CRIATI-SEG-001): sao tabelas e contratos
// diferentes, uma nao depende de empresa/vinculo, a outra exige.
public enum AcaoAuditoriaSegurancaGlobal {
	REDEFINICAO_ADMINISTRATIVA_SENHA_GLOBAL
}
