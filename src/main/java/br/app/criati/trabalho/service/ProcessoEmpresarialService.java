package br.app.criati.trabalho.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.ProcessoTrabalhoNaoEncontradoException;
import br.app.criati.exception.UsuarioEmpresaNaoEncontradoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoProcessoTrabalho;
import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoEventoHistoricoTrabalho;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.trabalho.model.HistoricoTrabalho;
import br.app.criati.trabalho.model.ProcessoEmpresarial;
import br.app.criati.trabalho.repository.HistoricoTrabalhoRepository;
import br.app.criati.trabalho.repository.ProcessoEmpresarialRepository;
import br.app.criati.trabalho.repository.TarefaEmpresarialRepository;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ProcessoEmpresarialService {

	private static final List<SituacaoTarefaTrabalho> SITUACOES_TAREFA_PENDENTES =
			List.of(SituacaoTarefaTrabalho.PENDENTE, SituacaoTarefaTrabalho.EM_ANDAMENTO);

	private final ProcessoEmpresarialRepository processoRepository;
	private final TarefaEmpresarialRepository tarefaRepository;
	private final HistoricoTrabalhoRepository historicoRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final HistoricoTrabalhoService historicoService;

	public ProcessoEmpresarialService(ProcessoEmpresarialRepository processoRepository,
			TarefaEmpresarialRepository tarefaRepository, HistoricoTrabalhoRepository historicoRepository,
			EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository, HistoricoTrabalhoService historicoService) {
		this.processoRepository = processoRepository;
		this.tarefaRepository = tarefaRepository;
		this.historicoRepository = historicoRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.historicoService = historicoService;
	}

	@Transactional(readOnly = true)
	public ContagemTarefasProcesso contarTarefas(ProcessoEmpresarial processo) {
		UUID processoId = processo.getId();
		UUID empresaId = processo.getEmpresa().getId();
		long total = tarefaRepository.countByProcessoIdAndEmpresaIdAndStatus(processoId, empresaId, StatusCadastro.ATIVO);
		long concluidas = tarefaRepository.countByProcessoIdAndEmpresaIdAndStatusAndSituacao(
				processoId, empresaId, StatusCadastro.ATIVO, SituacaoTarefaTrabalho.CONCLUIDA);
		return new ContagemTarefasProcesso(total, concluidas);
	}

	public record ContagemTarefasProcesso(long total, long concluidas) { }

	@Transactional(readOnly = true)
	public PaginaProcessosEmpresariais listarPagina(ContextoEmpresaAtual contexto, String busca,
			SituacaoProcessoTrabalho situacao, PrioridadeTrabalho prioridade, UUID responsavelId,
			LocalDate prazoInicio, LocalDate prazoFim, Boolean atrasado, StatusCadastro status,
			int pagina, int tamanho, String ordenarPor, String direcao) {
		if (pagina < 0) throw new DadosInvalidosException("Pagina deve ser maior ou igual a zero");
		if (tamanho < 1 || tamanho > 100) throw new DadosInvalidosException("Tamanho da pagina deve estar entre 1 e 100");
		if (prazoInicio != null && prazoFim != null && prazoInicio.isAfter(prazoFim)) {
			throw new DadosInvalidosException("Prazo inicial nao pode ser posterior ao final");
		}
		String propriedade = propriedadeOrdenacao(ordenarPor);
		Sort.Direction sentido = sentido(direcao);
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		StatusCadastro statusEfetivo = status == null ? StatusCadastro.ATIVO : status;
		LocalDate hoje = LocalDate.now();

		Specification<ProcessoEmpresarial> especificacao = (raiz, consulta, cb) -> cb.and(
				cb.equal(raiz.get("empresa").get("id"), contexto.empresaId()),
				cb.equal(raiz.get("status"), statusEfetivo),
				termo.isBlank() ? cb.conjunction() : cb.like(cb.lower(raiz.get("titulo")), "%" + termo + "%"),
				situacao == null ? cb.conjunction() : cb.equal(raiz.get("situacao"), situacao),
				prioridade == null ? cb.conjunction() : cb.equal(raiz.get("prioridade"), prioridade),
				responsavelId == null ? cb.conjunction() : cb.equal(raiz.get("responsavel").get("id"), responsavelId),
				prazoInicio == null ? cb.conjunction() : cb.greaterThanOrEqualTo(raiz.get("prazo"), prazoInicio),
				prazoFim == null ? cb.conjunction() : cb.lessThanOrEqualTo(raiz.get("prazo"), prazoFim),
				atrasado == null ? cb.conjunction() : atrasado
						? cb.and(cb.isNotNull(raiz.get("prazo")), cb.isNull(raiz.get("dataConclusao")),
								cb.lessThan(raiz.get("prazo"), hoje),
								raiz.get("situacao").in(SituacaoProcessoTrabalho.ABERTO, SituacaoProcessoTrabalho.EM_ANDAMENTO))
						: cb.or(cb.isNull(raiz.get("prazo")), cb.isNotNull(raiz.get("dataConclusao")),
								cb.greaterThanOrEqualTo(raiz.get("prazo"), hoje),
								raiz.get("situacao").in(SituacaoProcessoTrabalho.CONCLUIDO, SituacaoProcessoTrabalho.CANCELADO)));

		Page<ProcessoEmpresarial> resultado = processoRepository.findAll(especificacao,
				PageRequest.of(pagina, tamanho, Sort.by(sentido, propriedade).and(Sort.by(Sort.Direction.DESC, "id"))));
		return new PaginaProcessosEmpresariais(resultado.getContent(), pagina, tamanho, resultado.getTotalElements(),
				resultado.getTotalPages(), ordenarPor, sentido.name().toLowerCase(Locale.ROOT));
	}

	private String propriedadeOrdenacao(String ordenarPor) {
		return switch (ordenarPor == null ? "abertura" : ordenarPor) {
			case "titulo" -> "titulo";
			case "situacao" -> "situacao";
			case "prioridade" -> "prioridade";
			case "abertura" -> "dataAbertura";
			case "prazo" -> "prazo";
			default -> throw new DadosInvalidosException("Campo de ordenacao invalido");
		};
	}

	private Sort.Direction sentido(String direcao) {
		Sort.Direction sentido = "asc".equalsIgnoreCase(direcao) ? Sort.Direction.ASC :
				"desc".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : null;
		if (sentido == null) throw new DadosInvalidosException("Direcao de ordenacao invalida");
		return sentido;
	}

	@Transactional(readOnly = true)
	public ProcessoEmpresarial buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<HistoricoTrabalho> historico(UUID id, ContextoEmpresaAtual contexto) {
		buscarDaEmpresa(id, contexto.empresaId());
		return historicoRepository.findAllByProcessoIdAndEmpresaIdOrderByOcorridoEmDesc(id, contexto.empresaId());
	}

	@Transactional
	public ProcessoEmpresarial criar(String titulo, String descricao, UUID responsavelId,
			PrioridadeTrabalho prioridade, LocalDate prazo, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		String tituloValido = validarTitulo(titulo);
		PrioridadeTrabalho prioridadeEfetiva = prioridade == null ? PrioridadeTrabalho.MEDIA : prioridade;
		UsuarioEmpresa responsavel = buscarResponsavel(responsavelId, contexto.empresaId());
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());
		ProcessoEmpresarial processo = processoRepository.save(new ProcessoEmpresarial(empresa, tituloValido,
				normalizarDescricao(descricao), responsavel, prioridadeEfetiva, prazo, autor));
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.CRIACAO,
				"Processo criado", null, tituloValido, autor);
		return processo;
	}

	@Transactional
	public ProcessoEmpresarial editar(UUID id, String titulo, String descricao, PrioridadeTrabalho prioridade,
			LocalDate prazo, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		String tituloValido = validarTitulo(titulo);
		PrioridadeTrabalho prioridadeEfetiva = prioridade == null ? processo.getPrioridade() : prioridade;
		processo.atualizarDados(tituloValido, normalizarDescricao(descricao), prioridadeEfetiva, prazo,
				buscarAutor(contexto.usuarioId()));
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial atribuirResponsavel(UUID id, UUID responsavelId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		UsuarioEmpresa responsavelAnterior = processo.getResponsavel();
		UsuarioEmpresa responsavel = buscarResponsavel(responsavelId, contexto.empresaId());
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.atribuirResponsavel(responsavel, autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.ALTERACAO_RESPONSAVEL,
				"Responsavel alterado", nomeResponsavel(responsavelAnterior), nomeResponsavel(responsavel), autor);
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial iniciar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		SituacaoProcessoTrabalho anterior = processo.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.iniciar(autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.ALTERACAO_SITUACAO,
				"Processo iniciado", anterior.name(), processo.getSituacao().name(), autor);
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial concluir(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		if (processoRepository.existemTarefasAtivasNaSituacao(id, contexto.empresaId(), SITUACOES_TAREFA_PENDENTES)) {
			throw new DadosInvalidosException("Processo possui tarefas pendentes ou em andamento e nao pode ser concluido");
		}
		SituacaoProcessoTrabalho anterior = processo.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.concluir(autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.CONCLUSAO,
				"Processo concluido", anterior.name(), processo.getSituacao().name(), autor);
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial reabrir(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		SituacaoProcessoTrabalho anterior = processo.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.reabrir(autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.REABERTURA,
				"Processo reaberto", anterior.name(), processo.getSituacao().name(), autor);
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial cancelar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		SituacaoProcessoTrabalho anterior = processo.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.cancelar(autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.ALTERACAO_SITUACAO,
				"Processo cancelado", anterior.name(), processo.getSituacao().name(), autor);
		return processoRepository.save(processo);
	}

	@Transactional
	public ProcessoEmpresarial inativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ProcessoEmpresarial processo = buscarDaEmpresa(id, contexto.empresaId());
		Usuario autor = buscarAutor(contexto.usuarioId());
		processo.inativar(autor);
		historicoService.registrarProcesso(processo, TipoEventoHistoricoTrabalho.INATIVACAO,
				"Processo inativado", StatusCadastro.ATIVO.name(), StatusCadastro.INATIVO.name(), autor);
		return processoRepository.save(processo);
	}

	private UsuarioEmpresa buscarResponsavel(UUID responsavelId, UUID empresaId) {
		if (responsavelId == null) return null;
		UsuarioEmpresa responsavel = usuarioEmpresaRepository.findByIdAndEmpresaId(responsavelId, empresaId)
				.orElseThrow(UsuarioEmpresaNaoEncontradoException::new);
		if (responsavel.getStatus() != StatusCadastro.ATIVO) {
			throw new DadosInvalidosException("Responsavel precisa possuir vinculo ativo com a empresa");
		}
		return responsavel;
	}

	private String nomeResponsavel(UsuarioEmpresa responsavel) {
		return responsavel == null ? null : responsavel.getUsuario().getNome();
	}

	private String validarTitulo(String titulo) {
		if (titulo == null || titulo.isBlank()) throw new DadosInvalidosException("Titulo e obrigatorio");
		String exibicao = titulo.trim().replaceAll("\\s+", " ");
		if (exibicao.length() > 200) throw new DadosInvalidosException("Titulo deve possuir no maximo 200 caracteres");
		return exibicao;
	}

	private String normalizarDescricao(String descricao) {
		return descricao == null || descricao.isBlank() ? null : descricao.trim();
	}

	private ProcessoEmpresarial buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) throw new ProcessoTrabalhoNaoEncontradoException();
		return processoRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(ProcessoTrabalhoNaoEncontradoException::new);
	}

	private Usuario buscarAutor(UUID id) {
		return usuarioRepository.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	// CRIATI-WRK-001 restringe escrita (criar/editar/inativar/situacao/responsavel)
	// a ADMINISTRADOR nesta primeira versao - GESTOR nao foi mencionado no
	// escopo aprovado, ao contrario do Financeiro; nao estender sem decisao
	// explicita (ver relatorio final: limitacao de autorizacao documentada).
	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
