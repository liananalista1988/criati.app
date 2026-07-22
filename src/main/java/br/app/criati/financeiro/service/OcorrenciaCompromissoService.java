package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.CompromissoFinanceiroNaoEncontradoException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.OcorrenciaCompromissoNaoEncontradaException;
import br.app.criati.exception.OcorrenciaCompromissoStatusInvalidoException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RecorrenciaFinanceiraStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompromissoFinanceiro;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.OcorrenciaCompromisso;
import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompromissoFinanceiroRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.OcorrenciaCompromissoRepository;
import br.app.criati.financeiro.repository.RecorrenciaFinanceiraRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Ocorrencia concreta de uma competencia (obrigacao com vencimento e valor).
 * Ver docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md.
 */
@Service
public class OcorrenciaCompromissoService {

	/** Padrao inicial de antecedencia para alertas internos de vencimento (vence em ate N dias). */
	public static final int ALERTA_DIAS_ANTECEDENCIA = 3;

	private final OcorrenciaCompromissoRepository ocorrenciaRepository;
	private final CompromissoFinanceiroRepository compromissoRepository;
	private final RecorrenciaFinanceiraRepository recorrenciaRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final ParteFinanceiraRepository parteRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public OcorrenciaCompromissoService(OcorrenciaCompromissoRepository ocorrenciaRepository,
			CompromissoFinanceiroRepository compromissoRepository, RecorrenciaFinanceiraRepository recorrenciaRepository,
			CategoriaFinanceiraRepository categoriaRepository, PessoaFinanceiraRepository pessoaRepository,
			ParteFinanceiraRepository parteRepository, ContaFinanceiraRepository contaRepository,
			EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository) {
		this.ocorrenciaRepository = ocorrenciaRepository;
		this.compromissoRepository = compromissoRepository;
		this.recorrenciaRepository = recorrenciaRepository;
		this.categoriaRepository = categoriaRepository;
		this.pessoaRepository = pessoaRepository;
		this.parteRepository = parteRepository;
		this.contaRepository = contaRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<OcorrenciaCompromisso> listar(ContextoEmpresaAtual contexto, YearMonth competencia,
			LocalDate vencimentoInicio, LocalDate vencimentoFim, StatusOcorrenciaCompromisso status, Boolean vencidas,
			UUID pessoaId, UUID categoriaId, UUID parteId, UUID compromissoId, UUID contaPrevistaId, String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		LocalDate hoje = LocalDate.now();
		return ocorrenciaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(o -> competencia == null || o.getCompetencia().equals(competencia))
				.filter(o -> vencimentoInicio == null || !o.getVencimento().isBefore(vencimentoInicio))
				.filter(o -> vencimentoFim == null || !o.getVencimento().isAfter(vencimentoFim))
				.filter(o -> status == null || o.getStatus() == status)
				.filter(o -> vencidas == null || o.estaVencida(hoje) == vencidas)
				.filter(o -> pessoaId == null || pessoaId.equals(o.getPessoaFinanceira().getId()))
				.filter(o -> categoriaId == null || categoriaId.equals(o.getCategoria().getId()))
				.filter(o -> parteId == null || o.getParteFinanceira() != null && parteId.equals(o.getParteFinanceira().getId()))
				.filter(o -> compromissoId == null || o.getCompromisso() != null && compromissoId.equals(o.getCompromisso().getId()))
				.filter(o -> contaPrevistaId == null || o.getContaPrevista() != null && contaPrevistaId.equals(o.getContaPrevista().getId()))
				.filter(o -> termo.isBlank() || o.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<OcorrenciaCompromisso> listarCalendario(ContextoEmpresaAtual contexto, YearMonth mes) {
		YearMonth alvo = mes == null ? YearMonth.now() : mes;
		LocalDate inicio = alvo.atDay(1);
		LocalDate fim = alvo.atEndOfMonth();
		return ocorrenciaRepository.findAllByEmpresaIdAndVencimentoBetween(contexto.empresaId(), inicio, fim).stream()
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public OcorrenciaCompromisso buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public ResumoOcorrenciasContaPagar resumir(ContextoEmpresaAtual contexto, LocalDate vencimentoInicio,
			LocalDate vencimentoFim) {
		LocalDate hoje = LocalDate.now();
		List<OcorrenciaCompromisso> itens = ocorrenciaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(o -> vencimentoInicio == null || !o.getVencimento().isBefore(vencimentoInicio))
				.filter(o -> vencimentoFim == null || !o.getVencimento().isAfter(vencimentoFim))
				.toList();
		BigDecimal totalPrevisto = somar(itens, o -> o.getValorPrevisto() == null ? BigDecimal.ZERO : o.getValorPrevisto());
		BigDecimal totalPrincipal = somar(itens, OcorrenciaCompromisso::getValorPrincipal);
		BigDecimal totalJuros = somar(itens, OcorrenciaCompromisso::getJuros);
		BigDecimal totalMultas = somar(itens, OcorrenciaCompromisso::getMulta);
		BigDecimal totalDescontos = somar(itens, OcorrenciaCompromisso::getDesconto);
		BigDecimal totalPago = somar(itens, OcorrenciaCompromisso::getValorPago);
		BigDecimal saldoPendente = somar(itens, OcorrenciaCompromisso::getSaldoPendente);
		long pendentes = itens.stream().filter(o -> o.getStatus() == StatusOcorrenciaCompromisso.PENDENTE).count();
		long parciais = itens.stream().filter(o -> o.getStatus() == StatusOcorrenciaCompromisso.PARCIALMENTE_PAGA).count();
		long pagas = itens.stream().filter(o -> o.getStatus() == StatusOcorrenciaCompromisso.PAGA).count();
		long vencidas = itens.stream().filter(o -> o.estaVencida(hoje)).count();
		LocalDate limiteAlerta = hoje.plusDays(ALERTA_DIAS_ANTECEDENCIA);
		long vencendoEmBreve = itens.stream()
				.filter(o -> o.getStatus() != StatusOcorrenciaCompromisso.CANCELADA && o.getSaldoPendente().signum() > 0)
				.filter(o -> !o.getVencimento().isBefore(hoje) && !o.getVencimento().isAfter(limiteAlerta))
				.count();
		return new ResumoOcorrenciasContaPagar(totalPrevisto, totalPrincipal, totalJuros, totalMultas, totalDescontos,
				totalPago, saldoPendente, pendentes, parciais, pagas, vencidas, vencendoEmBreve);
	}

	@Transactional
	public OcorrenciaCompromisso criar(UUID compromissoId, String descricao, YearMonth competencia, UUID categoriaId,
			UUID pessoaId, UUID parteId, UUID contaPrevistaId, BigDecimal valorPrevisto, BigDecimal valorPrincipal,
			LocalDate vencimento, LocalDate dataRecebimentoCobranca, BigDecimal juros, BigDecimal multa,
			BigDecimal desconto, String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (competencia == null) {
			throw new DadosInvalidosException("Competencia e obrigatoria");
		}
		if (vencimento == null) {
			throw new DadosInvalidosException("Vencimento e obrigatorio");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao e obrigatoria");
		}
		CompromissoFinanceiro compromisso = compromissoId == null ? null
				: compromissoRepository.findByIdAndEmpresaId(compromissoId, contexto.empresaId())
						.orElseThrow(CompromissoFinanceiroNaoEncontradoException::new);
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), null);
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), null);
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), null);
		ContaFinanceira contaPrevista = buscarConta(contaPrevistaId, contexto.empresaId(), null);
        Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		OcorrenciaCompromisso ocorrencia = new OcorrenciaCompromisso(empresa, compromisso, null, competencia.atDay(1),
				normalizarDescricao(descricao), categoria, pessoa, parte, contaPrevista, valorPrevisto, valorPrincipal,
				vencimento, dataRecebimentoCobranca, juros, multa, desconto, normalizarOpcional(observacao),
				buscarAutor(contexto));
		return ocorrenciaRepository.save(ocorrencia);
	}

	/**
	 * Gera (ou retorna, se ja existir) a proxima ocorrencia de um compromisso
	 * recorrente. Reaproveita integralmente o motor de calculo de
	 * RecorrenciaFinanceira (calcularVencimento/dentroDoPeriodo/
	 * avancarProximaCompetencia) em vez de reimplementar periodicidade aqui —
	 * o unico ponto que muda em relacao a LES-F2-006 e o que e criado ao final
	 * (uma OcorrenciaCompromisso em vez de um LancamentoFinanceiro direto).
	 */
	@Transactional
	public OcorrenciaCompromisso gerarPorRecorrencia(UUID compromissoId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		CompromissoFinanceiro compromisso = compromissoRepository.findByIdAndEmpresaId(compromissoId, contexto.empresaId())
				.orElseThrow(CompromissoFinanceiroNaoEncontradoException::new);
		if (!compromisso.ehRecorrente()) {
			throw new DadosInvalidosException("Compromisso nao possui recorrencia vinculada");
		}
		if (!compromisso.isAtivo()) {
			throw new DadosInvalidosException("Compromisso esta inativo");
		}
		RecorrenciaFinanceira recorrencia = compromisso.getRecorrencia();
		if (!recorrencia.estaAtiva()) {
			throw new RecorrenciaFinanceiraStatusInvalidoException("Recorrencia nao esta ativa");
		}
		YearMonth alvo = recorrencia.getProximaCompetencia();
		if (alvo.isAfter(YearMonth.now())) {
			throw new DadosInvalidosException("Ainda nao e possivel gerar: proxima competencia esta fora do periodo");
		}
		if (!recorrencia.dentroDoPeriodo(alvo)) {
			throw new DadosInvalidosException("Competencia fora do periodo da recorrencia");
		}
		LocalDate dataCompetencia = alvo.atDay(1);
		var existente = ocorrenciaRepository.findByRecorrenciaIdAndCompetencia(recorrencia.getId(), dataCompetencia);
		if (existente.isPresent()) {
			return existente.get();
		}
		LocalDate vencimento = recorrencia.calcularVencimento(alvo);
		BigDecimal valorPrincipal = compromisso.getValorPadrao() != null ? compromisso.getValorPadrao()
				: recorrencia.getValorPadrao();
		Usuario autor = buscarAutor(contexto);
		OcorrenciaCompromisso ocorrencia = new OcorrenciaCompromisso(compromisso.getEmpresa(), compromisso, recorrencia,
				dataCompetencia, compromisso.getDescricao(), compromisso.getCategoria(), compromisso.getPessoaFinanceira(),
				compromisso.getParteFinanceira(), compromisso.getContaPadrao(), null, valorPrincipal, vencimento, null,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, autor);
		OcorrenciaCompromisso salva = ocorrenciaRepository.save(ocorrencia);
		if (alvo.equals(recorrencia.getProximaCompetencia())) {
			recorrencia.avancarProximaCompetencia();
			recorrenciaRepository.save(recorrencia);
		}
		return salva;
	}

	@Transactional
	public OcorrenciaCompromisso editar(UUID id, String descricao, UUID categoriaId, UUID pessoaId, UUID parteId,
			UUID contaPrevistaId, BigDecimal valorPrevisto, BigDecimal valorPrincipal, LocalDate vencimento,
			LocalDate dataRecebimentoCobranca, BigDecimal juros, BigDecimal multa, BigDecimal desconto,
			String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		OcorrenciaCompromisso atual = buscarDaEmpresa(id, contexto.empresaId());
		if (atual.getStatus() == StatusOcorrenciaCompromisso.CANCELADA) {
			throw new OcorrenciaCompromissoStatusInvalidoException("Ocorrencia cancelada nao pode ser editada");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao e obrigatoria");
		}
		if (vencimento == null) {
			throw new DadosInvalidosException("Vencimento e obrigatorio");
		}
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), atual.getCategoria());
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), atual.getPessoaFinanceira());
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), atual.getParteFinanceira());
		ContaFinanceira contaPrevista = buscarConta(contaPrevistaId, contexto.empresaId(), atual.getContaPrevista());
		Usuario autor = buscarAutor(contexto);
		atual.atualizarDadosGerais(normalizarDescricao(descricao), categoria, pessoa, parte, contaPrevista,
				normalizarOpcional(observacao), autor);
		atual.atualizarValores(valorPrevisto, valorPrincipal, juros, multa, desconto, autor);
		atual.atualizarVencimento(vencimento, autor);
		atual.registrarDataRecebimentoCobranca(dataRecebimentoCobranca, autor);
		return ocorrenciaRepository.save(atual);
	}

	@Transactional
	public OcorrenciaCompromisso cancelar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		OcorrenciaCompromisso ocorrencia = buscarDaEmpresa(id, contexto.empresaId());
		if (ocorrencia.getStatus() == StatusOcorrenciaCompromisso.CANCELADA) {
			throw new OcorrenciaCompromissoStatusInvalidoException("Ocorrencia ja esta cancelada");
		}
		ocorrencia.cancelar(buscarAutor(contexto));
		return ocorrenciaRepository.save(ocorrencia);
	}

	OcorrenciaCompromisso buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new OcorrenciaCompromissoNaoEncontradaException();
		}
		return ocorrenciaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(OcorrenciaCompromissoNaoEncontradaException::new);
	}

	private CategoriaFinanceira buscarCategoria(UUID id, UUID empresaId, CategoriaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva() && (atual == null || !categoria.getId().equals(atual.getId()))) {
			throw new CategoriaFinanceiraInativaException();
		}
		return categoria;
	}

	private PessoaFinanceira buscarPessoa(UUID id, UUID empresaId, PessoaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Pessoa financeira e obrigatoria");
		}
		PessoaFinanceira pessoa = pessoaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		if (!pessoa.estaAtiva() && (atual == null || !pessoa.getId().equals(atual.getId()))) {
			throw new PessoaFinanceiraInativaException();
		}
		return pessoa;
	}

	private ParteFinanceira buscarParte(UUID id, UUID empresaId, ParteFinanceira atual) {
		if (id == null) {
			return null;
		}
		ParteFinanceira parte = parteRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ParteFinanceiraNaoEncontradaException::new);
		if (!parte.estaAtiva() && (atual == null || !parte.getId().equals(atual.getId()))) {
			throw new DadosInvalidosException("Contato financeiro inativo");
		}
		return parte;
	}

	ContaFinanceira buscarConta(UUID id, UUID empresaId, ContaFinanceira atual) {
		if (id == null) {
			return null;
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva() && (atual == null || !conta.getId().equals(atual.getId()))) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private BigDecimal somar(List<OcorrenciaCompromisso> itens, java.util.function.Function<OcorrenciaCompromisso, BigDecimal> extrator) {
		return itens.stream().map(extrator).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarDescricao(String valor) {
		return valor.trim().replaceAll("\\s+", " ");
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}
}
