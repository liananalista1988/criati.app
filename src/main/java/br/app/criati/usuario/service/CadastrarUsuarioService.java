package br.app.criati.usuario.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
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
	private final PasswordEncoder passwordEncoder;

	public CadastrarUsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public Usuario executar(String nome, String email, String senha) {
		validarObrigatorio(nome, "Nome e obrigatorio");
		validarObrigatorio(email, "E-mail e obrigatorio");
		validarObrigatorio(senha, "Senha e obrigatoria");

		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
		if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
			throw new EmailJaCadastradoException();
		}

		String senhaCodificada = passwordEncoder.encode(senha);
		Usuario usuario = new Usuario(nome, emailNormalizado, senhaCodificada, StatusCadastro.ATIVO);
		return usuarioRepository.save(usuario);
	}

	private void validarObrigatorio(String valor, String mensagem) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(mensagem);
		}
	}
}
