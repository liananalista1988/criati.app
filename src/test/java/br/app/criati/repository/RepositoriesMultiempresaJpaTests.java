package br.app.criati.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@DataJpaTest
class RepositoriesMultiempresaJpaTests {

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void deveBuscarEmpresaPorCnpj() {
		Empresa empresa = persistirEmpresa("Empresa Busca Ltda", "11111111000191");

		Optional<Empresa> resultado = empresaRepository.findByCnpj(empresa.getCnpj());

		assertThat(resultado)
				.isPresent()
				.get()
				.extracting(Empresa::getId)
				.isEqualTo(empresa.getId());
	}

	@Test
	void deveVerificarExistenciaDeEmpresaPorCnpj() {
		Empresa empresa = persistirEmpresa("Empresa Existente Ltda", "22222222000192");

		boolean existe = empresaRepository.existsByCnpj(empresa.getCnpj());

		assertThat(existe).isTrue();
	}

	@Test
	void deveBuscarUsuarioPorEmailIgnorandoMaiusculasEMinusculas() {
		Usuario usuario = persistirUsuario("Usuario Busca", "Usuario.Busca@Criati.Test");

		Optional<Usuario> resultado = usuarioRepository.findByEmailIgnoreCase("USUARIO.BUSCA@CRIATI.TEST");

		assertThat(resultado)
				.isPresent()
				.get()
				.extracting(Usuario::getId)
				.isEqualTo(usuario.getId());
	}

	@Test
	void deveVerificarExistenciaDeUsuarioPorEmailIgnorandoMaiusculasEMinusculas() {
		persistirUsuario("Usuario Existente", "Usuario.Existente@Criati.Test");

		boolean existe = usuarioRepository.existsByEmailIgnoreCase("usuario.existente@criati.test");

		assertThat(existe).isTrue();
	}

	@Test
	void deveBuscarVinculoPorUsuarioIdEEmpresaId() {
		UsuarioEmpresa vinculo = persistirVinculo(
				"Empresa Vinculo Ltda",
				"33333333000193",
				"Usuario Vinculo",
				"usuario.vinculo@criati.test");

		Optional<UsuarioEmpresa> resultado = usuarioEmpresaRepository.findByUsuarioIdAndEmpresaId(
				vinculo.getUsuario().getId(),
				vinculo.getEmpresa().getId());

		assertThat(resultado)
				.isPresent()
				.get()
				.extracting(UsuarioEmpresa::getId)
				.isEqualTo(vinculo.getId());
		assertThat(usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(
				vinculo.getUsuario().getId(),
				vinculo.getEmpresa().getId())).isTrue();
	}

	@Test
	void deveListarVinculosDeUmUsuario() {
		Usuario usuario = persistirUsuario("Usuario Multiempresa", "multiempresa@criati.test");
		Empresa primeiraEmpresa = persistirEmpresa("Primeira Empresa Ltda", "44444444000194");
		Empresa segundaEmpresa = persistirEmpresa("Segunda Empresa Ltda", "55555555000195");
		UsuarioEmpresa primeiroVinculo = persistirVinculo(usuario, primeiraEmpresa);
		UsuarioEmpresa segundoVinculo = persistirVinculo(usuario, segundaEmpresa);
		entityManager.flush();

		List<UsuarioEmpresa> resultados = usuarioEmpresaRepository.findAllByUsuarioId(usuario.getId());

		assertThat(resultados)
				.extracting(UsuarioEmpresa::getId)
				.containsExactlyInAnyOrder(primeiroVinculo.getId(), segundoVinculo.getId());
	}

	@Test
	void deveListarVinculosDeUmaEmpresa() {
		Empresa empresa = persistirEmpresa("Empresa Multiusuario Ltda", "66666666000196");
		Usuario primeiroUsuario = persistirUsuario("Primeiro Usuario", "primeiro@criati.test");
		Usuario segundoUsuario = persistirUsuario("Segundo Usuario", "segundo@criati.test");
		UsuarioEmpresa primeiroVinculo = persistirVinculo(primeiroUsuario, empresa);
		UsuarioEmpresa segundoVinculo = persistirVinculo(segundoUsuario, empresa);
		entityManager.flush();

		List<UsuarioEmpresa> resultados = usuarioEmpresaRepository.findAllByEmpresaId(empresa.getId());

		assertThat(resultados)
				.extracting(UsuarioEmpresa::getId)
				.containsExactlyInAnyOrder(primeiroVinculo.getId(), segundoVinculo.getId());
	}

	private Empresa persistirEmpresa(String nome, String cnpj) {
		Empresa empresa = new Empresa(nome, nome, cnpj, StatusCadastro.ATIVO);
		entityManager.persist(empresa);
		entityManager.flush();
		return empresa;
	}

	private Usuario persistirUsuario(String nome, String email) {
		Usuario usuario = new Usuario(nome, email, "senha-hash-de-teste", StatusCadastro.ATIVO);
		entityManager.persist(usuario);
		entityManager.flush();
		return usuario;
	}

	private UsuarioEmpresa persistirVinculo(
			String nomeEmpresa,
			String cnpj,
			String nomeUsuario,
			String emailUsuario) {
		Empresa empresa = persistirEmpresa(nomeEmpresa, cnpj);
		Usuario usuario = persistirUsuario(nomeUsuario, emailUsuario);
		UsuarioEmpresa vinculo = persistirVinculo(usuario, empresa);
		entityManager.flush();
		return vinculo;
	}

	private UsuarioEmpresa persistirVinculo(Usuario usuario, Empresa empresa) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				usuario,
				empresa,
				PerfilUsuario.USUARIO,
				StatusCadastro.ATIVO);
		entityManager.persist(vinculo);
		return vinculo;
	}
}
