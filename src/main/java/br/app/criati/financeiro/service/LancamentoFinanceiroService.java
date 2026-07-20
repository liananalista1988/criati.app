package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusLancamentoFinanceiro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;

@Service
public class LancamentoFinanceiroService {

	private final LancamentoFinanceiroRepository lancamentoFinanceiroRepository;
	private final ContaFinanceiraRepository contaFinanceiraRepository;
	private final CategoriaFinanceiraRepository categoriaFinanceiraRepository;
	private final EmpresaRepository empresaRepository;

	public LancamentoFinanceiroService(
			LancamentoFinanceiroRepository lancamentoFinanceiroRepository,
			ContaFinanceiraRepository contaFinanceiraRepository,
			CategoriaFinanceiraRepository categoriaFinanceiraRepository,
			EmpresaRepository empresaRepository) {
		this.lancamentoFinanceiroRepository = lancamentoFinanceiroRepository;
		this.contaFinanceiraRepository = contaFinanceiraRepository;
		this.categoriaFinanceiraRepository = categoriaFinanceiraRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<LancamentoFinanceiro> listar(
			ContextoEmpresaAtual contexto,
			LocalDate dataInicial,
			LocalDate dataFinal,
			TipoFinanceiro tipoFiltro,
			StatusLancamentoFinanceiro statusFiltro,
			UUID contaIdFiltro,
			UUID categoriaIdFiltro,
			String busca) {
		String buscaNormalizada = (busca == null || busca.isBlank()) ? null : busca.trim().toLowerCase(Locale.ROOT);

		return lancamentoFinanceiroRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(l -> dataInicial == null || !l.getDataCompetencia().isBefore(dataInicial))
				.filter(l -> dataFinal == null || !l.getDataCompetencia().isAfter(dataFinal))
				.filter(l -> tipoFiltro == null || l.getTipo() == tipoFiltro)
				.filter(l -> statusFiltro == null || l.getStatus() == statusFiltro)
				.filter(l -> contaIdFiltro == null || l.getConta().getId().equals(contaIdFiltro))
				.filter(l -> categoriaIdFiltro == null || l.getCategoria().getId().equals(categoriaIdFiltro))
				.filter(l -> buscaNormalizada == null
						|| l.getDescricao().toLowerCase(Locale.ROOT).contains(buscaNormalizada))
				.sorted((a, b) -> b.getDataCompetencia().compareTo(a.getDataCompetencia()))
				.toList();
	}

	@Transactional(readOnly = true)
	public LancamentoFinanceiro buscar(UUID lancamentoId, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(lancamentoId, contexto.empresaId());
	}

	@Transactional
	public LancamentoFinanceiro criar(
			UUID contaId,
			UUID categoriaId,
			TipoFinanceiro tipo,
			String descricao,
			BigDecimal valor,
			LocalDate dataCompetencia,
			StatusLancamentoFinanceiro status,
			LocalDate dataPagamento,
			String observacao,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		validarDadosBasicos(tipo, descricao, valor, dataCompetencia);

		ContaFinanceira conta = buscarContaValida(contaId, contexto.empresaId());
		CategoriaFinanceira categoria = buscarCategoriaValida(categoriaId, contexto.empresaId(), tipo);
		StatusLancamentoFinanceiro statusEfetivo = status == null ? StatusLancamentoFinanceiro.PENDENTE : status;
		LocalDate dataPagamentoEfetiva = validarStatusEData(statusEfetivo, dataPagamento);

		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);

		LancamentoFinanceiro lancamento = new LancamentoFinanceiro(
				empresa,
				conta,
				categoria,
				tipo,
				descricao.trim(),
				MoedaUtils.normalizar(valor),
				dataCompetencia,
				dataPagamentoEfetiva,
				statusEfetivo,
				observacao == null || observacao.isBlank() ? null : observacao.trim());
		return lancamentoFinanceiroRepository.save(lancamento);
	}

	// So permite editar lancamentos PENDENTE: um lancamento PAGO precisa ser
	// reaberto antes (regra segura citada na tarefa), e um lancamento
	// CANCELADO nunca pode ser alterado - preserva o historico tal como foi
	// cancelado.
	@Transactional
	public LancamentoFinanceiro editar(
			UUID lancamentoId,
			UUID contaId,
			UUID categoriaId,
			String descricao,
			BigDecimal valor,
			LocalDate dataCompetencia,
			String observacao,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LancamentoFinanceiro lancamento = buscarDaEmpresa(lancamentoId, contexto.empresaId());
		if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
			throw new LancamentoStatusInvalidoException(
					"Somente lancamentos pendentes podem ser editados (reabra o lancamento antes de editar)");
		}
		validarDadosBasicos(lancamento.getTipo(), descricao, valor, dataCompetencia);

		ContaFinanceira conta = buscarContaValida(contaId, contexto.empresaId());
		CategoriaFinanceira categoria = buscarCategoriaValida(categoriaId, contexto.empresaId(), lancamento.getTipo());

		lancamento.atualizarDados(
				conta,
				categoria,
				descricao.trim(),
				MoedaUtils.normalizar(valor),
				dataCompetencia,
				observacao == null || observacao.isBlank() ? null : observacao.trim());
		return lancamentoFinanceiroRepository.save(lancamento);
	}

	@Transactional
	public LancamentoFinanceiro pagar(UUID lancamentoId, LocalDate dataPagamento, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (dataPagamento == null) {
			throw new DadosInvalidosException("Data de pagamento e obrigatoria");
		}
		LancamentoFinanceiro lancamento = buscarDaEmpresa(lancamentoId, contexto.empresaId());
		if (lancamento.getStatus() != StatusLancamentoFinanceiro.PENDENTE) {
			throw new LancamentoStatusInvalidoException("Lancamento nao esta pendente");
		}
		lancamento.pagar(dataPagamento);
		return lancamentoFinanceiroRepository.save(lancamento);
	}

	@Transactional
	public LancamentoFinanceiro reabrir(UUID lancamentoId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		LancamentoFinanceiro lancamento = buscarDaEmpresa(lancamentoId, contexto.empresaId());
		if (lancamento.getStatus() != StatusLancamentoFinanceiro.PAGO) {
			throw new LancamentoStatusInvalidoException("Lancamento nao esta pago");
		}
		lancamento.reabrir();
		return lancamentoFinanceiroRepository.save(lancamento);
	}

	@Transactional
	public LancamentoFinanceiro cancelar(UUID lancamentoId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LancamentoFinanceiro lancamento = buscarDaEmpresa(lancamentoId, contexto.empresaId());
		if (lancamento.getStatus() == StatusLancamentoFinanceiro.CANCELADO) {
			throw new LancamentoStatusInvalidoException("Lancamento ja esta cancelado");
		}
		lancamento.cancelar();
		return lancamentoFinanceiroRepository.save(lancamento);
	}

	private ContaFinanceira buscarContaValida(UUID contaId, UUID empresaId) {
		if (contaId == null) {
			throw new DadosInvalidosException("Conta e obrigatoria");
		}
		ContaFinanceira conta = contaFinanceiraRepository.findByIdAndEmpresaId(contaId, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private CategoriaFinanceira buscarCategoriaValida(UUID categoriaId, UUID empresaId, TipoFinanceiro tipo) {
		if (categoriaId == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaFinanceiraRepository.findByIdAndEmpresaId(categoriaId, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva()) {
			throw new CategoriaFinanceiraInativaException();
		}
		if (categoria.getTipo() != tipo) {
			throw new DadosInvalidosException("Tipo do lancamento incompativel com a categoria");
		}
		return categoria;
	}

	private LancamentoFinanceiro buscarDaEmpresa(UUID lancamentoId, UUID empresaId) {
		if (lancamentoId == null) {
			throw new LancamentoFinanceiroNaoEncontradoException();
		}
		return lancamentoFinanceiroRepository.findByIdAndEmpresaId(lancamentoId, empresaId)
				.orElseThrow(LancamentoFinanceiroNaoEncontradoException::new);
	}

	private void validarDadosBasicos(TipoFinanceiro tipo, String descricao, BigDecimal valor, LocalDate dataCompetencia) {
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao e obrigatoria");
		}
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor deve ser maior que zero");
		}
		if (dataCompetencia == null) {
			throw new DadosInvalidosException("Data de competencia e obrigatoria");
		}
	}

	private LocalDate validarStatusEData(StatusLancamentoFinanceiro status, LocalDate dataPagamento) {
		if (status == StatusLancamentoFinanceiro.PAGO && dataPagamento == null) {
			throw new DadosInvalidosException("Data de pagamento e obrigatoria para lancamento pago");
		}
		if (status == StatusLancamentoFinanceiro.PENDENTE && dataPagamento != null) {
			throw new DadosInvalidosException("Lancamento pendente nao pode ter data de pagamento");
		}
		if (status == StatusLancamentoFinanceiro.CANCELADO) {
			throw new DadosInvalidosException("Nao e possivel criar um lancamento ja cancelado");
		}
		return status == StatusLancamentoFinanceiro.PAGO ? dataPagamento : null;
	}

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
