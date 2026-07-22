package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RecorrenciaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RecorrenciaFinanceiraStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.RecorrenciaFinanceiraRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusRecorrencia;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class RecorrenciaFinanceiraService {

	private final RecorrenciaFinanceiraRepository recorrenciaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final ParteFinanceiraRepository parteRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public RecorrenciaFinanceiraService(RecorrenciaFinanceiraRepository recorrenciaRepository,
			LancamentoFinanceiroRepository lancamentoRepository, ContaFinanceiraRepository contaRepository,
			CategoriaFinanceiraRepository categoriaRepository, PessoaFinanceiraRepository pessoaRepository,
			ParteFinanceiraRepository parteRepository, EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository) {
		this.recorrenciaRepository = recorrenciaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.contaRepository = contaRepository;
		this.categoriaRepository = categoriaRepository;
		this.pessoaRepository = pessoaRepository;
		this.parteRepository = parteRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<RecorrenciaFinanceira> listar(ContextoEmpresaAtual contexto, StatusRecorrencia status,
			TipoFinanceiro tipo, PeriodicidadeRecorrencia periodicidade, UUID pessoaId, UUID categoriaId,
			String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return recorrenciaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(r -> status == null || r.getStatus() == status)
				.filter(r -> tipo == null || r.getTipo() == tipo)
				.filter(r -> periodicidade == null || r.getPeriodicidade() == periodicidade)
				.filter(r -> pessoaId == null || pessoaId.equals(r.getPessoaFinanceira().getId()))
				.filter(r -> categoriaId == null || categoriaId.equals(r.getCategoria().getId()))
				.filter(r -> termo.isBlank() || r.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> a.getDescricao().compareToIgnoreCase(b.getDescricao()))
				.toList();
	}

	@Transactional(readOnly = true)
	public RecorrenciaFinanceira buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<LancamentoFinanceiro> listarOcorrencias(UUID id, ContextoEmpresaAtual contexto) {
		buscarDaEmpresa(id, contexto.empresaId());
		return lancamentoRepository.findAllByEmpresaIdAndRecorrenciaIdOrderByDataCompetenciaDesc(
				contexto.empresaId(), id);
	}

	@Transactional(readOnly = true)
	public long contarOcorrencias(UUID recorrenciaId) {
		return lancamentoRepository.countByRecorrenciaId(recorrenciaId);
	}

	@Transactional(readOnly = true)
	public LocalDate ultimaCompetenciaGerada(UUID recorrenciaId, UUID empresaId) {
		return lancamentoRepository.findAllByEmpresaIdAndRecorrenciaIdOrderByDataCompetenciaDesc(empresaId, recorrenciaId)
				.stream().findFirst().map(LancamentoFinanceiro::getDataCompetencia).orElse(null);
	}

	@Transactional(readOnly = true)
	public ResumoRecorrenciasFinanceiras resumir(ContextoEmpresaAtual contexto) {
		List<RecorrenciaFinanceira> todas = recorrenciaRepository.findAllByEmpresaId(contexto.empresaId());
		long ativas = todas.stream().filter(r -> r.getStatus() == StatusRecorrencia.ATIVA).count();
		long pausadas = todas.stream().filter(r -> r.getStatus() == StatusRecorrencia.PAUSADA).count();
		long encerradas = todas.stream().filter(r -> r.getStatus() == StatusRecorrencia.ENCERRADA).count();
		BigDecimal receitasPrevistas = somarValorPadrao(todas, TipoFinanceiro.RECEITA);
		BigDecimal despesasPrevistas = somarValorPadrao(todas, TipoFinanceiro.DESPESA);
		YearMonth atual = YearMonth.now();
		long geradasNaCompetencia = lancamentoRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(l -> l.getOrigem() == OrigemLancamentoFinanceiro.RECORRENCIA)
				.filter(l -> YearMonth.from(l.getDataCompetencia()).equals(atual))
				.count();
		List<ProximaOcorrenciaResumo> proximas = todas.stream()
				.filter(r -> r.getStatus() == StatusRecorrencia.ATIVA)
				.sorted((a, b) -> a.getProximaCompetencia().compareTo(b.getProximaCompetencia()))
				.limit(10)
				.map(r -> new ProximaOcorrenciaResumo(r.getId(), r.getDescricao(), r.getTipo(), r.getValorPadrao(),
						r.getProximaCompetencia()))
				.toList();
		return new ResumoRecorrenciasFinanceiras(ativas, pausadas, encerradas, receitasPrevistas, despesasPrevistas,
				geradasNaCompetencia, proximas);
	}

	@Transactional
	public RecorrenciaFinanceira criar(TipoFinanceiro tipo, String descricao, BigDecimal valorPadrao, UUID contaId,
			UUID categoriaId, UUID pessoaId, UUID parteId, FormaPagamentoLancamento formaPagamento,
			PeriodicidadeRecorrencia periodicidade, Integer intervalo, Integer diaReferencia, Integer mesReferencia,
			LocalDate dataInicial, LocalDate dataFinal, boolean gerarAutomaticamente, String observacao,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		validarBasicos(tipo, descricao, valorPadrao, periodicidade, intervalo, diaReferencia, mesReferencia,
				dataInicial, dataFinal);
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId(), null);
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), tipo, null);
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), null);
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), null);
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		RecorrenciaFinanceira recorrencia = new RecorrenciaFinanceira(empresa, tipo, normalizarDescricao(descricao),
				MoedaUtils.normalizar(valorPadrao), conta, categoria, pessoa, parte, formaPagamento, periodicidade,
				intervalo, diaReferencia, periodicidade == PeriodicidadeRecorrencia.ANUAL ? mesReferencia : null,
				dataInicial, dataFinal, gerarAutomaticamente, normalizarOpcional(observacao), buscarAutor(contexto));
		return recorrenciaRepository.save(recorrencia);
	}

	@Transactional
	public RecorrenciaFinanceira editar(UUID id, String descricao, BigDecimal valorPadrao, UUID contaId,
			UUID categoriaId, UUID pessoaId, UUID parteId, FormaPagamentoLancamento formaPagamento,
			Integer intervalo, Integer diaReferencia, Integer mesReferencia, LocalDate dataFinal,
			boolean gerarAutomaticamente, String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		RecorrenciaFinanceira atual = buscarDaEmpresa(id, contexto.empresaId());
		if (atual.getStatus() == StatusRecorrencia.ENCERRADA) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia encerrada nao pode ser editada");
		}
		validarBasicos(atual.getTipo(), descricao, valorPadrao, atual.getPeriodicidade(), intervalo, diaReferencia,
				mesReferencia, atual.getDataInicial(), dataFinal);
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId(), atual.getConta());
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), atual.getTipo(), atual.getCategoria());
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), atual.getPessoaFinanceira());
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), atual.getParteFinanceira());
		atual.atualizarSerie(normalizarDescricao(descricao), MoedaUtils.normalizar(valorPadrao), conta, categoria,
				pessoa, parte, formaPagamento, intervalo, diaReferencia,
				atual.getPeriodicidade() == PeriodicidadeRecorrencia.ANUAL ? mesReferencia : null, dataFinal,
				gerarAutomaticamente, normalizarOpcional(observacao), buscarAutor(contexto));
		return recorrenciaRepository.save(atual);
	}

	@Transactional
	public RecorrenciaFinanceira pausar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		RecorrenciaFinanceira r = buscarDaEmpresa(id, contexto.empresaId());
		if (r.getStatus() != StatusRecorrencia.ATIVA) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia nao esta ativa");
		}
		r.pausar(buscarAutor(contexto));
		return recorrenciaRepository.save(r);
	}

	@Transactional
	public RecorrenciaFinanceira retomar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		RecorrenciaFinanceira r = buscarDaEmpresa(id, contexto.empresaId());
		if (r.getStatus() != StatusRecorrencia.PAUSADA) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia nao esta pausada");
		}
		r.retomar(YearMonth.now(), buscarAutor(contexto));
		return recorrenciaRepository.save(r);
	}

	@Transactional
	public RecorrenciaFinanceira encerrar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		RecorrenciaFinanceira r = buscarDaEmpresa(id, contexto.empresaId());
		if (r.getStatus() == StatusRecorrencia.ENCERRADA) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia ja esta encerrada");
		}
		r.encerrar(buscarAutor(contexto));
		return recorrenciaRepository.save(r);
	}

	@Transactional
	public LancamentoFinanceiro gerarOcorrencia(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		RecorrenciaFinanceira r = buscarDaEmpresa(id, contexto.empresaId());
		exigirAtiva(r);
		YearMonth alvo = r.getProximaCompetencia();
		if (alvo.isAfter(YearMonth.now())) {
			throw new DadosInvalidosException("Ainda nao e possivel gerar: proxima competencia esta fora do periodo");
		}
		if (!r.dentroDoPeriodo(alvo)) {
			throw new DadosInvalidosException("Competencia fora do periodo da recorrencia");
		}
		return gerarOuRetornarExistente(r, alvo, contexto, true);
	}

	@Transactional
	public LancamentoFinanceiro gerarCompetenciaEspecifica(UUID id, YearMonth competencia, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (competencia == null) {
			throw new DadosInvalidosException("Competencia e obrigatoria");
		}
		RecorrenciaFinanceira r = buscarDaEmpresa(id, contexto.empresaId());
		exigirAtiva(r);
		if (competencia.isAfter(YearMonth.now())) {
			throw new DadosInvalidosException("Nao e possivel gerar uma competencia futura");
		}
		if (!r.dentroDoPeriodo(competencia)) {
			throw new DadosInvalidosException("Competencia fora do periodo da recorrencia");
		}
		boolean eraProxima = competencia.equals(r.getProximaCompetencia());
		return gerarOuRetornarExistente(r, competencia, contexto, eraProxima);
	}

	/** Processa em lote, uma competencia por vez, as recorrencias ativas marcadas para geracao automatica. */
	@Transactional
	public int gerarAutomaticas(ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		YearMonth atual = YearMonth.now();
		List<RecorrenciaFinanceira> elegiveis = recorrenciaRepository
				.findAllByEmpresaIdAndStatusAndGerarAutomaticamenteTrueAndProximaCompetenciaLessThanEqual(
						contexto.empresaId(), StatusRecorrencia.ATIVA, atual.atDay(1));
		int geradas = 0;
		for (RecorrenciaFinanceira r : elegiveis) {
			YearMonth alvo = r.getProximaCompetencia();
			if (!r.dentroDoPeriodo(alvo)) {
				continue;
			}
			boolean jaExistia = lancamentoRepository.existsByRecorrenciaIdAndDataCompetencia(r.getId(), alvo.atDay(1));
			gerarOuRetornarExistente(r, alvo, contexto, true);
			if (!jaExistia) {
				geradas++;
			}
		}
		return geradas;
	}

	private LancamentoFinanceiro gerarOuRetornarExistente(RecorrenciaFinanceira r, YearMonth competencia,
			ContextoEmpresaAtual contexto, boolean avancarSeProxima) {
		LocalDate dataCompetencia = competencia.atDay(1);
		Optional<LancamentoFinanceiro> existente = lancamentoRepository
				.findByRecorrenciaIdAndDataCompetencia(r.getId(), dataCompetencia);
		if (existente.isPresent()) {
			return existente.get();
		}
		LocalDate vencimento = r.calcularVencimento(competencia);
		Usuario autor = buscarAutor(contexto);
		LancamentoFinanceiro gerado = LancamentoFinanceiro.gerarDeRecorrencia(r, dataCompetencia, vencimento, autor);
		LancamentoFinanceiro salvo = lancamentoRepository.save(gerado);
		if (avancarSeProxima && competencia.equals(r.getProximaCompetencia())) {
			r.avancarProximaCompetencia();
			recorrenciaRepository.save(r);
		}
		return salvo;
	}

	private void exigirAtiva(RecorrenciaFinanceira r) {
		if (r.getStatus() != StatusRecorrencia.ATIVA) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia nao esta ativa");
		}
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId, ContaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Conta e obrigatoria");
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva() && (atual == null || !conta.getId().equals(atual.getId()))) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private CategoriaFinanceira buscarCategoria(UUID id, UUID empresaId, TipoFinanceiro tipo, CategoriaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva() && (atual == null || !categoria.getId().equals(atual.getId()))) {
			throw new CategoriaFinanceiraInativaException();
		}
		if (categoria.getTipo() != tipo) {
			throw new DadosInvalidosException("Tipo da recorrencia incompativel com a categoria");
		}
		return categoria;
	}

	private PessoaFinanceira buscarPessoa(UUID id, UUID empresaId, PessoaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Pessoa financeira e obrigatoria");
		}
		PessoaFinanceira pessoa = pessoaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		if (!pessoa.estaAtiva() && (atual == null || !pessoa.getId().equals(atual.getId()))) {
			throw new PessoaFinanceiraInativaException();
		}
		return pessoa;
	}

	private ParteFinanceira buscarParte(UUID id, UUID empresaId, ParteFinanceira atual) {
		if (id == null) {
			return null;
		}
		ParteFinanceira parte = parteRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(ParteFinanceiraNaoEncontradaException::new);
		if (!parte.estaAtiva() && (atual == null || !parte.getId().equals(atual.getId()))) {
			throw new DadosInvalidosException("Contato financeiro inativo");
		}
		return parte;
	}

	private void validarBasicos(TipoFinanceiro tipo, String descricao, BigDecimal valorPadrao,
			PeriodicidadeRecorrencia periodicidade, Integer intervalo, Integer diaReferencia, Integer mesReferencia,
			LocalDate dataInicial, LocalDate dataFinal) {
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao e obrigatoria");
		}
		if (descricao.trim().length() > 200) {
			throw new DadosInvalidosException("Descricao deve possuir no maximo 200 caracteres");
		}
		if (valorPadrao == null || valorPadrao.signum() <= 0) {
			throw new DadosInvalidosException("Valor padrao deve ser maior que zero");
		}
		if (periodicidade == null) {
			throw new DadosInvalidosException("Periodicidade e obrigatoria");
		}
		if (intervalo == null || intervalo < 1) {
			throw new DadosInvalidosException("Intervalo deve ser maior ou igual a 1");
		}
		if (diaReferencia == null || diaReferencia < 1 || diaReferencia > 31) {
			throw new DadosInvalidosException("Dia de referencia deve estar entre 1 e 31");
		}
		if (periodicidade == PeriodicidadeRecorrencia.ANUAL) {
			if (mesReferencia == null || mesReferencia < 1 || mesReferencia > 12) {
				throw new DadosInvalidosException("Mes de referencia e obrigatorio e deve estar entre 1 e 12 para recorrencia anual");
			}
		}
		if (dataInicial == null) {
			throw new DadosInvalidosException("Data inicial e obrigatoria");
		}
		if (dataFinal != null && dataFinal.isBefore(dataInicial)) {
			throw new DadosInvalidosException("Data final deve ser posterior ou igual a data inicial");
		}
	}

	private BigDecimal somarValorPadrao(List<RecorrenciaFinanceira> itens, TipoFinanceiro tipo) {
		return itens.stream().filter(r -> r.getStatus() == StatusRecorrencia.ATIVA).filter(r -> r.getTipo() == tipo)
				.map(RecorrenciaFinanceira::getValorPadrao).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private RecorrenciaFinanceira buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new RecorrenciaFinanceiraNaoEncontradaException();
		}
		return recorrenciaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(RecorrenciaFinanceiraNaoEncontradaException::new);
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarDescricao(String valor) {
		return valor.trim().replaceAll("\\s+", " ");
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}
}
