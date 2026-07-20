package br.app.criati.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.usuario.repository.UsuarioRepository;

// Contexto Spring dedicado (properties diferentes da classe abaixo forcam um
// ApplicationContext novo): confirma que as variaveis de ambiente
// CRIATI_BOOTSTRAP_* chegam de fato ao SuperAdministradorBootstrapService
// atraves de application.properties + @Value, e nao apenas quando o servico
// e chamado diretamente (como nos testes unitarios com Mockito).
@SpringBootTest(properties = {
		"CRIATI_BOOTSTRAP_NOME=Superadministrador Inicial",
		"CRIATI_BOOTSTRAP_EMAIL=superadmin.bootstrap.wiring@criati.test",
		"CRIATI_BOOTSTRAP_PASSWORD=senha-do-bootstrap-inicial"
})
@ActiveProfiles("test")
class SuperAdministradorBootstrapWiringTests {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Test
	void deveCriarSuperAdministradorAPartirDasVariaveisDeAmbienteNaInicializacao() {
		var superAdministrador = usuarioRepository
				.findByEmailIgnoreCase("superadmin.bootstrap.wiring@criati.test")
				.orElseThrow(() -> new AssertionError("Superadministrador nao foi criado pelo bootstrap"));

		assertThat(superAdministrador.isSuperAdministrador()).isTrue();
		assertThat(superAdministrador.getNome()).isEqualTo("Superadministrador Inicial");
	}
}
