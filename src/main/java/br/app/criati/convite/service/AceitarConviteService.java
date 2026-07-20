package br.app.criati.convite.service;

import java.time.OffsetDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.ConviteInvalidoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmailJaCadastradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.validacao.SenhaValidador;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class AceitarConviteService {

	private final ConviteRepository conviteRepository;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final ConviteTokenService conviteTokenService;
	private final PasswordEncoder passwordEncoder;
	private final SenhaValidador senhaValidador;

	public AceitarConviteService(
			ConviteRepository conviteRepository,
			UsuarioRepository usuarioRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			ConviteTokenService conviteTokenService,
			PasswordEncoder passwordEncoder,
			SenhaValidador senhaValidador) {
		this.conviteRepository = conviteRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.conviteTokenService = conviteTokenService;
		this.passwordEncoder = passwordEncoder;
		this.senhaValidador = senhaValidador;
	}

	// Transacao unica, dona da transacao (sem contexto ambiente: chamada
	// direta a partir de um endpoint publico). Qualquer falha desfaz usuario,
	// vinculo e a marcacao do convite como utilizado, atomicamente.
	//
	// Caso B (e-mail ja possui Usuario) fica fora do escopo do MVP por
	// decisao explicita: rejeitar com EmailJaCadastradoException em vez de
	// permitir redefinir senha de conta existente via convite, o que abriria
	// uma brecha de takeover de conta. Ver docs/DECISOES.md.
	@Transactional
	public Usuario aceitar(String tokenBruto, String nome, String senha, String confirmacaoSenha) {
		senhaValidador.validar(senha, confirmacaoSenha);
		if (nome == null || nome.isBlank()) {
			throw new DadosInvalidosException("Nome e obrigatorio");
		}

		Convite convite = buscarConviteValido(tokenBruto);
		Empresa empresa = convite.getEmpresa();
		if (empresa.getStatus() != StatusCadastro.ATIVO) {
			throw new ConviteInvalidoException();
		}

		String email = convite.getEmail();
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new EmailJaCadastradoException();
		}

		String senhaCodificada = passwordEncoder.encode(senha);
		Usuario usuario = new Usuario(nome, email, senhaCodificada, StatusCadastro.ATIVO);
		usuarioRepository.save(usuario);

		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, convite.getPerfil(), StatusCadastro.ATIVO);
		usuarioEmpresaRepository.save(vinculo);

		convite.marcarUtilizado(OffsetDateTime.now());
		conviteRepository.save(convite);

		return usuario;
	}

	private Convite buscarConviteValido(String tokenBruto) {
		if (tokenBruto == null || tokenBruto.isBlank()) {
			throw new ConviteInvalidoException();
		}
		String hash = conviteTokenService.calcularHash(tokenBruto);
		return conviteRepository.findByTokenHash(hash)
				.filter(convite -> convite.estaEfetivamenteValido(OffsetDateTime.now()))
				.orElseThrow(ConviteInvalidoException::new);
	}
}
