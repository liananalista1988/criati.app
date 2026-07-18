package br.app.criati;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.app.criati.empresa.Empresa;
import br.app.criati.empresa.EmpresaRepository;
import br.app.criati.empresa.StatusEmpresa;
import br.app.criati.perfil.Perfil;
import br.app.criati.perfil.PerfilRepository;
import br.app.criati.perfil.UsuarioEmpresaPerfil;
import br.app.criati.usuario.StatusUsuarioEmpresa;
import br.app.criati.usuario.Usuario;
import br.app.criati.usuario.UsuarioEmpresa;
import br.app.criati.usuario.UsuarioEmpresaRepository;
import br.app.criati.usuario.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FundacaoMultiempresaIntegrationTests {

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private PerfilRepository perfilRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void executaMigrationInicial() {
		Integer migrations = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success = true",
				Integer.class);

		assertThat(migrations).isEqualTo(1);
	}

	@Test
	void persisteEmpresaComUuid() {
		Empresa empresa = empresaRepository.saveAndFlush(novaEmpresa("12345678000190", "Criati"));

		assertThat(empresaRepository.findByUuid(empresa.getUuid())).contains(empresa);
		assertThat(empresaRepository.findByCnpj("12345678000190")).contains(empresa);
	}

	@Test
	void persisteUsuarioGlobal() {
		Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario("global@criati.app"));

		assertThat(usuarioRepository.findByUuid(usuario.getUuid())).contains(usuario);
		assertThat(usuarioRepository.findByEmailIgnoreCase("GLOBAL@CRIATI.APP")).contains(usuario);
		assertThat(usuarioRepository.existsByEmailIgnoreCase("global@criati.app")).isTrue();
	}

	@Test
	void vinculaMesmoUsuarioAMaisDeUmaEmpresa() {
		Usuario usuario = usuarioRepository.save(novoUsuario("multi@criati.app"));
		Empresa empresaA = empresaRepository.save(novaEmpresa("11111111000191", "Empresa A"));
		Empresa empresaB = empresaRepository.save(novaEmpresa("22222222000191", "Empresa B"));

		usuarioEmpresaRepository.save(new UsuarioEmpresa(usuario, empresaA, StatusUsuarioEmpresa.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresaB, StatusUsuarioEmpresa.ATIVO));

		assertThat(usuarioEmpresaRepository.findAllByUsuarioUuidAndStatus(
				usuario.getUuid(), StatusUsuarioEmpresa.ATIVO)).hasSize(2);
		assertThat(usuarioEmpresaRepository.existsByUsuarioUuidAndEmpresaUuidAndStatus(
				usuario.getUuid(), empresaA.getUuid(), StatusUsuarioEmpresa.ATIVO)).isTrue();
	}

	@Test
	void rejeitaVinculoDuplicado() {
		Usuario usuario = usuarioRepository.save(novoUsuario("duplicado@criati.app"));
		Empresa empresa = empresaRepository.save(novaEmpresa("33333333000191", "Empresa Única"));
		usuarioEmpresaRepository.saveAndFlush(new UsuarioEmpresa(usuario, empresa, StatusUsuarioEmpresa.ATIVO));

		assertThatThrownBy(() -> usuarioEmpresaRepository
				.saveAndFlush(new UsuarioEmpresa(usuario, empresa, StatusUsuarioEmpresa.ATIVO)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void persistePerfilPertencenteAEmpresaEPermiteMesmoNomeEmOutra() {
		Empresa empresaA = empresaRepository.save(novaEmpresa("44444444000191", "Empresa A"));
		Empresa empresaB = empresaRepository.save(novaEmpresa("55555555000191", "Empresa B"));
		Perfil perfilA = perfilRepository.save(new Perfil(empresaA, "GESTOR", "Gestor", null));
		Perfil perfilB = perfilRepository.saveAndFlush(new Perfil(empresaB, "GESTOR", "Gestor", null));

		assertThat(perfilRepository.findByUuidAndEmpresaUuid(perfilA.getUuid(), empresaA.getUuid()))
				.contains(perfilA);
		assertThat(perfilRepository.findByUuidAndEmpresaUuid(perfilA.getUuid(), empresaB.getUuid()))
				.isEmpty();
		assertThat(perfilRepository.findAllByEmpresaUuid(empresaA.getUuid())).containsExactly(perfilA);
		assertThat(perfilB.getNome()).isEqualTo(perfilA.getNome());
	}

	@Test
	void rejeitaPerfilDeOutraEmpresaNoVinculo() {
		Usuario usuario = usuarioRepository.save(novoUsuario("perfil@criati.app"));
		Empresa empresaA = empresaRepository.save(novaEmpresa("66666666000191", "Empresa A"));
		Empresa empresaB = empresaRepository.save(novaEmpresa("77777777000191", "Empresa B"));
		UsuarioEmpresa vinculo = usuarioEmpresaRepository
				.save(new UsuarioEmpresa(usuario, empresaA, StatusUsuarioEmpresa.ATIVO));
		Perfil perfilDaOutraEmpresa = perfilRepository
				.save(new Perfil(empresaB, "GESTOR", "Gestor", null));

		assertThatIllegalArgumentException()
				.isThrownBy(() -> new UsuarioEmpresaPerfil(vinculo, perfilDaOutraEmpresa))
				.withMessage("O perfil deve pertencer à empresa do vínculo");
	}

	@Test
	void naoExpoeSenhaHash() throws NoSuchFieldException {
		String hash = "$argon2id$hash-que-nao-pode-vazar";
		Usuario usuario = novoUsuario("seguro@criati.app", hash);

		assertThat(Usuario.class.getDeclaredField("senhaHash").isAnnotationPresent(JsonIgnore.class)).isTrue();
		assertThat(Usuario.class.getMethods())
				.noneMatch(metodo -> metodo.getName().equals("getSenhaHash"));
		assertThat(usuario.toString()).doesNotContain("senha", hash);
		assertThat(usuario.hashCode()).isEqualTo(usuario.getUuid().hashCode());
	}

	private Empresa novaEmpresa(String cnpj, String nomeFantasia) {
		return new Empresa("Razão Social " + nomeFantasia, nomeFantasia, cnpj, StatusEmpresa.ATIVA);
	}

	private Usuario novoUsuario(String email) {
		return novoUsuario(email, "$argon2id$hash-de-teste");
	}

	private Usuario novoUsuario(String email, String hash) {
		return new Usuario("Usuário " + UUID.randomUUID(), email, hash, true);
	}
}
