package br.app.criati.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.exception.EmpresaStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminEmpresaOnboardingServiceTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private AdminEmpresaService adminEmpresaService;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private EmpresaAplicacaoRepository empresaAplicacaoRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarEmpresaComAdministradorExistenteEHabilitarAplicacoes() {
		Usuario existente = usuarioRepository.saveAndFlush(
				new Usuario("Admin Existente", "admin.existente@criati.test",
						passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));

		UsuarioEmpresa vinculo = adminEmpresaService.criarComAdministradorExistente(
				"Empresa Onboarding Existente", null, "10111111000101",
				List.of("FINANCEIRO"), existente.getId());

		assertThat(vinculo.getPerfil()).isEqualTo(PerfilUsuario.ADMINISTRADOR);
		assertThat(vinculo.getStatus()).isEqualTo(StatusCadastro.ATIVO);
		assertThat(vinculo.getUsuario().getId()).isEqualTo(existente.getId());
		assertThat(empresaAplicacaoRepository.findAllByEmpresaId(vinculo.getEmpresa().getId()))
				.anySatisfy(vinculoApp -> assertThat(vinculoApp.getAplicacao().getCodigo()).isEqualTo("FINANCEIRO"));
	}

	@Test
	void naoDeveCriarComAdministradorExistenteInexistente() {
		assertThatThrownBy(() -> adminEmpresaService.criarComAdministradorExistente(
				"Empresa Sem Admin", null, "10222222000102", List.of(), UUID.randomUUID()))
				.isInstanceOf(UsuarioNaoEncontradoException.class);
		assertThat(empresaRepository.existsByCnpj("10222222000102")).isFalse();
	}

	@Test
	void naoDeveDeixarEmpresaOrfaQuandoCnpjDuplicadoNoOnboardingComExistente() {
		Usuario existente = usuarioRepository.saveAndFlush(
				new Usuario("Admin Dup", "admin.dup@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		empresaRepository.saveAndFlush(new Empresa("Ja Existe", null, "10333333000103", StatusCadastro.ATIVO));

		assertThatThrownBy(() -> adminEmpresaService.criarComAdministradorExistente(
				"Nova Empresa", null, "10333333000103", List.of(), existente.getId()))
				.isInstanceOf(CnpjJaCadastradoException.class);
	}

	@Test
	void deveCriarEmpresaComConviteDeAdministradorSemDefinirSenha() {
		AdminEmpresaService.EmpresaComConviteAdministrador resultado = adminEmpresaService.criarComAdministradorConvidado(
				"Empresa Onboarding Convite", null, "10444444000104",
				List.of("FINANCEIRO", "CLINICA"), "Novo Administrador", "novo.admin@criati.test",
				bootstrapSuperAdminId());

		assertThat(resultado.empresa().getCnpj()).isEqualTo("10444444000104");
		assertThat(resultado.conviteCriado().convite().getPerfil()).isEqualTo(PerfilUsuario.ADMINISTRADOR);
		assertThat(resultado.conviteCriado().convite().getEmail()).isEqualTo("novo.admin@criati.test");
		assertThat(resultado.conviteCriado().tokenBruto()).isNotBlank();
		assertThat(usuarioRepository.existsByEmailIgnoreCase("novo.admin@criati.test")).isFalse();
	}

	@Test
	void deveAtivarEInativarEmpresa() {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Status", null, "10555555000105", StatusCadastro.ATIVO));

		Empresa inativada = adminEmpresaService.inativar(empresa.getId());
		assertThat(inativada.getStatus()).isEqualTo(StatusCadastro.INATIVO);

		Empresa ativada = adminEmpresaService.ativar(empresa.getId());
		assertThat(ativada.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void naoDeveAtivarEmpresaJaAtiva() {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Ja Ativa", null, "10666666000106", StatusCadastro.ATIVO));
		assertThatThrownBy(() -> adminEmpresaService.ativar(empresa.getId()))
				.isInstanceOf(EmpresaStatusInvalidoException.class);
	}

	@Test
	void deveCalcularSituacaoOperacionalProntaEPendente() {
		Empresa empresaPendente = empresaRepository.saveAndFlush(
				new Empresa("Empresa Pendente", null, "10777777000107", StatusCadastro.ATIVO));
		AdminEmpresaService.EmpresaDetalheDados pendente = adminEmpresaService.buscarDetalhe(empresaPendente.getId());
		assertThat(pendente.pronta()).isFalse();

		Usuario admin = usuarioRepository.saveAndFlush(
				new Usuario("Admin Pronta", "admin.pronta@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(admin, empresaPendente, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));

		AdminEmpresaService.EmpresaDetalheDados aindaSemApp = adminEmpresaService.buscarDetalhe(empresaPendente.getId());
		assertThat(aindaSemApp.pronta()).isFalse();
	}

	private UUID bootstrapSuperAdminId() {
		Usuario superAdmin = Usuario.criarSuperAdministrador(
				"Superadmin Onboarding", "superadmin.onboarding.service@criati.test", passwordEncoder.encode(SENHA));
		return usuarioRepository.saveAndFlush(superAdmin).getId();
	}
}
