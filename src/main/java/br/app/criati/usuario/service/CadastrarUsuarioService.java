package br.app.criati.usuario.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class CadastrarUsuarioService {

	private final UsuarioRepository usuarioRepository;

	public CadastrarUsuarioService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	public Usuario executar(String nome, String email, String senhaHash) {
		validarObrigatorio(nome, "Nome e obrigatorio");
		validarObrigatorio(email, "E-mail e obrigatorio");
		validarObrigatorio(senhaHash, "Hash da senha e obrigatorio");

		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
		if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}

		Usuario usuario = new Usuario(nome, emailNormalizado, senhaHash, StatusCadastro.ATIVO);
		return usuarioRepository.save(usuario);
	}

	private void validarObrigatorio(String valor, String mensagem) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(mensagem);
		}
	}
}
