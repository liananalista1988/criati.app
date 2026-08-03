package br.app.criati.pagina.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import br.app.criati.aplicacao.service.ModuloDisponibilidadeService;
import br.app.criati.aplicacao.service.ModuloDisponivelEmpresa;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.tenant.ContextoEmpresaService;
import jakarta.servlet.http.HttpSession;

/** Prepara a navegação compartilhada por todas as páginas autenticadas. */
@Service
public class NavegacaoPaginaService {

	private final ContextoEmpresaService contextoEmpresaService;
	private final ModuloDisponibilidadeService moduloDisponibilidadeService;

	public NavegacaoPaginaService(ContextoEmpresaService contextoEmpresaService,
			ModuloDisponibilidadeService moduloDisponibilidadeService) {
		this.contextoEmpresaService = contextoEmpresaService;
		this.moduloDisponibilidadeService = moduloDisponibilidadeService;
	}

	public void preparar(Model model, HttpSession session, UsuarioPrincipal principal) {
		model.addAttribute("superAdministrador", false);
		model.addAttribute("mostrarVisaoGeral", true);
		prepararSemContexto(model);
		if (principal == null) {
			return;
		}

		model.addAttribute("superAdministrador", principal.getUsuario().isSuperAdministrador());
		model.addAttribute("mostrarVisaoGeral",
				contextoEmpresaService.listarVinculosAtivos(principal.getUsuario().getId()).size() != 1);
		contextoEmpresaService.obterContextoAtual(session, principal.getUsuario().getId())
				.ifPresent(contexto -> prepararComContexto(model, contexto));
	}

	public void prepararComContexto(Model model, ContextoEmpresaAtual contexto) {
		List<ModuloDisponivelEmpresa> modulos = moduloDisponibilidadeService.listarDisponiveis(contexto);
		model.addAttribute("modulosDisponiveis", modulos);
		model.addAttribute("mostrarCatalogoModulos", modulos.size() != 1);
		model.addAttribute("podeAdministrarEmpresa", contexto.perfil() == PerfilUsuario.ADMINISTRADOR);
		model.addAttribute("trabalhoDisponivel", modulos.stream()
				.anyMatch(item -> item.modulo() == CodigoAplicacao.TAREFAS_PROCESSOS));
	}

	private void prepararSemContexto(Model model) {
		model.addAttribute("modulosDisponiveis", List.of());
		model.addAttribute("mostrarCatalogoModulos", false);
		model.addAttribute("podeAdministrarEmpresa", false);
		model.addAttribute("trabalhoDisponivel", false);
	}
}
