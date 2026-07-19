package br.app.criati.empresa.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.service.CadastrarEmpresaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

	private final CadastrarEmpresaService cadastrarEmpresaService;

	public EmpresaController(CadastrarEmpresaService cadastrarEmpresaService) {
		this.cadastrarEmpresaService = cadastrarEmpresaService;
	}

	@PostMapping
	public ResponseEntity<EmpresaResponse> cadastrar(@Valid @RequestBody CadastrarEmpresaRequest request) {
		Empresa empresa = cadastrarEmpresaService.executar(
				request.nome(), request.nomeFantasia(), request.cnpj());
		EmpresaResponse response = new EmpresaResponse(
				empresa.getId(),
				empresa.getNome(),
				empresa.getNomeFantasia(),
				empresa.getCnpj(),
				empresa.getStatus());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
}
