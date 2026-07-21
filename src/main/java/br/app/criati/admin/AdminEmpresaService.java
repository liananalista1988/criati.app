package br.app.criati.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.convite.service.ConviteCriado;
import br.app.criati.convite.service.ConviteService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.empresa.service.CadastrarEmpresaService;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaJaVinculadoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;
import br.app.criati.usuario.service.CadastrarUsuarioService;

@Service
public class AdminEmpresaService {

	private final CadastrarEmpresaService cadastrarEmpresaService;
	private final CadastrarUsuarioService cadastrarUsuarioService;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final AplicacaoService aplicacaoService;
	private final ConviteService conviteService;

	public AdminEmpresaService(
			CadastrarEmpresaService cadastrarEmpresaService,
			CadastrarUsuarioService cadastrarUsuarioService,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository,
			AplicacaoService aplicacaoService,
			ConviteService conviteService) {
		this.cadastrarEmpresaService = cadastrarEmpresaService;
		this.cadastrarUsuarioService = cadastrarUsuarioService;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.aplicacaoService = aplicacaoService;
		this.conviteService = conviteService;
	}

	// Transacional: se a empresa OU o usuario administrador falharem
	// (CNPJ/e-mail duplicado, dados invalidos), nada e persistido. O perfil e
	// sempre fixado como ADMINISTRADOR no backend, nunca aceito do cliente.
	@Transactional
	public UsuarioEmpresa criarComAdministrador(
			String nomeEmpresa,
			String nomeFantasia,
			String cnpj,
			String nomeAdministrador,
			String email,
			String senha) {
		Empresa empresa = cadastrarEmpresaService.executar(nomeEmpresa, nomeFantasia, cnpj);
		Usuario administrador = cadastrarUsuarioService.executar(nomeAdministrador, email, senha);

		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				administrador,
				empresa,
				PerfilUsuario.ADMINISTRADOR,
				StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.save(vinculo);
	}

	// Onboarding (painel global): vincula um usuario global JA EXISTENTE como
	// Administrador da empresa recem-criada. Nunca define/altera senha - o
	// usuario so e referenciado pelo id, exatamente como
	// VincularUsuarioEmpresaService faz para o fluxo empresarial comum.
	@Transactional
	public UsuarioEmpresa criarComAdministradorExistente(
			String nomeEmpresa,
			String nomeFantasia,
			String cnpj,
			List<String> aplicacoesIniciais,
			UUID administradorUsuarioId) {
		if (administradorUsuarioId == null) {
			throw new DadosInvalidosException("Administrador e obrigatorio");
		}
		Usuario administrador = usuarioRepository.findById(administradorUsuarioId)
				.orElseThrow(UsuarioNaoEncontradoException::new);
		if (administrador.getStatus() != StatusCadastro.ATIVO) {
			throw new DadosInvalidosException("Usuario selecionado esta inativo");
		}

		Empresa empresa = cadastrarEmpresaService.executar(nomeEmpresa, nomeFantasia, cnpj);
		habilitarAplicacoesIniciais(empresa.getId(), aplicacoesIniciais);

		if (usuarioEmpresaRepository.existsByUsuarioIdAndEmpresaId(administradorUsuarioId, empresa.getId())) {
			throw new UsuarioEmpresaJaVinculadoException();
		}
		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				administrador, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.save(vinculo);
	}

	// Onboarding (painel global): cria a empresa e gera um convite de
	// ADMINISTRADOR para uma pessoa nova. Nenhuma senha e definida aqui - o
	// convidado a escolhe ao aceitar (AceitarConviteService). Se a empresa ja
	// existir com o mesmo CNPJ ou o convite falhar, a transacao inteira e
	// desfeita (nenhuma empresa/aplicacao/convite fica orfao).
	@Transactional
	public EmpresaComConviteAdministrador criarComAdministradorConvidado(
			String nomeEmpresa,
			String nomeFantasia,
			String cnpj,
			List<String> aplicacoesIniciais,
			String nomeAdministrador,
			String emailAdministrador,
			UUID criadoPorUsuarioId) {
		if (nomeAdministrador == null || nomeAdministrador.isBlank()) {
			throw new DadosInvalidosException("Nome do administrador e obrigatorio");
		}
		if (emailAdministrador == null || emailAdministrador.isBlank()) {
			throw new DadosInvalidosException("E-mail do administrador e obrigatorio");
		}

		Empresa empresa = cadastrarEmpresaService.executar(nomeEmpresa, nomeFantasia, cnpj);
		habilitarAplicacoesIniciais(empresa.getId(), aplicacoesIniciais);

		ConviteCriado conviteCriado = conviteService.criarComoSuperAdministrador(
				empresa.getId(), emailAdministrador, PerfilUsuario.ADMINISTRADOR, criadoPorUsuarioId);

		return new EmpresaComConviteAdministrador(empresa, conviteCriado);
	}

	private void habilitarAplicacoesIniciais(UUID empresaId, List<String> aplicacoesIniciais) {
		if (aplicacoesIniciais == null) {
			return;
		}
		for (String codigo : aplicacoesIniciais) {
			if (codigo != null && !codigo.isBlank()) {
				aplicacaoService.habilitar(empresaId, codigo);
			}
		}
	}

	@Transactional(readOnly = true)
	public List<Empresa> listarTodas() {
		return empresaRepository.findAll();
	}

	@Transactional(readOnly = true)
	public Empresa buscarPorId(UUID empresaId) {
		Objects.requireNonNull(empresaId, "empresaId e obrigatorio");
		return empresaRepository.findById(empresaId).orElseThrow(EmpresaNaoEncontradaException::new);
	}

	// Agregado somente de leitura para o detalhe administrativo da empresa;
	// "pronta" nao e um campo persistido, apenas a combinacao das regras
	// descritas no proprio record (nenhuma migration necessaria).
	@Transactional(readOnly = true)
	public EmpresaDetalheDados buscarDetalhe(UUID empresaId) {
		Empresa empresa = buscarPorId(empresaId);
		long usuariosAtivos = usuarioEmpresaRepository.countByEmpresaIdAndStatus(empresaId, StatusCadastro.ATIVO);
		long administradoresAtivos = usuarioEmpresaRepository.countByEmpresaIdAndPerfilAndStatus(
				empresaId, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO);
		List<String> aplicacoesHabilitadas = aplicacaoService.listarAtivasDaEmpresa(empresaId).stream()
				.map(Aplicacao::getCodigo)
				.toList();
		long convitesPendentes = conviteService.listarPorEmpresaComoSuperAdministrador(empresaId).stream()
				.filter(convite -> convite.getStatusEfetivo(OffsetDateTime.now()) == StatusConvite.PENDENTE)
				.count();

		boolean pronta = empresa.getStatus() == StatusCadastro.ATIVO
				&& administradoresAtivos >= 1
				&& !aplicacoesHabilitadas.isEmpty();

		return new EmpresaDetalheDados(
				empresa, usuariosAtivos, administradoresAtivos, aplicacoesHabilitadas, convitesPendentes, pronta);
	}

	@Transactional
	public Empresa ativar(UUID empresaId) {
		Empresa empresa = buscarPorId(empresaId);
		empresa.ativar();
		return empresaRepository.save(empresa);
	}

	// Inativar bloqueia novos acessos (contexto/sessao passam a rejeitar a
	// empresa - ContextoEmpresaService ja valida empresa ATIVA); nada e
	// excluido: usuarios, vinculos, aplicacoes e convites sao preservados.
	@Transactional
	public Empresa inativar(UUID empresaId) {
		Empresa empresa = buscarPorId(empresaId);
		empresa.inativar();
		return empresaRepository.save(empresa);
	}

	public record EmpresaComConviteAdministrador(Empresa empresa, ConviteCriado conviteCriado) {
	}

	public record EmpresaDetalheDados(
			Empresa empresa,
			long quantidadeUsuariosAtivos,
			long administradoresAtivos,
			List<String> aplicacoesHabilitadas,
			long convitesPendentes,
			boolean pronta) {
	}
}
