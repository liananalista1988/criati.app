package br.app.criati.financeiro.web;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.service.ContextoFinanceiroService;
import br.app.criati.financeiro.service.DashboardFinanceiro;
import br.app.criati.financeiro.service.DashboardFinanceiroService;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.tenant.ContextoEmpresaAtual;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/contexto/financeiro")
public class DashboardFinanceiroController {

	private static final DateTimeFormatter FORMATO_COMPETENCIA = DateTimeFormatter.ofPattern("yyyy-MM");

	private final DashboardFinanceiroService dashboardFinanceiroService;
	private final ContextoFinanceiroService contextoFinanceiroService;

	public DashboardFinanceiroController(
			DashboardFinanceiroService dashboardFinanceiroService, ContextoFinanceiroService contextoFinanceiroService) {
		this.dashboardFinanceiroService = dashboardFinanceiroService;
		this.contextoFinanceiroService = contextoFinanceiroService;
	}

	@GetMapping("/dashboard")
	public ResponseEntity<DashboardFinanceiroResponse> dashboard(
			@RequestParam(required = false) String competencia,
			HttpSession session,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		ContextoEmpresaAtual contexto = contextoFinanceiroService.exigirAcesso(session, principal.getUsuario().getId());
		YearMonth competenciaParseada = parsearCompetencia(competencia);
		DashboardFinanceiro dashboard = dashboardFinanceiroService.gerar(contexto, competenciaParseada);
		return ResponseEntity.ok(DashboardFinanceiroResponse.from(dashboard));
	}

	private YearMonth parsearCompetencia(String competencia) {
		if (competencia == null || competencia.isBlank()) {
			return null;
		}
		try {
			return YearMonth.parse(competencia, FORMATO_COMPETENCIA);
		} catch (DateTimeParseException exception) {
			throw new DadosInvalidosException("Competencia deve estar no formato AAAA-MM");
		}
	}
}
