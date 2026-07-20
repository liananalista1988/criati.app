package br.app.criati.financeiro.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

/**
 * Portao unico de acesso ao modulo Financeiro: usuario autenticado, empresa
 * ativa, vinculo empresarial ativo (tudo isso ja garantido por
 * ContextoEmpresaService.exigirContextoAtivo) e, adicionalmente, a aplicacao
 * FINANCEIRO habilitada para a empresa ativa. Usado por todos os controllers
 * REST do modulo, para nao duplicar essa checagem em cada um. Um
 * Superadministrador sem vinculo empresarial nunca chega a validar
 * FINANCEIRO: ja falha antes, em exigirContextoAtivo (nenhum vinculo).
 */
@Service
public class ContextoFinanceiroService {

	private final ContextoEmpresaService contextoEmpresaService;
	private final AplicacaoService aplicacaoService;

	public ContextoFinanceiroService(
			ContextoEmpresaService contextoEmpresaService, AplicacaoService aplicacaoService) {
		this.contextoEmpresaService = contextoEmpresaService;
		this.aplicacaoService = aplicacaoService;
	}

	public ContextoEmpresaAtual exigirAcesso(HttpSession session, UUID usuarioId) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(session, usuarioId);
		if (!aplicacaoService.possuiAplicacaoAtiva(contexto.empresaId(), CodigoAplicacao.FINANCEIRO.name())) {
			throw new AcessoNegadoException();
		}
		return contexto;
	}
}
