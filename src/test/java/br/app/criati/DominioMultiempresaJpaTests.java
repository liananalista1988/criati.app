package br.app.criati;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
class DominioMultiempresaJpaTests {

	@PersistenceContext
	private EntityManager entityManager;

	@Test
	void devePersistirEmpresaComUuidGerado() {
		Empresa empresa = new Empresa(
				"Criati Tecnologia Ltda",
				"Criati",
				"12345678000190",
				StatusCadastro.ATIVO);

		entityManager.persist(empresa);
		entityManager.flush();

		assertThat(empresa.getId()).isNotNull();
		assertThat(empresa.getCriadoEm()).isNotNull();
		assertThat(empresa.getAtualizadoEm()).isNotNull();
	}

	@Test
	void devePersistirUsuarioComUuidGerado() {
		Usuario usuario = new Usuario(
				"Usuario de Teste",
				"usuario@criati.test",
				"senha-hash-de-teste",
				StatusCadastro.ATIVO);

		entityManager.persist(usuario);
		entityManager.flush();

		assertThat(usuario.getId()).isNotNull();
		assertThat(usuario.getCriadoEm()).isNotNull();
		assertThat(usuario.getAtualizadoEm()).isNotNull();
	}

	@Test
	void devePersistirUsuarioEmpresaComUuidGerado() {
		Empresa empresa = new Empresa(
				"Empresa Vinculada Ltda",
				"Empresa Vinculada",
				"98765432000110",
				StatusCadastro.ATIVO);
		Usuario usuario = new Usuario(
				"Administrador de Teste",
				"administrador@criati.test",
				"senha-hash-de-teste",
				StatusCadastro.ATIVO);

		entityManager.persist(empresa);
		entityManager.persist(usuario);

		UsuarioEmpresa usuarioEmpresa = new UsuarioEmpresa(
				usuario,
				empresa,
				PerfilUsuario.ADMINISTRADOR,
				StatusCadastro.ATIVO);
		entityManager.persist(usuarioEmpresa);
		entityManager.flush();

		assertThat(usuarioEmpresa.getId()).isNotNull();
		assertThat(usuarioEmpresa.getCriadoEm()).isNotNull();
		assertThat(usuarioEmpresa.getAtualizadoEm()).isNotNull();
	}
}
