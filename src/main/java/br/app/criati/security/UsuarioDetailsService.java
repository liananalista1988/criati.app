package br.app.criati.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String email) {
		return usuarioRepository.findByEmailIgnoreCase(email)
				.map(UsuarioPrincipal::new)
				.orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
	}
}
