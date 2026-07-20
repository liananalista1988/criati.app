package br.app.criati.aplicacao.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.aplicacao.service.AplicacaoService;

@RestController
@RequestMapping("/api/admin/aplicacoes")
public class AdminAplicacaoController {

	private final AplicacaoService aplicacaoService;

	public AdminAplicacaoController(AplicacaoService aplicacaoService) {
		this.aplicacaoService = aplicacaoService;
	}

	@GetMapping
	public ResponseEntity<List<AplicacaoResponse>> listar() {
		List<AplicacaoResponse> resposta = aplicacaoService.listarCatalogo().stream()
				.map(AplicacaoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}
}
