package br.app.criati.web;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

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
			UsuarioEmpresaJaVinculadoException.class
	})
	public ResponseEntity<ApiErrorResponse> tratarConflito(
			RuntimeException exception,
			HttpServletRequest request) {
		return criarResposta(HttpStatus.CONFLICT, exception.getMessage(), request, Map.of());
	}

	@ExceptionHandler({
			UsuarioNaoEncontradoException.class,
			EmpresaNaoEncontradaException.class
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
