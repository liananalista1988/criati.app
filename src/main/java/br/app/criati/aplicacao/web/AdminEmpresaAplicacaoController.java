package br.app.criati.aplicacao.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.aplicacao.service.AplicacaoService;

@RestController
@RequestMapping("/api/admin/empresas/{empresaId}/aplicacoes")
public class AdminEmpresaAplicacaoController {

	private final AplicacaoService aplicacaoService;

	public AdminEmpresaAplicacaoController(AplicacaoService aplicacaoService) {
		this.aplicacaoService = aplicacaoService;
	}

	@GetMapping
	public ResponseEntity<List<AplicacaoSituacaoResponse>> listar(@PathVariable UUID empresaId) {
		List<AplicacaoSituacaoResponse> resposta = aplicacaoService.listarParaEmpresa(empresaId).stream()
				.map(AplicacaoSituacaoResponse::from)
				.toList();
		return ResponseEntity.ok(resposta);
	}

	@PostMapping("/{codigo}/habilitar")
	public ResponseEntity<Void> habilitar(@PathVariable UUID empresaId, @PathVariable String codigo) {
		aplicacaoService.habilitar(empresaId, codigo);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{codigo}/desabilitar")
	public ResponseEntity<Void> desabilitar(@PathVariable UUID empresaId, @PathVariable String codigo) {
		aplicacaoService.desabilitar(empresaId, codigo);
		return ResponseEntity.ok().build();
	}
}
