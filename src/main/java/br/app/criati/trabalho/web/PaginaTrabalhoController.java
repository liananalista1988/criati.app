package br.app.criati.trabalho.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import br.app.criati.aplicacao.service.ModuloDisponibilidadeService;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.pagina.service.NavegacaoPaginaService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

/**
 * CRIATI-WRK-002: paginas do modulo Trabalho (processos/tarefas), em
 * controller proprio - nunca em PaginaController (controlador global de
 * entrada, reservado a frente paralela do Codex, fora de escopo desta
 * tarefa). Toda pagina exige contexto empresarial valido e o modulo
 * TAREFAS_PROCESSOS habilitado para a empresa. Listagem e detalhe sao abertos a
 * qualquer perfil (inclusive o responsavel de uma tarefa, que precisa
 * acessar o detalhe para atualizar o proprio andamento); novo/editar exigem
 * ADMINISTRADOR, unico perfil com acesso a GET /api/contexto/usuarios (usado
 * pelo seletor de responsavel do formulario) e unico autorizado a
 * criar/editar pelo backend (ver ProcessoEmpresarialService/TarefaEmpresarialService).
 */
@Controller
public class PaginaTrabalhoController {

	private final ContextoEmpresaService contextoEmpresaService;
	private final ModuloDisponibilidadeService moduloDisponibilidadeService;
	private final NavegacaoPaginaService navegacaoPaginaService;

	public PaginaTrabalhoController(ContextoEmpresaService contextoEmpresaService,
			ModuloDisponibilidadeService moduloDisponibilidadeService,
			NavegacaoPaginaService navegacaoPaginaService) {
		this.contextoEmpresaService = contextoEmpresaService;
		this.moduloDisponibilidadeService = moduloDisponibilidadeService;
		this.navegacaoPaginaService = navegacaoPaginaService;
	}

	@GetMapping("/app/trabalho/processos")
	public String processos(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		ContextoEmpresaAtual contexto = exigirContexto(session, principal);
		model.addAttribute("podeEscreverTrabalho", contexto.perfil() == PerfilUsuario.ADMINISTRADOR);
		return "app/trabalho-processos";
	}

	@GetMapping("/app/trabalho/processos/novo")
	public String processoNovo(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		exigirAdministrador(session, principal);
		return "app/trabalho-processo-form";
	}

	@GetMapping("/app/trabalho/processos/{id}/editar")
	public String processoEditar(@PathVariable String id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		exigirAdministrador(session, principal);
		model.addAttribute("processoId", id);
		return "app/trabalho-processo-form";
	}

	@GetMapping("/app/trabalho/processos/{id}")
	public String processoDetalhe(@PathVariable String id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		ContextoEmpresaAtual contexto = exigirContexto(session, principal);
		model.addAttribute("processoId", id);
		model.addAttribute("podeEscreverTrabalho", contexto.perfil() == PerfilUsuario.ADMINISTRADOR);
		return "app/trabalho-processo-detalhe";
	}

	@GetMapping("/app/trabalho/tarefas")
	public String tarefas(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		ContextoEmpresaAtual contexto = exigirContexto(session, principal);
		model.addAttribute("podeEscreverTrabalho", contexto.perfil() == PerfilUsuario.ADMINISTRADOR);
		model.addAttribute("usuarioEmpresaAtualId", contexto.usuarioEmpresaId());
		return "app/trabalho-tarefas";
	}

	@GetMapping("/app/trabalho/tarefas/nova")
	public String tarefaNova(HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		exigirAdministrador(session, principal);
		return "app/trabalho-tarefa-form";
	}

	@GetMapping("/app/trabalho/tarefas/{id}/editar")
	public String tarefaEditar(@PathVariable String id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		exigirAdministrador(session, principal);
		model.addAttribute("tarefaId", id);
		return "app/trabalho-tarefa-form";
	}

	@GetMapping("/app/trabalho/tarefas/{id}")
	public String tarefaDetalhe(@PathVariable String id, HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal, Model model) {
		ContextoEmpresaAtual contexto = exigirContexto(session, principal);
		model.addAttribute("tarefaId", id);
		model.addAttribute("podeEscreverTrabalho", contexto.perfil() == PerfilUsuario.ADMINISTRADOR);
		model.addAttribute("usuarioEmpresaAtualId", contexto.usuarioEmpresaId());
		return "app/trabalho-tarefa-detalhe";
	}

	private ContextoEmpresaAtual exigirContexto(HttpSession session, UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoEmpresaService.exigirContextoAtivo(
				session, principal.getUsuario().getId());
		moduloDisponibilidadeService.exigirVisualizacao(CodigoAplicacao.TAREFAS_PROCESSOS, contexto);
		return contexto;
	}

	@ModelAttribute
	public void prepararNavegacao(
			Model model, HttpSession session, @AuthenticationPrincipal UsuarioPrincipal principal) {
		navegacaoPaginaService.preparar(model, session, principal);
	}

	private void exigirAdministrador(HttpSession session, UsuarioPrincipal principal) {
		if (exigirContexto(session, principal).perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
