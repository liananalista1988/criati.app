package br.app.criati.usuario.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.exception.AcessoNegadoException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	@PostMapping
	public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastrarUsuarioRequest request) {
		// O convite de usuarios ainda nao existe (fora do escopo desta fase) e o
		// autocadastro aberto contraria docs/DECISOES.md (usuarios sao convidados,
		// nunca se autocadastram); por isso o endpoint fica bloqueado por enquanto.
		throw new AcessoNegadoException();
	}
}
