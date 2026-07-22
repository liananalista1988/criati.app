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
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.LancamentoFinanceiroNaoEncontradoException;
import br.app.criati.exception.LancamentoStatusInvalidoException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class LancamentoFinanceiroService {

	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final ParteFinanceiraRepository parteRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final SaldoFinanceiroService saldoService;

	public LancamentoFinanceiroService(LancamentoFinanceiroRepository lancamentoRepository,
			ContaFinanceiraRepository contaRepository, CategoriaFinanceiraRepository categoriaRepository,
			PessoaFinanceiraRepository pessoaRepository, ParteFinanceiraRepository parteRepository,
			EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
			SaldoFinanceiroService saldoService) {
		this.lancamentoRepository = lancamentoRepository;
		this.contaRepository = contaRepository;
		this.categoriaRepository = categoriaRepository;
		this.pessoaRepository = pessoaRepository;
		this.parteRepository = parteRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.saldoService = saldoService;
	}

	@Transactional(readOnly = true)
	public List<LancamentoFinanceiro> listar(ContextoEmpresaAtual contexto, LocalDate dataInicial,
			LocalDate dataFinal, TipoFinanceiro tipo, StatusLancamentoFinanceiro status, UUID contaId,
			UUID categoriaId, UUID pessoaId, UUID parteId, Boolean vencido, String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		LocalDate hoje = LocalDate.now();
		return lancamentoRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(l -> dataInicial == null || !l.getDataCompetencia().isBefore(dataInicial))
				.filter(l -> dataFinal == null || !l.getDataCompetencia().isAfter(dataFinal))
				.filter(l -> tipo == null || l.getTipo() == tipo)
				.filter(l -> status == null || l.getStatus() == status)
				.filter(l -> contaId == null || contaId.equals(l.getConta().getId()))
				.filter(l -> categoriaId == null || categoriaId.equals(l.getCategoria().getId()))
				.filter(l -> pessoaId == null || l.getPessoaFinanceira() != null && pessoaId.equals(l.getPessoaFinanceira().getId()))
				.filter(l -> parteId == null || l.getParteFinanceira() != null && parteId.equals(l.getParteFinanceira().getId()))
				.filter(l -> vencido == null || l.estaVencido(hoje) == vencido)
				.filter(l -> termo.isBlank() || l.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> b.getDataCompetencia().compareTo(a.getDataCompetencia()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<LancamentoFinanceiro> listar(ContextoEmpresaAtual contexto, LocalDate inicio, LocalDate fim,
			TipoFinanceiro tipo, StatusLancamentoFinanceiro status, UUID contaId, UUID categoriaId, String busca) {
		return listar(contexto, inicio, fim, tipo, status, contaId, categoriaId, null, null, null, busca);
	}

	@Transactional(readOnly = true)
	public LancamentoFinanceiro buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public ResumoLancamentosFinanceiros resumir(ContextoEmpresaAtual contexto, YearMonth competencia,
			UUID pessoaId, UUID contaId) {
		YearMonth mes = competencia == null ? YearMonth.now() : competencia;
		List<LancamentoFinanceiro> todos = lancamentoRepository.findAllByEmpresaId(contexto.empresaId());
		List<LancamentoFinanceiro> doMes = todos.stream()
				.filter(l -> YearMonth.from(l.getDataCompetencia()).equals(mes))
				.filter(l -> pessoaId == null || l.getPessoaFinanceira() != null && pessoaId.equals(l.getPessoaFinanceira().getId()))
				.filter(l -> contaId == null || contaId.equals(l.getConta().getId())).toList();
		BigDecimal receitasLiquidadas = somar(doMes, TipoFinanceiro.RECEITA, true);
		BigDecimal despesasLiquidadas = somar(doMes, TipoFinanceiro.DESPESA, true);
		BigDecimal receitasPendentes = somar(doMes, TipoFinanceiro.RECEITA, false);
		BigDecimal despesasPendentes = somar(doMes, TipoFinanceiro.DESPESA, false);
		long vencidos = doMes.stream().filter(l -> l.estaVencido(LocalDate.now())).count();
		List<ContaFinanceira> contas = contaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(c -> contaId == null || contaId.equals(c.getId())).toList();
		List<SaldoContaFinanceira> saldos = contas.stream()
				.map(c -> new SaldoContaFinanceira(c.getId(), c.getNome(), saldoService.calcularSaldoAtual(c))).toList();
		BigDecimal consolidado = saldos.stream().map(SaldoContaFinanceira::saldoAtual)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new ResumoLancamentosFinanceiros(mes, receitasLiquidadas, despesasLiquidadas,
				receitasLiquidadas.subtract(despesasLiquidadas), receitasPendentes, despesasPendentes,
				vencidos, saldos, consolidado);
	}

	@Transactional
	public LancamentoFinanceiro criar(UUID contaId, UUID categoriaId, UUID pessoaId, UUID parteId,
			TipoFinanceiro tipo, String descricao, BigDecimal valor, LocalDate competencia,
			LocalDate vencimento, StatusLancamentoFinanceiro status, LocalDate liquidacao,
			FormaPagamentoLancamento formaPagamento, String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		validarBasicos(tipo, descricao, valor, competencia, vencimento);
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId(), null);
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), tipo, null);
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), conta, null);
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), null);
		StatusLancamentoFinanceiro statusEfetivo = status == null ? StatusLancamentoFinanceiro.PENDENTE : status;
		LocalDate liquidacaoEfetiva = validarStatus(statusEfetivo, liquidacao);
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		return lancamentoRepository.save(new LancamentoFinanceiro(empresa, conta, categoria, pessoa, parte, tipo,
				normalizarDescricao(descricao), MoedaUtils.normalizar(valor), competencia, vencimento,
				liquidacaoEfetiva, statusEfetivo, formaPagamento, normalizarOpcional(observacao), buscarAutor(contexto)));
	}

	@Transactional
	public LancamentoFinanceiro criar(UUID contaId, UUID categoriaId, TipoFinanceiro tipo, String descricao,
			BigDecimal valor, LocalDate competencia, StatusLancamentoFinanceiro status, LocalDate pagamento,
			String observacao, ContextoEmpresaAtual contexto) {
		return criar(contaId, categoriaId, null, null, tipo, descricao, valor, competencia, null,
				status, pagamento, null, observacao, contexto);
	}

	@Transactional
	public LancamentoFinanceiro editar(UUID id, UUID contaId, UUID categoriaId, UUID pessoaId, UUID parteId,
			TipoFinanceiro tipo, String descricao, BigDecimal valor, LocalDate competencia,
			LocalDate vencimento, LocalDate liquidacao, FormaPagamentoLancamento formaPagamento,
			String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LancamentoFinanceiro atual = buscarDaEmpresa(id, contexto.empresaId());
		if (atual.getStatus() == StatusLancamentoFinanceiro.CANCELADO) {
			throw new LancamentoStatusInvalidoException("Lancamento cancelado nao pode ser editado");
		}
		TipoFinanceiro tipoEfetivo = tipo == null ? atual.getTipo() : tipo;
		validarBasicos(tipoEfetivo, descricao, valor, competencia, vencimento);
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId(), atual.getConta());
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), tipoEfetivo, atual.getCategoria());
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), conta, atual.getPessoaFinanceira());
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), atual.getParteFinanceira());
		LocalDate liquidacaoEfetiva = validarStatus(atual.getStatus(), liquidacao == null ? atual.getDataLiquidacao() : liquidacao);
		atual.atualizarDados(conta, categoria, pessoa, parte, tipoEfetivo, normalizarDescricao(descricao),
				MoedaUtils.normalizar(valor), competencia, vencimento, liquidacaoEfetiva, formaPagamento,
				normalizarOpcional(observacao), buscarAutor(contexto));
		return lancamentoRepository.save(atual);
	}

	@Transactional
	public LancamentoFinanceiro editar(UUID id, UUID contaId, UUID categoriaId, String descricao,
			BigDecimal valor, LocalDate competencia, String observacao, ContextoEmpresaAtual contexto) {
		LancamentoFinanceiro atual = buscarDaEmpresa(id, contexto.empresaId());
		return editar(id, contaId, categoriaId,
				atual.getPessoaFinanceira() == null ? null : atual.getPessoaFinanceira().getId(),
				atual.getParteFinanceira() == null ? null : atual.getParteFinanceira().getId(), atual.getTipo(),
				descricao, valor, competencia, atual.getDataVencimento(), atual.getDataLiquidacao(),
				atual.getFormaPagamento(), observacao, contexto);
	}

	@Transactional
	public LancamentoFinanceiro liquidar(UUID id, LocalDate data, FormaPagamentoLancamento forma,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (data == null) throw new DadosInvalidosException("Data de liquidacao e obrigatoria");
		LancamentoFinanceiro l = buscarDaEmpresa(id, contexto.empresaId());
		if (l.getStatus() != StatusLancamentoFinanceiro.PENDENTE) throw new LancamentoStatusInvalidoException("Lancamento nao esta pendente");
		if (!l.getConta().estaAtiva()) throw new ContaFinanceiraInativaException();
		l.liquidar(data, forma, buscarAutor(contexto));
		return lancamentoRepository.save(l);
	}

	@Transactional
	public LancamentoFinanceiro pagar(UUID id, LocalDate data, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (data == null) throw new DadosInvalidosException("Data de pagamento e obrigatoria");
		LancamentoFinanceiro l = buscarDaEmpresa(id, contexto.empresaId());
		if (l.getStatus() != StatusLancamentoFinanceiro.PENDENTE) throw new LancamentoStatusInvalidoException("Lancamento nao esta pendente");
		if (!l.getConta().estaAtiva()) throw new ContaFinanceiraInativaException();
		l.pagar(data, buscarAutor(contexto));
		return lancamentoRepository.save(l);
	}

	@Transactional
	public LancamentoFinanceiro desliquidar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		LancamentoFinanceiro l = buscarDaEmpresa(id, contexto.empresaId());
		if (!l.compoeSaldoRealizado()) throw new LancamentoStatusInvalidoException("Lancamento nao esta liquidado");
		l.desliquidar(buscarAutor(contexto));
		return lancamentoRepository.save(l);
	}

	@Transactional
	public LancamentoFinanceiro reabrir(UUID id, ContextoEmpresaAtual contexto) {
		return desliquidar(id, contexto);
	}

	@Transactional
	public LancamentoFinanceiro cancelar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LancamentoFinanceiro l = buscarDaEmpresa(id, contexto.empresaId());
		if (l.getStatus() == StatusLancamentoFinanceiro.CANCELADO) throw new LancamentoStatusInvalidoException("Lancamento ja esta cancelado");
		l.cancelar(buscarAutor(contexto));
		return lancamentoRepository.save(l);
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId, ContaFinanceira atual) {
		if (id == null) throw new DadosInvalidosException("Conta e obrigatoria");
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva() && (atual == null || !conta.getId().equals(atual.getId()))) throw new ContaFinanceiraInativaException();
		return conta;
	}

	private CategoriaFinanceira buscarCategoria(UUID id, UUID empresaId, TipoFinanceiro tipo, CategoriaFinanceira atual) {
		if (id == null) throw new DadosInvalidosException("Categoria e obrigatoria");
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva() && (atual == null || !categoria.getId().equals(atual.getId()))) throw new CategoriaFinanceiraInativaException();
		if (categoria.getTipo() != tipo) throw new DadosInvalidosException("Tipo do lancamento incompativel com a categoria");
		return categoria;
	}

	private PessoaFinanceira buscarPessoa(UUID id, UUID empresaId, ContaFinanceira conta, PessoaFinanceira atual) {
		PessoaFinanceira pessoa;
		if (id == null) {
			pessoa = conta.getTitular();
			if (pessoa == null) return atual;
		} else {
			pessoa = pessoaRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		}
		if (!pessoa.estaAtiva() && (atual == null || !pessoa.getId().equals(atual.getId()))) throw new PessoaFinanceiraInativaException();
		return pessoa;
	}

	private ParteFinanceira buscarParte(UUID id, UUID empresaId, ParteFinanceira atual) {
		if (id == null) return null;
		ParteFinanceira parte = parteRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(ParteFinanceiraNaoEncontradaException::new);
		if (!parte.estaAtiva() && (atual == null || !parte.getId().equals(atual.getId()))) throw new DadosInvalidosException("Contato financeiro inativo");
		return parte;
	}

	private void validarBasicos(TipoFinanceiro tipo, String descricao, BigDecimal valor,
			LocalDate competencia, LocalDate vencimento) {
		if (tipo == null) throw new DadosInvalidosException("Tipo e obrigatorio");
		if (descricao == null || descricao.isBlank()) throw new DadosInvalidosException("Descricao e obrigatoria");
		if (descricao.trim().length() > 200) throw new DadosInvalidosException("Descricao deve possuir no maximo 200 caracteres");
		if (valor == null || valor.signum() <= 0) throw new DadosInvalidosException("Valor deve ser maior que zero");
		if (competencia == null) throw new DadosInvalidosException("Data de competencia e obrigatoria");
	}

	private LocalDate validarStatus(StatusLancamentoFinanceiro status, LocalDate liquidacao) {
		if (status == StatusLancamentoFinanceiro.CANCELADO) throw new DadosInvalidosException("Nao e possivel criar ou editar como cancelado");
		if ((status == StatusLancamentoFinanceiro.LIQUIDADO || status == StatusLancamentoFinanceiro.PAGO) && liquidacao == null)
			throw new DadosInvalidosException("Data de liquidacao e obrigatoria");
		if (status == StatusLancamentoFinanceiro.PENDENTE && liquidacao != null)
			throw new DadosInvalidosException("Lancamento pendente nao pode ter data de liquidacao");
		return status == StatusLancamentoFinanceiro.PENDENTE ? null : liquidacao;
	}

	private BigDecimal somar(List<LancamentoFinanceiro> itens, TipoFinanceiro tipo, boolean liquidado) {
		return itens.stream().filter(l -> l.getTipo() == tipo)
				.filter(l -> liquidado ? l.compoeSaldoRealizado() : l.getStatus() == StatusLancamentoFinanceiro.PENDENTE)
				.map(LancamentoFinanceiro::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private LancamentoFinanceiro buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) throw new LancamentoFinanceiroNaoEncontradoException();
		return lancamentoRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(LancamentoFinanceiroNaoEncontradoException::new);
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarDescricao(String valor) { return valor.trim().replaceAll("\\s+", " "); }
	private String normalizarOpcional(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) throw new AcessoNegadoException();
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) throw new AcessoNegadoException();
	}
}
