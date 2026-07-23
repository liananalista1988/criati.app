package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.ParcelaEmprestimoStatusInvalidoException;
import br.app.criati.exception.RecebimentoParcelaEmprestimoNaoEncontradoException;
import br.app.criati.exception.RecebimentoParcelaEmprestimoStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.model.RecebimentoParcelaEmprestimo;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.RecebimentoParcelaEmprestimoRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Liquidacao (integral ou parcial) e estorno de parcelas de emprestimo
 * concedido. Regra central: cada recebimento gera exatamente um
 * LancamentoFinanceiro de receita liquidado (origem EMPRESTIMO_CONCEDIDO); o
 * estorno cancela esse mesmo lancamento em vez de criar um mecanismo de
 * reversao paralelo — mesmo desenho ja usado por
 * PagamentoOcorrenciaCompromissoService para contas a pagar. Os encargos
 * (juros/multa) sao sempre calculados por EncargosEmprestimoService antes de
 * qualquer recebimento ser aplicado, nunca aqui ou no controller.
 */
@Service
public class RecebimentoParcelaEmprestimoService {

	private final RecebimentoParcelaEmprestimoRepository recebimentoRepository;
	private final ParcelaEmprestimoService parcelaEmprestimoService;
	private final ContaFinanceiraRepository contaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final EncargosEmprestimoService encargosService;
	private final UsuarioRepository usuarioRepository;

	public RecebimentoParcelaEmprestimoService(RecebimentoParcelaEmprestimoRepository recebimentoRepository,
			ParcelaEmprestimoService parcelaEmprestimoService, ContaFinanceiraRepository contaRepository,
			LancamentoFinanceiroRepository lancamentoRepository, EncargosEmprestimoService encargosService,
			UsuarioRepository usuarioRepository) {
		this.recebimentoRepository = recebimentoRepository;
		this.parcelaEmprestimoService = parcelaEmprestimoService;
		this.contaRepository = contaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.encargosService = encargosService;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<RecebimentoParcelaEmprestimo> listar(UUID parcelaId, ContextoEmpresaAtual contexto) {
		parcelaEmprestimoService.buscarDaEmpresa(parcelaId, contexto.empresaId());
		return recebimentoRepository.findAllByEmpresaIdAndParcelaIdOrderByDataRecebimentoDesc(contexto.empresaId(),
				parcelaId);
	}

	@Transactional
	public RecebimentoParcelaEmprestimo receberIntegral(UUID parcelaId, UUID contaId, LocalDate dataRecebimento,
			FormaPagamentoLancamento formaPagamento, String observacao, ContextoEmpresaAtual contexto) {
		ParcelaEmprestimo parcela = prepararParaRecebimento(parcelaId, dataRecebimento, contexto);
		aplicarEncargos(parcela, dataRecebimento, contexto);
		BigDecimal saldo = parcela.getSaldoPendente();
		if (saldo.signum() <= 0) {
			throw new ParcelaEmprestimoStatusInvalidoException("Parcela nao possui saldo pendente");
		}
		return registrarRecebimento(parcela, contaId, saldo, dataRecebimento, formaPagamento, observacao, contexto);
	}

	@Transactional
	public RecebimentoParcelaEmprestimo receberParcial(UUID parcelaId, UUID contaId, BigDecimal valor,
			LocalDate dataRecebimento, FormaPagamentoLancamento formaPagamento, String observacao,
			ContextoEmpresaAtual contexto) {
		ParcelaEmprestimo parcela = prepararParaRecebimento(parcelaId, dataRecebimento, contexto);
		aplicarEncargos(parcela, dataRecebimento, contexto);
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do recebimento deve ser maior que zero");
		}
		BigDecimal valorNormalizado = valor.setScale(2, RoundingMode.HALF_UP);
		if (valorNormalizado.compareTo(parcela.getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do recebimento nao pode exceder o saldo pendente");
		}
		return registrarRecebimento(parcela, contaId, valorNormalizado, dataRecebimento, formaPagamento, observacao,
				contexto);
	}

	@Transactional
	public RecebimentoParcelaEmprestimo estornar(UUID parcelaId, UUID recebimentoId, String motivo,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ParcelaEmprestimo parcela = parcelaEmprestimoService.buscarDaEmpresa(parcelaId, contexto.empresaId());
		RecebimentoParcelaEmprestimo recebimento = recebimentoRepository
				.findByIdAndParcelaIdAndEmpresaId(recebimentoId, parcela.getId(), contexto.empresaId())
				.orElseThrow(RecebimentoParcelaEmprestimoNaoEncontradoException::new);
		if (!recebimento.estaAtivo()) {
			throw new RecebimentoParcelaEmprestimoStatusInvalidoException("Recebimento ja esta estornado");
		}
		Usuario autor = buscarAutor(contexto);
		LancamentoFinanceiro lancamento = recebimento.getLancamentoFinanceiro();
		lancamento.cancelar(autor);
		lancamentoRepository.save(lancamento);
		recebimento.estornar(motivo, autor);
		recebimentoRepository.save(recebimento);
		parcela.estornarRecebimento(recebimento.getValor(), autor);
		EmprestimoConcedido emprestimo = parcela.getEmprestimo();
		emprestimo.reabrir(autor);
		return recebimento;
	}

	private ParcelaEmprestimo prepararParaRecebimento(UUID parcelaId, LocalDate dataRecebimento,
			ContextoEmpresaAtual contexto) {
		parcelaEmprestimoService.exigirEscritaEmprestimo(contexto);
		ParcelaEmprestimo parcela = parcelaEmprestimoService.buscarDaEmpresa(parcelaId, contexto.empresaId());
		if (parcela.getStatus() == StatusParcelaEmprestimo.CANCELADO) {
			throw new ParcelaEmprestimoStatusInvalidoException("Parcela cancelada nao pode receber pagamento");
		}
		if (dataRecebimento == null) {
			throw new DadosInvalidosException("Data de recebimento e obrigatoria");
		}
		return parcela;
	}

	private void aplicarEncargos(ParcelaEmprestimo parcela, LocalDate dataRecebimento, ContextoEmpresaAtual contexto) {
		EmprestimoConcedido emprestimo = parcela.getEmprestimo();
		BigDecimal juros = encargosService.calcularJuros(emprestimo, parcela.getValorPrincipal(),
				parcela.getVencimento(), dataRecebimento);
		BigDecimal multa = encargosService.calcularMulta(emprestimo, parcela.getValorPrincipal(),
				parcela.getVencimento(), dataRecebimento);
		parcela.aplicarEncargos(juros, multa, buscarAutor(contexto));
	}

	private RecebimentoParcelaEmprestimo registrarRecebimento(ParcelaEmprestimo parcela, UUID contaId,
			BigDecimal valor, LocalDate dataRecebimento, FormaPagamentoLancamento formaPagamento, String observacao,
			ContextoEmpresaAtual contexto) {
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId());
		Usuario autor = buscarAutor(contexto);
		EmprestimoConcedido emprestimo = parcela.getEmprestimo();
		String descricao = descricaoRecebimento(emprestimo, parcela);
		LancamentoFinanceiro lancamento = LancamentoFinanceiro.gerarDeEmprestimoConcedido(emprestimo.getEmpresa(),
				conta, emprestimo.getCategoria(), emprestimo.getParteFinanceira(), descricao, valor,
				parcela.getVencimento().withDayOfMonth(1), parcela.getVencimento(), dataRecebimento, formaPagamento,
				autor);
		LancamentoFinanceiro lancamentoSalvo = lancamentoRepository.save(lancamento);
		RecebimentoParcelaEmprestimo recebimento = new RecebimentoParcelaEmprestimo(emprestimo.getEmpresa(), parcela,
				conta, valor, dataRecebimento, formaPagamento, normalizarOpcional(observacao), lancamentoSalvo, autor);
		RecebimentoParcelaEmprestimo recebimentoSalvo = recebimentoRepository.save(recebimento);
		parcela.registrarRecebimento(valor, dataRecebimento, autor);
		boolean todasQuitadas = emprestimo.getParcelas().stream()
				.allMatch(p -> p.getStatus() == StatusParcelaEmprestimo.PAGO
						|| p.getStatus() == StatusParcelaEmprestimo.CANCELADO);
		if (todasQuitadas) {
			emprestimo.marcarQuitado(autor);
		}
		return recebimentoSalvo;
	}

	private String descricaoRecebimento(EmprestimoConcedido emprestimo, ParcelaEmprestimo parcela) {
		String base = emprestimo.getDescricao() != null ? emprestimo.getDescricao()
				: "Emprestimo a " + emprestimo.getParteFinanceira().getNome();
		String descricao = "Recebimento parcela " + parcela.getNumero() + "/" + parcela.getTotalParcelas() + " - " + base;
		return descricao.length() > 200 ? descricao.substring(0, 200) : descricao;
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Conta e obrigatoria");
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
