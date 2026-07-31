package br.app.criati.web;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ArquivoImportacaoDuplicadoException;
import br.app.criati.exception.AplicacaoInativaException;
import br.app.criati.exception.AplicacaoNaoEncontradaException;
import br.app.criati.exception.AutoAlteracaoNaoPermitidaException;
import br.app.criati.exception.AutoRedefinicaoSenhaNaoPermitidaException;
import br.app.criati.exception.CategoriaFinanceiraComLancamentosException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.CategoriaFinanceiraNomeDuplicadaException;
import br.app.criati.exception.CartaoCreditoNaoEncontradoException;
import br.app.criati.exception.CartaoCreditoStatusInvalidoException;
import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.exception.CompromissoFinanceiroNaoEncontradoException;
import br.app.criati.exception.ContaFinanceiraComLancamentosException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.ContaFinanceiraNomeDuplicadoException;
import br.app.criati.exception.ConviteInvalidoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.exception.EmpresaInativaException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.EmpresaStatusInvalidoException;
import br.app.criati.exception.EmprestimoConcedidoNaoEncontradoException;
import br.app.criati.exception.EmprestimoConcedidoStatusInvalidoException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.FaturaCartaoNaoEncontradaException;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.exception.LancamentoFinanceiroNaoEncontradoException;
import br.app.criati.exception.LancamentoStatusInvalidoException;
import br.app.criati.exception.LoteImportacaoNaoEncontradoException;
import br.app.criati.exception.LoteImportacaoStatusInvalidoException;
import br.app.criati.exception.InstituicaoFinanceiraNaoEncontradaException;
import br.app.criati.exception.OcorrenciaCompromissoNaoEncontradaException;
import br.app.criati.exception.OcorrenciaCompromissoStatusInvalidoException;
import br.app.criati.exception.PagamentoOcorrenciaNaoEncontradoException;
import br.app.criati.exception.PagamentoOcorrenciaStatusInvalidoException;
import br.app.criati.exception.ParcelaEmprestimoNaoEncontradaException;
import br.app.criati.exception.ParcelaEmprestimoStatusInvalidoException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.RecebimentoParcelaEmprestimoNaoEncontradoException;
import br.app.criati.exception.RecebimentoParcelaEmprestimoStatusInvalidoException;
import br.app.criati.exception.RecorrenciaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RecorrenciaFinanceiraStatusInvalidoException;
import br.app.criati.exception.RessarcimentoParcelaCartaoNaoEncontradoException;
import br.app.criati.exception.RessarcimentoParcelaCartaoStatusInvalidoException;
import br.app.criati.exception.UltimoAdministradorAtivoException;
import br.app.criati.exception.UsuarioJaVinculadoPessoaFinanceiraException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.UsuarioStatusInvalidoException;
import br.app.criati.exception.ValorAReceberParcelaCartaoNaoEncontradoException;
import br.app.criati.exception.ValorAReceberParcelaCartaoStatusInvalidoException;
import br.app.criati.exception.VinculoStatusInvalidoException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> tratarAutenticacaoInvalida(
			AuthenticationException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos", request, Map.of());
	}

	@ExceptionHandler(AcessoNegadoException.class)
	public ResponseEntity<ApiErrorResponse> tratarAcessoNegado(
			AcessoNegadoException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.FORBIDDEN, "Acesso negado", request, Map.of());
	}

	@ExceptionHandler(DadosInvalidosException.class)
	public ResponseEntity<ApiErrorResponse> tratarDadosInvalidos(
			DadosInvalidosException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.BAD_REQUEST, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> tratarValidacao(
			MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return criarResposta(
				HttpStatus.BAD_REQUEST,
				"Dados de entrada invalidos",
				request,
				fieldErrors);
	}

	@ExceptionHandler({
			CnpjJaCadastradoException.class,
			EmailJaCadastradoException.class,
			UsuarioEmpresaJaVinculadoException.class,
			UltimoAdministradorAtivoException.class,
			AutoAlteracaoNaoPermitidaException.class,
			VinculoStatusInvalidoException.class,
			EmpresaInativaException.class,
			EmpresaStatusInvalidoException.class,
			AplicacaoInativaException.class,
			ContaFinanceiraInativaException.class,
			CategoriaFinanceiraInativaException.class,
			ContaFinanceiraComLancamentosException.class,
			CategoriaFinanceiraComLancamentosException.class,
			LancamentoStatusInvalidoException.class,
			FinanceiroStatusInvalidoException.class,
			FaturaCartaoStatusInvalidoException.class,
			ContaFinanceiraNomeDuplicadoException.class,
			CategoriaFinanceiraNomeDuplicadaException.class,
			UsuarioJaVinculadoPessoaFinanceiraException.class,
			PessoaFinanceiraInativaException.class,
			RecorrenciaFinanceiraStatusInvalidoException.class,
			OcorrenciaCompromissoStatusInvalidoException.class,
			PagamentoOcorrenciaStatusInvalidoException.class,
			CartaoCreditoStatusInvalidoException.class,
			EmprestimoConcedidoStatusInvalidoException.class,
			ParcelaEmprestimoStatusInvalidoException.class,
			RecebimentoParcelaEmprestimoStatusInvalidoException.class,
			ValorAReceberParcelaCartaoStatusInvalidoException.class,
			RessarcimentoParcelaCartaoStatusInvalidoException.class,
			AutoRedefinicaoSenhaNaoPermitidaException.class,
			UsuarioStatusInvalidoException.class,
			LoteImportacaoStatusInvalidoException.class,
			ArquivoImportacaoDuplicadoException.class
	})
	public ResponseEntity<ApiErrorResponse> tratarConflito(
			RuntimeException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler({
			UsuarioNaoEncontradoException.class,
			EmpresaNaoEncontradaException.class,
			ConviteInvalidoException.class,
			AplicacaoNaoEncontradaException.class,
			ContaFinanceiraNaoEncontradaException.class,
			CategoriaFinanceiraNaoEncontradaException.class,
			LancamentoFinanceiroNaoEncontradoException.class,
			PessoaFinanceiraNaoEncontradaException.class,
			ParteFinanceiraNaoEncontradaException.class,
			InstituicaoFinanceiraNaoEncontradaException.class,
			RecorrenciaFinanceiraNaoEncontradaException.class,
			CompromissoFinanceiroNaoEncontradoException.class,
			OcorrenciaCompromissoNaoEncontradaException.class,
			PagamentoOcorrenciaNaoEncontradoException.class,
			CartaoCreditoNaoEncontradoException.class,
			EmprestimoConcedidoNaoEncontradoException.class,
			ParcelaEmprestimoNaoEncontradaException.class,
			RecebimentoParcelaEmprestimoNaoEncontradoException.class,
			ValorAReceberParcelaCartaoNaoEncontradoException.class,
			RessarcimentoParcelaCartaoNaoEncontradoException.class,
			FaturaCartaoNaoEncontradaException.class,
			LoteImportacaoNaoEncontradoException.class
	})
	public ResponseEntity<ApiErrorResponse> tratarNaoEncontrado(
			RuntimeException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
	}

	private ResponseEntity<ApiErrorResponse> criarResposta(
			HttpStatus status,
			String message,
			HttpServletRequest request,
			Map<String, String> fieldErrors) {
		ApiErrorResponse response = new ApiErrorResponse(
				OffsetDateTime.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				request.getRequestURI(),
				fieldErrors);
		return ResponseEntity.status(status).body(response);
	}
}
