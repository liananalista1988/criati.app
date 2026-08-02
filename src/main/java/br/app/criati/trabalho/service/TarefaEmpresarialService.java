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
import br.app.criati.exception.TarefaTrabalhoNaoEncontradaException;
import br.app.criati.exception.UsuarioEmpresaNaoEncontradoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoEventoHistoricoTrabalho;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.trabalho.model.HistoricoTrabalho;
import br.app.criati.trabalho.model.ProcessoEmpresarial;
import br.app.criati.trabalho.model.TarefaEmpresarial;
import br.app.criati.trabalho.repository.HistoricoTrabalhoRepository;
import br.app.criati.trabalho.repository.ProcessoEmpresarialRepository;
import br.app.criati.trabalho.repository.TarefaEmpresarialRepository;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class TarefaEmpresarialService {

	private final TarefaEmpresarialRepository tarefaRepository;
	private final ProcessoEmpresarialRepository processoRepository;
	private final HistoricoTrabalhoRepository historicoRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final HistoricoTrabalhoService historicoService;

	public TarefaEmpresarialService(TarefaEmpresarialRepository tarefaRepository,
			ProcessoEmpresarialRepository processoRepository, HistoricoTrabalhoRepository historicoRepository,
			EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
			UsuarioEmpresaRepository usuarioEmpresaRepository, HistoricoTrabalhoService historicoService) {
		this.tarefaRepository = tarefaRepository;
		this.processoRepository = processoRepository;
		this.historicoRepository = historicoRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.historicoService = historicoService;
	}

	@Transactional(readOnly = true)
	public PaginaTarefasEmpresariais listarPagina(ContextoEmpresaAtual contexto, String busca, UUID processoId,
			SituacaoTarefaTrabalho situacao, PrioridadeTrabalho prioridade, UUID responsavelId,
			boolean minhasTarefas, LocalDate prazoInicio, LocalDate prazoFim, Boolean atrasada,
			StatusCadastro status, int pagina, int tamanho, String ordenarPor, String direcao) {
		if (pagina < 0) throw new DadosInvalidosException("Pagina deve ser maior ou igual a zero");
		if (tamanho < 1 || tamanho > 100) throw new DadosInvalidosException("Tamanho da pagina deve estar entre 1 e 100");
		if (prazoInicio != null && prazoFim != null && prazoInicio.isAfter(prazoFim)) {
			throw new DadosInvalidosException("Prazo inicial nao pode ser posterior ao final");
		}
		String propriedade = propriedadeOrdenacao(ordenarPor);
		Sort.Direction sentido = sentido(direcao);
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		StatusCadastro statusEfetivo = status == null ? StatusCadastro.ATIVO : status;
		UUID responsavelEfetivo = minhasTarefas ? contexto.usuarioEmpresaId() : responsavelId;
		LocalDate hoje = LocalDate.now();

		Specification<TarefaEmpresarial> especificacao = (raiz, consulta, cb) -> cb.and(
				cb.equal(raiz.get("empresa").get("id"), contexto.empresaId()),
				cb.equal(raiz.get("status"), statusEfetivo),
				termo.isBlank() ? cb.conjunction() : cb.like(cb.lower(raiz.get("titulo")), "%" + termo + "%"),
				processoId == null ? cb.conjunction() : cb.equal(raiz.get("processo").get("id"), processoId),
				situacao == null ? cb.conjunction() : cb.equal(raiz.get("situacao"), situacao),
				prioridade == null ? cb.conjunction() : cb.equal(raiz.get("prioridade"), prioridade),
				responsavelEfetivo == null ? cb.conjunction() : cb.equal(raiz.get("responsavel").get("id"), responsavelEfetivo),
				prazoInicio == null ? cb.conjunction() : cb.greaterThanOrEqualTo(raiz.get("prazo"), prazoInicio),
				prazoFim == null ? cb.conjunction() : cb.lessThanOrEqualTo(raiz.get("prazo"), prazoFim),
				atrasada == null ? cb.conjunction() : atrasada
						? cb.and(cb.isNotNull(raiz.get("prazo")), cb.isNull(raiz.get("dataConclusao")),
								cb.lessThan(raiz.get("prazo"), hoje),
								raiz.get("situacao").in(SituacaoTarefaTrabalho.PENDENTE, SituacaoTarefaTrabalho.EM_ANDAMENTO))
						: cb.or(cb.isNull(raiz.get("prazo")), cb.isNotNull(raiz.get("dataConclusao")),
								cb.greaterThanOrEqualTo(raiz.get("prazo"), hoje),
								raiz.get("situacao").in(SituacaoTarefaTrabalho.CONCLUIDA, SituacaoTarefaTrabalho.CANCELADA)));

		Page<TarefaEmpresarial> resultado = tarefaRepository.findAll(especificacao,
				PageRequest.of(pagina, tamanho, Sort.by(sentido, propriedade).and(Sort.by(Sort.Direction.DESC, "id"))));
		return new PaginaTarefasEmpresariais(resultado.getContent(), pagina, tamanho, resultado.getTotalElements(),
				resultado.getTotalPages(), ordenarPor, sentido.name().toLowerCase(Locale.ROOT));
	}

	private String propriedadeOrdenacao(String ordenarPor) {
		return switch (ordenarPor == null ? "criadoEm" : ordenarPor) {
			case "titulo" -> "titulo";
			case "situacao" -> "situacao";
			case "prioridade" -> "prioridade";
			case "prazo" -> "prazo";
			case "criadoEm" -> "criadoEm";
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
	public TarefaEmpresarial buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<HistoricoTrabalho> historico(UUID id, ContextoEmpresaAtual contexto) {
		buscarDaEmpresa(id, contexto.empresaId());
		return historicoRepository.findAllByTarefaIdAndEmpresaIdOrderByOcorridoEmDesc(id, contexto.empresaId());
	}

	@Transactional
	public TarefaEmpresarial criar(UUID processoId, String titulo, String descricao, UUID responsavelId,
			PrioridadeTrabalho prioridade, LocalDate prazo, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		String tituloValido = validarTitulo(titulo);
		PrioridadeTrabalho prioridadeEfetiva = prioridade == null ? PrioridadeTrabalho.MEDIA : prioridade;
		ProcessoEmpresarial processo = buscarProcesso(processoId, contexto.empresaId());
		UsuarioEmpresa responsavel = buscarResponsavel(responsavelId, contexto.empresaId());
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());
		TarefaEmpresarial tarefa = tarefaRepository.save(new TarefaEmpresarial(empresa, processo, tituloValido,
				normalizarDescricao(descricao), responsavel, prioridadeEfetiva, prazo, autor));
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.CRIACAO,
				"Tarefa criada", null, tituloValido, autor);
		return tarefa;
	}

	@Transactional
	public TarefaEmpresarial editar(UUID id, String titulo, String descricao, PrioridadeTrabalho prioridade,
			LocalDate prazo, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		String tituloValido = validarTitulo(titulo);
		PrioridadeTrabalho prioridadeEfetiva = prioridade == null ? tarefa.getPrioridade() : prioridade;
		tarefa.atualizarDados(tituloValido, normalizarDescricao(descricao), prioridadeEfetiva, prazo,
				buscarAutor(contexto.usuarioId()));
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial atribuirResponsavel(UUID id, UUID responsavelId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		UsuarioEmpresa responsavelAnterior = tarefa.getResponsavel();
		UsuarioEmpresa responsavel = buscarResponsavel(responsavelId, contexto.empresaId());
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.atribuirResponsavel(responsavel, autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.ALTERACAO_RESPONSAVEL,
				"Responsavel alterado", nomeResponsavel(responsavelAnterior), nomeResponsavel(responsavel), autor);
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial iniciar(UUID id, ContextoEmpresaAtual contexto) {
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		exigirEscritaOuResponsavel(contexto, tarefa);
		SituacaoTarefaTrabalho anterior = tarefa.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.iniciar(autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.ALTERACAO_SITUACAO,
				"Tarefa iniciada", anterior.name(), tarefa.getSituacao().name(), autor);
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial concluir(UUID id, ContextoEmpresaAtual contexto) {
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		exigirEscritaOuResponsavel(contexto, tarefa);
		SituacaoTarefaTrabalho anterior = tarefa.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.concluir(autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.CONCLUSAO,
				"Tarefa concluida", anterior.name(), tarefa.getSituacao().name(), autor);
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial reabrir(UUID id, ContextoEmpresaAtual contexto) {
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		exigirEscritaOuResponsavel(contexto, tarefa);
		SituacaoTarefaTrabalho anterior = tarefa.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.reabrir(autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.REABERTURA,
				"Tarefa reaberta", anterior.name(), tarefa.getSituacao().name(), autor);
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial cancelar(UUID id, ContextoEmpresaAtual contexto) {
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		exigirEscritaOuResponsavel(contexto, tarefa);
		SituacaoTarefaTrabalho anterior = tarefa.getSituacao();
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.cancelar(autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.ALTERACAO_SITUACAO,
				"Tarefa cancelada", anterior.name(), tarefa.getSituacao().name(), autor);
		return tarefaRepository.save(tarefa);
	}

	@Transactional
	public TarefaEmpresarial inativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		TarefaEmpresarial tarefa = buscarDaEmpresa(id, contexto.empresaId());
		Usuario autor = buscarAutor(contexto.usuarioId());
		tarefa.inativar(autor);
		historicoService.registrarTarefa(tarefa, TipoEventoHistoricoTrabalho.INATIVACAO,
				"Tarefa inativada", StatusCadastro.ATIVO.name(), StatusCadastro.INATIVO.name(), autor);
		return tarefaRepository.save(tarefa);
	}

	private ProcessoEmpresarial buscarProcesso(UUID processoId, UUID empresaId) {
		if (processoId == null) return null;
		return processoRepository.findByIdAndEmpresaId(processoId, empresaId)
				.orElseThrow(ProcessoTrabalhoNaoEncontradoException::new);
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

	private TarefaEmpresarial buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) throw new TarefaTrabalhoNaoEncontradaException();
		return tarefaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(TarefaTrabalhoNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID id) {
		return usuarioRepository.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	// CRIATI-WRK-001 restringe escrita estrutural a ADMINISTRADOR nesta primeira
	// versao (ver mesma nota em ProcessoEmpresarialService.exigirEscrita).
	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	// Alem de ADMINISTRADOR, o proprio responsavel pela tarefa pode atualizar o
	// andamento (iniciar/concluir/reabrir/cancelar) dela mesma - nunca de
	// tarefas de outros, nunca campos estruturais (titulo/processo/responsavel).
	private void exigirEscritaOuResponsavel(ContextoEmpresaAtual contexto, TarefaEmpresarial tarefa) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() == PerfilUsuario.ADMINISTRADOR) return;
		if (tarefa.getResponsavel() != null && tarefa.getResponsavel().getId().equals(contexto.usuarioEmpresaId())) return;
		throw new AcessoNegadoException();
	}
}
