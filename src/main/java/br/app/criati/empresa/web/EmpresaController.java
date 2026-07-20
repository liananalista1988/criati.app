package br.app.criati.empresa.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.exception.AcessoNegadoException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

	@PostMapping
	public ResponseEntity<EmpresaResponse> cadastrar(@Valid @RequestBody CadastrarEmpresaRequest request) {
		// Cadastro de empresa e restrito ao Superadministrador da Criati, que ainda
		// nao existe nesta fase (docs/DECISOES.md); nenhum usuario autenticado
		// pode cadastrar empresas ate essa decisao ser implementada.
		throw new AcessoNegadoException();
	}
}
