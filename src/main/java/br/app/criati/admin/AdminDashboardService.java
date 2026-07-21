package br.app.criati.admin;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.usuario.repository.UsuarioRepository;

// Visao geral do painel global: numeros reais, sempre recalculados na
// consulta (nenhum contador cacheado/incrementado que possa divergir do
// estado real do banco).
@Service
public class AdminDashboardService {

	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final EmpresaAplicacaoRepository empresaAplicacaoRepository;
	private final ConviteRepository conviteRepository;

	public AdminDashboardService(
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			EmpresaAplicacaoRepository empresaAplicacaoRepository,
			ConviteRepository conviteRepository) {
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.empresaAplicacaoRepository = empresaAplicacaoRepository;
		this.conviteRepository = conviteRepository;
	}

	@Transactional(readOnly = true)
	public AdminDashboardDados calcular() {
		long totalEmpresas = empresaRepository.count();
		long empresasAtivas = empresaRepository.countByStatus(StatusCadastro.ATIVO);
		long empresasInativas = empresaRepository.countByStatus(StatusCadastro.INATIVO);

		long totalUsuarios = usuarioRepository.count();
		long usuariosAtivos = usuarioRepository.countByStatus(StatusCadastro.ATIVO);

		long totalVinculosAtivos = usuarioEmpresaRepository.countByStatus(StatusCadastro.ATIVO);

		OffsetDateTime agora = OffsetDateTime.now();
		long convitesPendentes = conviteRepository.findAll().stream()
				.filter(convite -> convite.getStatusEfetivo(agora) == StatusConvite.PENDENTE)
				.count();

		long empresasComFinanceiroHabilitado = empresaAplicacaoRepository.countByAplicacaoCodigoAndStatus(
				CodigoAplicacao.FINANCEIRO.name(), StatusCadastro.ATIVO);

		List<Empresa> empresasRecentes = empresaRepository.findTop5ByOrderByCriadoEmDesc();

		return new AdminDashboardDados(
				totalEmpresas,
				empresasAtivas,
				empresasInativas,
				totalUsuarios,
				usuariosAtivos,
				totalVinculosAtivos,
				convitesPendentes,
				empresasComFinanceiroHabilitado,
				empresasRecentes);
	}

	public record AdminDashboardDados(
			long totalEmpresas,
			long empresasAtivas,
			long empresasInativas,
			long totalUsuarios,
			long usuariosAtivos,
			long totalVinculosAtivos,
			long convitesPendentes,
			long empresasComFinanceiroHabilitado,
			List<Empresa> empresasRecentes) {
	}
}
