package br.app.criati.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.convite.model.Convite;
import br.app.criati.convite.repository.ConviteRepository;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusConvite;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// Consulta global de usuarios para o painel do Superadministrador. Sem
// paginacao/filtros no banco de proposito (volume inicial pequeno, decisao
// documentada em docs/PAINEL_ADMINISTRATIVO.md); filtros de busca/status sao
// aplicados em memoria sobre findAll().
@Service
public class AdminUsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final ConviteRepository conviteRepository;

	public AdminUsuarioService(
			UsuarioRepository usuarioRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			ConviteRepository conviteRepository) {
		this.usuarioRepository = usuarioRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.conviteRepository = conviteRepository;
	}

	@Transactional(readOnly = true)
	public List<UsuarioComResumo> listarTodos(String busca, StatusCadastro statusFiltro) {
		String buscaNormalizada = (busca == null || busca.isBlank())
				? null
				: busca.trim().toLowerCase(Locale.ROOT);

		List<Usuario> usuarios = usuarioRepository.findAll().stream()
				.filter(usuario -> statusFiltro == null || usuario.getStatus() == statusFiltro)
				.filter(usuario -> buscaNormalizada == null || corresponde(usuario, buscaNormalizada))
				.toList();

		Map<UUID, List<UsuarioEmpresa>> vinculosPorUsuario = usuarioEmpresaRepository.findAll().stream()
				.collect(Collectors.groupingBy(vinculo -> vinculo.getUsuario().getId()));

		return usuarios.stream()
				.map(usuario -> {
					List<UsuarioEmpresa> vinculos = vinculosPorUsuario.getOrDefault(usuario.getId(), List.of());
					long ativos = vinculos.stream().filter(v -> v.getStatus() == StatusCadastro.ATIVO).count();
					return new UsuarioComResumo(usuario, vinculos.size(), ativos);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public UsuarioDetalheDados buscarDetalhe(UUID usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
		List<UsuarioEmpresa> vinculos = usuarioEmpresaRepository.findAllByUsuarioId(usuarioId);

		OffsetDateTime agora = OffsetDateTime.now();
		List<Convite> convitesPendentes = conviteRepository.findAllByEmailIgnoreCase(usuario.getEmail()).stream()
				.filter(convite -> convite.getStatusEfetivo(agora) == StatusConvite.PENDENTE)
				.toList();

		return new UsuarioDetalheDados(usuario, vinculos, convitesPendentes);
	}

	private boolean corresponde(Usuario usuario, String buscaNormalizada) {
		String nome = usuario.getNome();
		String email = usuario.getEmail();
		return (nome != null && nome.toLowerCase(Locale.ROOT).contains(buscaNormalizada))
				|| (email != null && email.toLowerCase(Locale.ROOT).contains(buscaNormalizada));
	}

	public record UsuarioComResumo(Usuario usuario, long quantidadeEmpresas, long vinculosAtivos) {
	}

	public record UsuarioDetalheDados(Usuario usuario, List<UsuarioEmpresa> vinculos, List<Convite> convitesPendentes) {
	}
}
