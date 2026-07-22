package br.app.criati.financeiro.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraComLancamentosException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.CategoriaFinanceiraNomeDuplicadaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class CategoriaFinanceiraService {

	private final CategoriaFinanceiraRepository categoriaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public CategoriaFinanceiraService(CategoriaFinanceiraRepository categoriaRepository,
			LancamentoFinanceiroRepository lancamentoRepository, EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository) {
		this.categoriaRepository = categoriaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<CategoriaFinanceira> listar(ContextoEmpresaAtual contexto, StatusCadastro status,
			TipoFinanceiro tipo, UUID paiId, String busca, Boolean principais, Boolean permiteOrcamento) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		StatusCadastro statusEfetivo = status == null ? StatusCadastro.ATIVO : status;
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return categoriaRepository.findAllByEmpresaIdAndStatusOrderByOrdemExibicaoAscNomeAsc(
				contexto.empresaId(), statusEfetivo).stream()
				.filter(c -> tipo == null || c.getTipo() == tipo)
				.filter(c -> paiId == null || c.getCategoriaPai() != null && paiId.equals(c.getCategoriaPai().getId()))
				.filter(c -> !Boolean.TRUE.equals(principais) || c.getCategoriaPai() == null)
				.filter(c -> permiteOrcamento == null || c.isPermiteOrcamento() == permiteOrcamento)
				.filter(c -> termo.isBlank() || c.getNome().toLowerCase(Locale.ROOT).contains(termo))
				.sorted(Comparator.comparing(CategoriaFinanceira::getStatus)
						.thenComparingInt(CategoriaFinanceira::getOrdemExibicao)
						.thenComparing(CategoriaFinanceira::getNome, String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CategoriaFinanceira> listar(ContextoEmpresaAtual contexto, StatusCadastro status, TipoFinanceiro tipo) {
		return listar(contexto, status, tipo, null, null, null, null);
	}

	@Transactional(readOnly = true)
	public CategoriaFinanceira buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public long contarSubcategorias(CategoriaFinanceira categoria) {
		return categoriaRepository.countByEmpresaIdAndCategoriaPaiId(categoria.getEmpresa().getId(), categoria.getId());
	}

	@Transactional(readOnly = true)
	public ResumoCategoriasFinanceiras resumir(ContextoEmpresaAtual contexto) {
		List<CategoriaFinanceira> ativas = listar(contexto, StatusCadastro.ATIVO, null);
		return new ResumoCategoriasFinanceiras(ativas.size(),
				ativas.stream().filter(c -> c.getTipo() == TipoFinanceiro.RECEITA).count(),
				ativas.stream().filter(c -> c.getTipo() == TipoFinanceiro.DESPESA).count(),
				ativas.stream().filter(CategoriaFinanceira::isPermiteOrcamento).count(),
				ativas.stream().filter(c -> c.getCategoriaPai() == null).count(),
				ativas.stream().filter(c -> c.getCategoriaPai() != null).count());
	}

	@Transactional
	public CategoriaFinanceira criar(String nome, String descricao, UUID paiId, TipoFinanceiro tipo,
			Integer ordem, Boolean permiteOrcamento, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		DadosCategoria dados = validarDados(nome, descricao, paiId, tipo, ordem, permiteOrcamento, contexto, null);
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		return categoriaRepository.save(new CategoriaFinanceira(empresa, dados.nome(), dados.descricao(), dados.pai(), tipo,
				dados.ordem(), dados.permiteOrcamento(), buscarAutor(contexto.usuarioId())));
	}

	@Transactional
	public CategoriaFinanceira criar(String nome, TipoFinanceiro tipo, ContextoEmpresaAtual contexto) {
		return criar(nome, null, null, tipo, 0, tipo == TipoFinanceiro.DESPESA, contexto);
	}

	@Transactional
	public CategoriaFinanceira editar(UUID id, String nome, String descricao, UUID paiId, TipoFinanceiro tipo,
			Integer ordem, Boolean permiteOrcamento, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CategoriaFinanceira categoria = buscarDaEmpresa(id, contexto.empresaId());
		DadosCategoria dados = validarDados(nome, descricao, paiId, tipo, ordem, permiteOrcamento, contexto, categoria);
		boolean mudouPai = !Objects.equals(idPai(categoria.getCategoriaPai()), idPai(dados.pai()));
		if (lancamentoRepository.existsByCategoriaIdAndEmpresaId(id, contexto.empresaId())
				&& (categoria.getTipo() != tipo || mudouPai)) {
			throw new CategoriaFinanceiraComLancamentosException();
		}
		categoria.atualizarDados(dados.nome(), dados.descricao(), dados.pai(), tipo, dados.ordem(),
				dados.permiteOrcamento(), buscarAutor(contexto.usuarioId()));
		return categoriaRepository.save(categoria);
	}

	@Transactional
	public CategoriaFinanceira editar(UUID id, String nome, TipoFinanceiro tipo, ContextoEmpresaAtual contexto) {
		CategoriaFinanceira atual = buscarDaEmpresa(id, contexto.empresaId());
		return editar(id, nome, atual.getDescricao(), idPai(atual.getCategoriaPai()), tipo, atual.getOrdemExibicao(),
				atual.isPermiteOrcamento(), contexto);
	}

	@Transactional
	public CategoriaFinanceira inativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CategoriaFinanceira categoria = buscarDaEmpresa(id, contexto.empresaId());
		if (!categoria.estaAtiva()) throw new FinanceiroStatusInvalidoException("Categoria financeira ja esta inativa");
		if (categoriaRepository.existsByEmpresaIdAndCategoriaPaiIdAndStatus(
				contexto.empresaId(), id, StatusCadastro.ATIVO)) {
			throw new DadosInvalidosException("Inative ou mova explicitamente as subcategorias ativas antes de inativar a categoria principal");
		}
		categoria.inativar(buscarAutor(contexto.usuarioId()));
		return categoriaRepository.save(categoria);
	}

	@Transactional
	public CategoriaFinanceira reativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CategoriaFinanceira categoria = buscarDaEmpresa(id, contexto.empresaId());
		if (categoria.estaAtiva()) throw new FinanceiroStatusInvalidoException("Categoria financeira ja esta ativa");
		if (categoria.getCategoriaPai() != null && !categoria.getCategoriaPai().estaAtiva()) {
			throw new DadosInvalidosException("A categoria pai precisa estar ativa");
		}
		exigirNomeDisponivel(contexto.empresaId(), idPai(categoria.getCategoriaPai()), categoria.getTipo(),
				categoria.getNome(), categoria.getId());
		categoria.reativar(buscarAutor(contexto.usuarioId()));
		return categoriaRepository.save(categoria);
	}

	private DadosCategoria validarDados(String nome, String descricao, UUID paiId, TipoFinanceiro tipo, Integer ordem,
			Boolean permiteOrcamento, ContextoEmpresaAtual contexto, CategoriaFinanceira atual) {
		String nomeExibicao = validarNome(nome);
		if (tipo == null) throw new DadosInvalidosException("Natureza e obrigatoria");
		int ordemEfetiva = ordem == null ? 0 : ordem;
		if (ordemEfetiva < 0) throw new DadosInvalidosException("Ordem deve ser maior ou igual a zero");
		if (descricao != null && descricao.trim().length() > 500) throw new DadosInvalidosException("Descricao deve possuir no maximo 500 caracteres");
		CategoriaFinanceira pai = validarPai(paiId, tipo, contexto, atual);
		if (atual != null && categoriaRepository.countByEmpresaIdAndCategoriaPaiId(
				contexto.empresaId(), atual.getId()) > 0) {
			if (pai != null) {
				throw new DadosInvalidosException("Categoria com subcategorias nao pode se tornar subcategoria");
			}
			if (atual.getTipo() != tipo) {
				throw new DadosInvalidosException("A natureza da categoria com subcategorias nao pode ser alterada isoladamente");
			}
		}
		exigirNomeDisponivel(contexto.empresaId(), paiId, tipo, nomeExibicao, atual == null ? null : atual.getId());
		boolean permiteOrcamentoEfetivo = permiteOrcamento == null
				? tipo == TipoFinanceiro.DESPESA : permiteOrcamento;
		return new DadosCategoria(nomeExibicao, descricao == null || descricao.isBlank() ? null : descricao.trim(), pai,
				ordemEfetiva, permiteOrcamentoEfetivo);
	}

	private CategoriaFinanceira validarPai(UUID paiId, TipoFinanceiro tipo, ContextoEmpresaAtual contexto,
			CategoriaFinanceira atual) {
		if (paiId == null) return null;
		if (atual != null && paiId.equals(atual.getId())) throw new DadosInvalidosException("Categoria nao pode ser pai de si mesma");
		CategoriaFinanceira pai = buscarDaEmpresa(paiId, contexto.empresaId());
		if (!pai.estaAtiva()) throw new DadosInvalidosException("A categoria pai precisa estar ativa");
		if (atual != null && pai.getCategoriaPai() != null && atual.getId().equals(pai.getCategoriaPai().getId())) {
			throw new DadosInvalidosException("Ciclo de categorias nao permitido");
		}
		if (pai.getCategoriaPai() != null) throw new DadosInvalidosException("A hierarquia permite somente categoria e subcategoria");
		if (pai.getTipo() != tipo) throw new DadosInvalidosException("A natureza da subcategoria deve ser igual a da categoria pai");
		return pai;
	}

	private void exigirNomeDisponivel(UUID empresaId, UUID paiId, TipoFinanceiro tipo, String nome, UUID ignorarId) {
		if (categoriaRepository.existeDuplicada(empresaId, paiId, tipo, nome, StatusCadastro.ATIVO, ignorarId)) {
			throw new CategoriaFinanceiraNomeDuplicadaException();
		}
	}

	private String validarNome(String nome) {
		if (nome == null || nome.isBlank()) throw new DadosInvalidosException("Nome e obrigatorio");
		String exibicao = nome.trim().replaceAll("\\s+", " ");
		if (exibicao.length() > 150) throw new DadosInvalidosException("Nome deve possuir no maximo 150 caracteres");
		return exibicao;
	}

	private CategoriaFinanceira buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) throw new CategoriaFinanceiraNaoEncontradaException();
		return categoriaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID id) {
		return usuarioRepository.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private UUID idPai(CategoriaFinanceira pai) { return pai == null ? null : pai.getId(); }

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) throw new AcessoNegadoException();
	}

	private record DadosCategoria(String nome, String descricao, CategoriaFinanceira pai, int ordem,
			boolean permiteOrcamento) { }
}
