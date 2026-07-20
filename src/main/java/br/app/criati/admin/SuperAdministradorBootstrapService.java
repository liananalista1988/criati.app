package br.app.criati.admin;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class SuperAdministradorBootstrapService {

	private static final Logger log = LoggerFactory.getLogger(SuperAdministradorBootstrapService.class);

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;

	public SuperAdministradorBootstrapService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public void executar(String nome, String email, String senha) {
		if (usuarioRepository.existsBySuperAdministradorTrue()) {
			log.info("Bootstrap de Superadministrador ignorado: ja existe um Superadministrador cadastrado.");
			return;
		}

		if (nome == null || nome.isBlank() || email == null || email.isBlank() || senha == null || senha.isBlank()) {
			log.info("Bootstrap de Superadministrador nao configurado ou incompleto: nenhuma acao realizada.");
			return;
		}

		String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
		if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
			log.warn("Bootstrap de Superadministrador ignorado: o e-mail configurado ja esta em uso.");
			return;
		}

		String senhaCodificada = passwordEncoder.encode(senha);
		Usuario superAdministrador = Usuario.criarSuperAdministrador(nome, emailNormalizado, senhaCodificada);
		usuarioRepository.save(superAdministrador);
		log.info("Superadministrador inicial criado com sucesso.");
	}
}
